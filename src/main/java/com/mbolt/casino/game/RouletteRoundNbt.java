package com.mbolt.casino.game;

import com.mbolt.casino.game.roulette.RouletteBet;
import com.mbolt.casino.game.roulette.RouletteRound;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;

/** Minecraft persistence adapter. Pure round validation also applies to restored data. */
public final class RouletteRoundNbt {
    private static final int FORMAT_VERSION = 1;

    private RouletteRoundNbt() {
    }

    public static CompoundTag save(RouletteRound round) {
        var saved = round.snapshot();
        var tag = new CompoundTag();
        tag.putInt("Version", FORMAT_VERSION);
        tag.putUUID("Id", saved.id());
        tag.putInt("BettingTicks", saved.bettingTicks());
        tag.putInt("SpinningTicks", saved.spinningTicks());
        tag.putInt("PayoutTicks", saved.payoutTicks());
        tag.putString("Phase", saved.phase().name());
        tag.putLong("Deadline", saved.deadline());
        tag.putInt("Number", saved.number());
        tag.putBoolean("Settled", saved.settled());
        var bets = new ListTag();
        for (var placed : saved.bets()) {
            var row = new CompoundTag();
            row.putUUID("Id", placed.id());
            row.putUUID("Player", placed.player());
            row.putString("Type", placed.bet().type().name());
            row.putInt("Selection", placed.bet().selection());
            row.putLong("Chips", placed.bet().chips());
            bets.add(row);
        }
        tag.put("Bets", bets);
        return tag;
    }

    /** Never silently discard corrupt stakes or redraw a missing result. The caller must quarantine bad data. */
    public static RouletteRound load(CompoundTag tag) {
        if (tag.getInt("Version") != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported roulette round version");
        }
        if (!tag.contains("Bets", Tag.TAG_LIST) || !tag.contains("Phase", Tag.TAG_STRING)
                || !tag.contains("Number", Tag.TAG_INT) || !tag.contains("Deadline", Tag.TAG_LONG)
                || !tag.contains("Settled", Tag.TAG_BYTE) || !tag.hasUUID("Id")
                || !tag.contains("BettingTicks", Tag.TAG_INT) || !tag.contains("SpinningTicks", Tag.TAG_INT)
                || !tag.contains("PayoutTicks", Tag.TAG_INT)) {
            throw new IllegalArgumentException("Incomplete saved roulette round");
        }
        var bets = new ArrayList<RouletteRound.PlacedBet>();
        ListTag rows = tag.getList("Bets", Tag.TAG_COMPOUND);
        if (rows.size() > RouletteRound.MAX_BETS) {
            throw new IllegalArgumentException("Too many saved roulette bets");
        }
        for (Tag entry : rows) {
            var row = (CompoundTag) entry;
            if (!row.contains("Selection", Tag.TAG_INT) || !row.contains("Chips", Tag.TAG_LONG)) {
                throw new IllegalArgumentException("Incomplete saved roulette bet");
            }
            var type = RouletteBet.Type.valueOf(row.getString("Type"));
            var bet = new RouletteBet(type, row.getInt("Selection"), row.getLong("Chips"));
            bets.add(new RouletteRound.PlacedBet(row.getUUID("Id"), row.getUUID("Player"), bet));
        }
        var snapshot = new RouletteRound.Snapshot(tag.getUUID("Id"), tag.getInt("BettingTicks"),
                tag.getInt("SpinningTicks"), tag.getInt("PayoutTicks"),
                RouletteRound.Phase.valueOf(tag.getString("Phase")), tag.getLong("Deadline"),
                tag.getInt("Number"), bets, tag.getBoolean("Settled"));
        return RouletteRound.restore(snapshot);
    }
}
