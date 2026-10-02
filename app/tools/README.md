# Native report artifact checks

- [verify_pdf_probe.py](verify_pdf_probe.py) checks synthetic native probe PDFs:
  all image placements/EXIF transforms, complete notes, categories, actual
  numbering/index, A4 bounds and embedded QR extraction. This is fixture QA,
  not a general PDF/legal conformity validator. Requires Python, pdfplumber,
  pypdf and Pillow in the local workspace tooling; no app runtime dependency.

Run `python verify_pdf_probe.py /absolute/path/to/probe` after extracting
`rental.zip` and `technical.zip` into matching folder names. Render every
page with Poppler and visually inspect them separately; extraction cannot
establish visual quality. Validate both packages with `backend/cmd/verify`.
