package com.mbolt.casino.game.roulette;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntUnaryOperator;

/** One persistent round. The Minecraft layer must debit chips and reserve the admitted exposure atomically. */
public final class RouletteRound {
    public static final int MAX_BETS = 256;
    private final UUID id;
    private final int bettingTicks;
    private final int spinningTicks;
    private final int payoutTicks;
    private final List<PlacedBet> bets = new ArrayList<>();
    private Phase phase = Phase.IDLE;
    private long deadline;
    private int number = -1;
    private long exposure;
    private boolean settled;

    public RouletteRound(UUID id, int bettingTicks, int spinningTicks, int payoutTicks) {
        if (id == null || bettingTicks <= 0 || spinningTicks <= 0 || payoutTicks <= 0) {
            throw new IllegalArgumentException("Invalid roulette timing");
        }
        this.id = id;
        this.bettingTicks = bettingTicks;
        this.spinningTicks = spinningTicks;
        this.payoutTicks = payoutTicks;
    }

    /** Pure admission check; rejections leave every part of the round untouched. */
    public Admission preview(PlacedBet placed, long now, long wallet, long freeReserve, long maxBet,
                             long maxRoundPayout) {
        if (phase != Phase.IDLE && (phase != Phase.BETTING || now >= deadline)) {
            return new Admission(Refusal.CLOSED, exposure);
        }
        if (bets.stream().anyMatch(bet -> bet.id().equals(placed.id()))) {
            return new Admission(Refusal.DUPLICATE, exposure);
        }
        if (bets.size() >= MAX_BETS) {
            return new Admission(Refusal.TOO_MANY_BETS, exposure);
        }
        if (placed.bet().chips() > maxBet) {
            return new Admission(Refusal.BET_LIMIT, exposure);
        }
        if (placed.bet().chips() > wallet) {
            return new Admission(Refusal.INSUFFICIENT_CHIPS, exposure);
        }
        var candidate = new ArrayList<RouletteBet>(bets.size() + 1);
        bets.forEach(bet -> candidate.add(bet.bet()));
        candidate.add(placed.bet());
        long nextExposure;
        try {
            long totalStakes = 0;
            for (RouletteBet bet : candidate) {
                totalStakes = Math.addExact(totalStakes, bet.chips());
            }
            nextExposure = RouletteLogic.maximumPayout(candidate);
        } catch (ArithmeticException overflow) {
            return new Admission(Refusal.ROUND_LIMIT, exposure);
        }
        if (nextExposure > maxRoundPayout) {
            return new Admission(Refusal.ROUND_LIMIT, exposure);
        }
        if (nextExposure - exposure > freeReserve) {
            return new Admission(Refusal.INSUFFICIENT_RESERVE, exposure);
        }
        return new Admission(Refusal.NONE, nextExposure);
    }

    public Admission place(PlacedBet placed, long now, long wallet, long freeReserve, long maxBet,
                           long maxRoundPayout) {
        Admission admission = preview(placed, now, wallet, freeReserve, maxBet, maxRoundPayout);
        if (admission.accepted()) {
            long nextDeadline = phase == Phase.IDLE ? Math.addExact(now, bettingTicks) : deadline;
            bets.add(placed);
            exposure = admission.exposure();
            deadline = nextDeadline;
            phase = Phase.BETTING;
        }
        return admission;
    }

    /** Draw exactly once, at the close of betting. No result exists while players can still bet. */
    public boolean advance(long now, IntUnaryOperator serverRandom) {
        if (now < deadline) {
            return false;
        }
        if (phase == Phase.BETTING) {
            int chosen = RouletteLogic.draw(serverRandom);
            long nextDeadline = Math.addExact(now, spinningTicks);
            number = chosen;
            deadline = nextDeadline;
            phase = Phase.SPINNING;
        } else if (phase == Phase.SPINNING) {
            deadline = Math.addExact(now, payoutTicks);
            phase = Phase.PAYOUT;
        } else if (phase == Phase.PAYOUT && settled) {
            phase = Phase.COMPLETE;
        } else {
            return false;
        }
        return true;
    }

