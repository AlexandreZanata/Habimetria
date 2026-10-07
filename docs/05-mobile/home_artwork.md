# Home artwork

- [home_brazil_house.png](../../app/composeApp/src/commonMain/composeResources/drawable/home_brazil_house.png): generated
  transparent Brazilian house illustration used in the Home banner and as
  a decorative fallback Property cover. Saved photos always take precedence.
  No generated artwork is stored as Property data or Evidence.

Generated with the built-in ImageGen tool on 2026-10-07, inspired by the
user's Home reference. Native Compose supplies all text, backgrounds,
icons and buttons. Transparency lets the artwork use light/dark theme roles.

## Validation

The 2026-10-07 code delivery passed `make verify`, the repository secret
scan and `make verify-mobile` (shared JVM tests, shared Kotlin compilation
for iOS Simulator Arm64, Android debug APK). The APK includes the new PNG
and removes both previous decorative JPEGs. Device appearance, 200% font
scale, TalkBack, iOS host rendering and user-session acceptance remain
unverified, manual and user-owned. No PDF behavior changes in this slice.

## Final generation prompt

```text
Use case: ads-marketing
Asset type: transparent decorative hero image for a Brazilian mobile home-inspection app.
Primary request: a welcoming, affordable everyday Brazilian single-storey family house. Terracotta clay tile hip roof, white painted masonry walls, modest covered varanda with wooden door, simple aluminum-framed windows, a low front wall, a small lawn and a few tropical shrubs. Contemporary but recognizably Brazilian, ordinary middle-class neighbourhood scale, not a luxury mansion.
Style/medium: polished realistic architectural photography cutout, natural warm daylight, crisp but soft friendly lighting.
Composition/framing: landscape 3:2 canvas. Complete house in a three-quarter frontal view centred in the canvas with breathing room on all sides; roof and walls fully visible. Small ground patch and soft contact shadow only, no sky or distant background.
Color palette: terracotta, warm white, natural leafy greens, compatible with mint and forest-green UI.
Text: none.
Constraints: actual transparent background, no people, cars, signage, typography, watermark, luxury glass balconies or multi-storey facade. This is an illustration only, never a user's actual property photo. Output will sit on the right side of a compact mint banner with app text kept separately in accessible native UI.
```
