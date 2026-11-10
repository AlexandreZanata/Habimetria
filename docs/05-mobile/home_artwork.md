# Home artwork

[FacadeDrawing.kt](../../app/composeApp/src/commonMain/kotlin/vault/ui/brand/FacadeDrawing.kt)
draws an architectural elevation on a grid, inspired by the line art in
the Habimetria brand board. The vector drawing scales proportionally and
takes its ink from the active theme. It is shared by the Home hero and
Property cover fallbacks, including the Property cover editor. A saved
Property photo takes precedence and is center-cropped.

The artwork is decorative, hidden from accessibility announcements, and
never written to Property storage or included as Evidence.
