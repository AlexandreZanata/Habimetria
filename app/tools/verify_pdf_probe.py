"""Check only the explicitly synthetic PdfDeviceRunner outputs; no user data."""
import json
import re
import sys
from pathlib import Path

import pdfplumber
from pypdf import PdfReader

CORNERS = [[0, 1, 2, 3], [1, 0, 3, 2], [3, 2, 1, 0], [2, 3, 0, 1],
           [0, 2, 1, 3], [2, 0, 3, 1], [3, 1, 2, 0], [1, 3, 0, 2]]
COLORS = [(255, 0, 0), (0, 255, 0), (0, 0, 255), (255, 255, 0)]
CAPTION = r"Ambiente sintético \d+ · Suíte São João — nº (\d+)"


def normalized(text):
    return re.sub(r"\s+", " ", text).strip()


def verify(root, label, expected):
    manifest = json.loads((root / label / "manifest.json").read_text())
    rows = manifest["evidences"]
    assert len(rows) == expected
    with pdfplumber.open(root / f"{label}.pdf") as pdf:
        texts = [page.extract_text() or "" for page in pdf.pages]
        bodies = [page.crop((39, 63, 556, 787)).extract_text() or "" for page in pdf.pages]
        text = normalized("\n".join(bodies))
        for finding in manifest["findings"]:
            assert normalized(finding["note"]) in text, "incomplete note"
        placements = 0
        for number, page in enumerate(pdf.pages, 1):
            assert page.width == 595 and page.height == 842
            assert f"{number:02d} / {len(pdf.pages):02d}" in texts[number - 1]
            for char in page.chars:
                assert 0 <= char["x0"] < 595 and char["x1"] < 595.5
                assert 0 <= char["top"] < 842 and char["bottom"] < 842.5
            captions = re.findall(CAPTION, texts[number - 1])
            if not captions:
                continue
            assert len(captions) == len(page.images) == 1
            row = rows[int(captions[0]) - 1]
            dims = row["displayMetadata"]
            image = page.images[0]
            assert image["x0"] >= 39 and image["x1"] <= 556
            assert image["top"] >= 63 and image["bottom"] <= 787
            aspect = (image["x1"] - image["x0"]) / (image["bottom"] - image["top"])
            assert abs(aspect - dims["displayWidth"] / dims["displayHeight"]) < .001
            placements += 1
        assert text.count("Categoria da foto:") == expected
        assert placements == expected
        assert sum(len(page.images) for page in pdf.pages) == expected + 1
        sequence = [int(x) for value in texts for x in re.findall(CAPTION, value)]
        assert sorted(sequence) == list(range(1, expected + 1))
        summary = pdf.pages[next(i for i, text in enumerate(texts) if "Síntese e navegação" in text)]
        room_names = normalized(summary.crop((126, 239, 312, 435)).extract_text() or "")
        references = (summary.crop((470, 239, 524, 435)).extract_text() or "").splitlines()
        assert len(references) == 5
        for index, reference in enumerate(references, 1):
            label_text = f"Ambiente sintético {index} · Suíte São João"
            assert label_text in room_names
            room_page = pdf.pages[int(reference) - 1]
            title = normalized(room_page.crop((84, 95, 410, 210)).extract_text() or "")
            assert f"03.{index:02d} / REGISTRO POR AMBIENTE" in title and label_text in title
            assert "Cobertura por categorias:" in text
        pages = len(pdf.pages)
    reader = PdfReader(root / f"{label}.pdf")
    matched = 0
    for page in reader.pages:
        captions = re.findall(CAPTION, page.extract_text())
        if not captions:
            continue
        row = rows[int(captions[0]) - 1]
        dims = row["displayMetadata"]
        image = page.images[0].image.convert("RGB")
        width, height = image.size
        assert abs(width / height - dims["displayWidth"] / dims["displayHeight"]) < .001
        for corner, (x, y) in enumerate([(.2, .2), (.8, .2), (.2, .8), (.8, .8)]):
            actual = image.getpixel((int(width * x), int(height * y)))
            wanted = COLORS[CORNERS[dims["orientation"] - 1][corner]]
            assert max(abs(a - b) for a, b in zip(actual, wanted)) < 25
        matched += 1
    assert matched == expected
    integrity = next(page for page in reader.pages if "Integridade e autoria" in page.extract_text())
    assert len(integrity.images) == 1
    integrity.images[0].image.save(root / f"{label}-qr.png")
    return {"pages": pages, "photos": expected, "embedded_images_matched": matched,
            "complete_notes": True, "numbering_index_and_bounds": True,
            "bytes": (root / f"{label}.pdf").stat().st_size}


if __name__ == "__main__":
    directory = Path(sys.argv[1])
    results = {label: verify(directory, label, count)
               for label, count in [("rental", 100), ("technical", 20)]}
    (directory / "pdf-probe-verdict.json").write_text(json.dumps(results, indent=2))
    print(json.dumps(results, indent=2))
