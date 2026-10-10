package com.comp90018.app.features.haptics

/*
 * Identifies one feedback attempt within a signed-in exploration session.
 * Token identity prevents an old asynchronous result from matching a new session.
 */

/** Referential token: an old session's token can never match one created after sign-in. */
class TreasureHapticAttempt internal constructor(
    val treasureId: String,
    val completionId: String,
    internal val owner: String,
)
