package vault.ui

import vault.local.ShotCategory

// Guided shot categories in pt-BR. Coverage, review, seal and compare
// screens share these labels so missing shots read identically.
fun shotLabel(category: ShotCategory): String = when (category) {
    ShotCategory.OVERVIEW -> Strings.shotOverview
    ShotCategory.WALLS -> Strings.shotWalls
    ShotCategory.OPENINGS -> Strings.shotOpenings
    ShotCategory.FLOOR -> Strings.shotFloor
    ShotCategory.CEILING -> Strings.shotCeiling
}
