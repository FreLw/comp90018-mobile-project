package com.comp90018.app.data.treasure

import com.comp90018.app.data.social.Subscription

/** Per-user persistence boundary for discovered treasures. */
interface TreasureCollectionRepository {
    fun observeDiscoveredTreasureIds(
        userId: String,
        onChange: (Set<String>, String?) -> Unit,
    ): Subscription

    fun addDiscoveredTreasure(
        userId: String,
        treasureId: String,
        onComplete: (String?) -> Unit,
    )
}
