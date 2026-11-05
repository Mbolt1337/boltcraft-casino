"""Review sheets for Maxim: preview screenshots in a grid with Russian captions, written to build/review/.

    python art/review_sheets.py [sheet ...]

Take the screenshots first with the art preview (gradlew runClient -PartPreview [-PpreviewScene=decor|structures]).
SHEETS lists the last review; replace it with the shots of the next one. Missing screenshots are skipped.
"""
from pathlib import Path
import sys

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SHOTS = ROOT / 'run-preview/screenshots'
OUT = ROOT / 'build/review'
FONT = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 22)
TITLE = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 34)
CELL_W, CELL_H = 640, 360
PAD = 14
CAPTION = 34


def sheet(name, title, items, columns=2):
    rows = (len(items) + columns - 1) // columns
    width = columns * CELL_W + (columns + 1) * PAD
    height = 70 + rows * (CELL_H + CAPTION + PAD) + PAD
    image = Image.new('RGB', (width, height), (24, 18, 22))
    draw = ImageDraw.Draw(image)
    draw.text((PAD, 16), title, font=TITLE, fill=(242, 210, 122))
    for i, (shot, caption) in enumerate(items):
        x = PAD + (i % columns) * (CELL_W + PAD)
        y = 70 + (i // columns) * (CELL_H + CAPTION + PAD)
        picture = Image.open(SHOTS / f'{shot}.png').convert('RGB').resize((CELL_W, CELL_H), Image.LANCZOS)
        image.paste(picture, (x, y))
        draw.text((x + 4, y + CELL_H + 4), caption, font=FONT, fill=(232, 226, 200))
    OUT.mkdir(parents=True, exist_ok=True)
    image.save(OUT / f'{name}.png')
    print(OUT / f'{name}.png')


SHEETS = {
    'review_1_neon': ('Неон: буквы, трубки, вывески, панно, бегущие огни', [
        ('decor_facade', 'Надпись из неоновых букв в рамке из трубки, панно в бегущих огнях'),
        ('decor_night_facade', 'То же ночью'),
        ('decor_letters_angle', 'Буквы — объёмные трубки на стене, ореол на стене'),
        ('decor_panels', 'Панно BOLTCRAFT, JACKPOT, BAR, VIP'),
        ('decor_signs', 'Вывески-иконки: фишка, 7, звезда, карты, кости, корона, бокал'),
        ('gui_neon_picker', 'Окошко выбора знака (можно набрать на клавиатуре)'),
    ]),
    'review_2_interior': ('Интерьер: бар, лаунж, мебель, свет', [
        ('decor_overview', 'Весь шоурум декора днём'),
        ('decor_night_overview', 'Ночью'),
        ('decor_bar', 'Барная стойка: прямая, внутренний и внешний углы, стулья'),
        ('decor_bar_back', 'Сторона бармена: полки с бутылками'),
        ('decor_lounge', 'Диван (соединяется), столик, кадки с пальмами, бра'),
        ('decor_card_table', 'Карточный стол со стульями'),
        ('decor_ropes', 'Бархатные канаты 4 цветов, большая люстра 3×3'),
        ('decor_columns', 'Колонны (база и капитель сами), занавес с карнизом и бахромой'),
        ('decor_floors', 'Ковры 4 узоров, мраморная и шахматная плитка, ступени и плиты'),
        ('gui_decor_tab', 'Вкладка «BoltCraft Casino: декор»'),
    ]),
    'review_3_casinos': ('Казино в мире: неоновый лаунж и большой зал', [
        ('casino_lounge_facade', 'Неоновый лаунж: козырёк на колоннах, огни, панно, CASINO'),
        ('casino_night_lounge', 'Лаунж ночью'),
        ('casino_lounge_hall', 'Внутри: автоматы под JACKPOT, бар справа'),
        ('casino_lounge_bar', 'Бар лаунжа'),
        ('casino_lounge_cashier', 'Касса у входа и дверь в сейфовую'),
        ('casino_lounge_aerial', 'Сверху'),
        ('casino_hall_facade', 'Большой зал: портик на 4 колоннах, панно, C A S I N O'),
        ('casino_night_hall', 'Большой зал ночью'),
        ('casino_hall_atrium', 'Атриум в два света и большая люстра'),
        ('casino_hall_slots', 'Зал автоматов'),
        ('casino_hall_roulette', 'Рулетка (крупье появится после привязки стола)'),
        ('casino_hall_bar', 'Бар зала'),
        ('casino_hall_gallery', 'VIP-галерея на втором этаже за канатами'),
        ('casino_hall_vault', 'Сейфовая комната: пустой ничейный сейф и сундуки'),
    ]),
    'review_4_croupier': ('Крупье говорит в облачке', [
        ('roulette_spinning', '«Ставок больше нет!» на 40-м тике вращения'),
        ('roulette_announce', 'Результат с цветом: «19, красное!»'),
    ]),
    'review_6_concept_facades': ('Казино по концептам: фасады', [
        ('casino_lounge_facade', 'Лаунж: вывеска, гребень из фишек, козырёк, баннеры'),
        ('casino_night_lounge', 'Лаунж ночью'),
        ('casino_lounge_angle', 'Лаунж под углом: пилоны, золото, красный неон'),
        ('casino_lounge_entrance', 'Вход лаунжа: ковёр по ступеням, фонари'),
        ('casino_hall_facade', 'Большой зал: башня с вывеской и фишками, навес-терраса'),
        ('casino_night_hall', 'Большой зал ночью'),
        ('casino_hall_angle', 'Большой зал под углом: фонтан, живые изгороди'),
        ('casino_hall_portico', 'Под навесом: опоры с золотыми капителями, фонари'),
        ('casino_hall_side', 'Боковой фасад'),
        ('casino_hall_aerial', 'Сверху'),
    ]),
    'review_7_concept_interiors': ('Казино по концептам: внутри', [
        ('casino_lounge_hall', 'Лаунж: рулетка под люстрой, автоматы вдоль стен'),
        ('casino_lounge_bar', 'Бар лаунжа'),
        ('casino_lounge_cashier', 'Касса и дверь в сейфовую'),
        ('casino_lounge_staff', 'Сейфовая лаунжа'),
        ('casino_hall_atrium', 'Атриум: парадная лестница, люстры, кессоны'),
        ('casino_hall_slots', 'Зал автоматов'),
        ('casino_hall_roulette', 'Игровой зал: рулетки и карточные столы'),
        ('casino_hall_bar', 'Бар под площадкой лестницы'),
        ('casino_hall_gallery', 'Галерея второго этажа над атриумом'),
        ('casino_hall_front_gallery', 'С передней галереи: атриум, лестница, люстры'),
        ('casino_hall_vault', 'VIP-этаж: сейфовая'),
        ('casino_hall_balcony', 'Терраса на навесе'),
    ]),
}

def book():
    """Every spread of the guide book that the decor preview photographed, cut out of the screen."""
    spreads = sorted(SHOTS.glob('gui_guide_*.png'), key=lambda path: int(path.stem.rsplit('_', 1)[1]))
    if not spreads:
        return
    tile_w, tile_h = 445, 280
    columns = 4
    rows = (len(spreads) + columns - 1) // columns
    image = Image.new('RGB', (columns * tile_w + 2 * PAD, rows * tile_h + 70 + PAD), (24, 18, 22))
    ImageDraw.Draw(image).text((PAD, 16), 'Книга-путеводитель: все развороты', font=TITLE, fill=(242, 210, 122))
    for i, path in enumerate(spreads):
        # The book is 296x188 GUI pixels in the middle of a 1280x720 screenshot at GUI scale 3.
        spread = Image.open(path).convert('RGB').crop((195, 80, 1085, 640)).resize((tile_w, tile_h), Image.LANCZOS)
        image.paste(spread, (PAD + (i % columns) * tile_w, 70 + (i // columns) * tile_h))
    OUT.mkdir(parents=True, exist_ok=True)
    image.save(OUT / 'review_5_book.png')
    print(OUT / 'review_5_book.png')


if __name__ == '__main__':
    wanted = sys.argv[1:] or list(SHEETS)
    for key in wanted:
        title, items = SHEETS[key]
        items = [item for item in items if (SHOTS / f'{item[0]}.png').exists()]
        sheet(key, title, items)
    book()
