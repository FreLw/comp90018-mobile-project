package com.comp90018.app.features.haptics

/** Referential token: an old session's token can never match one created after sign-in. */
class TreasureHapticAttempt internal constructor(
    val treasureId: String,
    val completionId: String,
    internal val owner: String,
)
