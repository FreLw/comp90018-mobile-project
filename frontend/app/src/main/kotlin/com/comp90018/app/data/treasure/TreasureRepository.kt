package com.comp90018.app.data.treasure

import com.comp90018.app.data.social.Subscription
import com.comp90018.app.features.map.MapRelic

/** Read-only boundary for the shared treasure catalogue. */
interface TreasureRepository {
    fun observeEnabledTreasures(
        onChange: (List<MapRelic>, String?) -> Unit,
    ): Subscription
}
