# Native PDF probes

- [PdfDeviceRunner.kt](PdfDeviceRunner.kt): real Android PDF/signature/ZIP checks
  in an isolated synthetic directory; never opens user data.
- [DevicePhotoFixture.kt](DevicePhotoFixture.kt): asymmetric images and native
  assertions for all eight EXIF orientations and mirror transforms.
- [DeviceQrFixture.kt](DeviceQrFixture.kt): 100 varied native QR identities
  decoded with normal detection; shares the production bitmap function.
- [DevicePdfFixture.kt](DevicePdfFixture.kt): rental (100 photos), technical
  (20 photos), long Unicode note and explicitly simulated/absent GPS claims.

Build `:composeApp:assembleDebugAndroidTest`, install the debug and test APKs
without clearing data, then run
`adb shell am instrument -w vault.app.test/vault.pdf.PdfDeviceRunner`.
Pull only the `pdf-device-probe` artifacts for independent visual/Go checks.
Instrumented output records page counts, total render/export time and sampled
process PSS (not a continuous allocation profile).