    public Map<UUID, Long> payouts() {
        if (phase != Phase.PAYOUT && phase != Phase.COMPLETE) {
            return Map.of();
        }
        Map<UUID, Long> result = new LinkedHashMap<>();
        for (PlacedBet placed : bets) {
            long chips = RouletteLogic.payout(placed.bet(), number);
            if (chips > 0) {
                result.merge(placed.player(), chips, Math::addExact);
            }
        }
        return Map.copyOf(result);
    }

    /** Refunds are planned before marking the round cancelled; persistence is owned by the caller. */
    public Map<UUID, Long> refunds() {
        if (settled || phase == Phase.COMPLETE || phase == Phase.CANCELLED) {
            return Map.of();
        }
        Map<UUID, Long> result = new LinkedHashMap<>();
        bets.forEach(bet -> result.merge(bet.player(), bet.bet().chips(), Math::addExact));
        return Map.copyOf(result);
    }

    public boolean settle() {
        if (phase != Phase.PAYOUT || settled) {
            return false;
        }
        settled = true;
        return true;
    }

    public boolean cancel() {
        if (phase == Phase.IDLE || settled || phase == Phase.COMPLETE || phase == Phase.CANCELLED) {
            return false;
        }
        phase = Phase.CANCELLED;
        return true;
    }

    public Snapshot snapshot() {
        return new Snapshot(id, bettingTicks, spinningTicks, payoutTicks, phase, deadline, number,
                List.copyOf(bets), settled);
    }

    public static RouletteRound restore(Snapshot saved) {
        var round = new RouletteRound(saved.id(), saved.bettingTicks(), saved.spinningTicks(), saved.payoutTicks());
        var uniqueIds = new HashSet<UUID>();
        boolean inconsistent = saved.phase() == Phase.IDLE && !saved.bets().isEmpty()
                || saved.phase() != Phase.IDLE && saved.bets().isEmpty()
                || saved.settled() && saved.phase() != Phase.PAYOUT && saved.phase() != Phase.COMPLETE
                || saved.phase() == Phase.COMPLETE && !saved.settled();
        for (PlacedBet bet : saved.bets()) {
            inconsistent |= !uniqueIds.add(bet.id());
        }
        boolean chosen = saved.phase() == Phase.SPINNING || saved.phase() == Phase.PAYOUT
                || saved.phase() == Phase.COMPLETE;
        if (inconsistent || saved.bets().size() > MAX_BETS || saved.deadline() < 0 || saved.number() < -1
                || saved.number() >= RouletteLogic.POCKETS
                || chosen && (saved.number() < 0 || saved.number() >= RouletteLogic.POCKETS)
                || !chosen && saved.phase() != Phase.CANCELLED && saved.number() != -1) {
            throw new IllegalArgumentException("Inconsistent saved roulette round");
        }
        round.bets.addAll(saved.bets());
        round.exposure = RouletteLogic.maximumPayout(saved.bets().stream().map(PlacedBet::bet).toList());
        round.phase = saved.phase();
        round.deadline = saved.deadline();
        round.number = saved.number();
        round.settled = saved.settled();
        return round;
    }

    public enum Phase {
        IDLE, BETTING, SPINNING, PAYOUT, COMPLETE, CANCELLED
    }

    public enum Refusal {
        NONE, CLOSED, DUPLICATE, TOO_MANY_BETS, BET_LIMIT, ROUND_LIMIT, INSUFFICIENT_CHIPS, INSUFFICIENT_RESERVE
    }

    public record Admission(Refusal refusal, long exposure) {
        public boolean accepted() {
            return refusal == Refusal.NONE;
        }
    }

    public record PlacedBet(UUID id, UUID player, RouletteBet bet) {
        public PlacedBet {
            if (id == null || player == null || bet == null) {
                throw new IllegalArgumentException("Incomplete placed bet");
            }
        }
    }

    public record Snapshot(UUID id, int bettingTicks, int spinningTicks, int payoutTicks, Phase phase,
                           long deadline, int number, List<PlacedBet> bets, boolean settled) {
        public Snapshot {
            if (phase == null) {
                throw new IllegalArgumentException("Missing round phase");
            }
            bets = List.copyOf(bets);
        }
    }
}
