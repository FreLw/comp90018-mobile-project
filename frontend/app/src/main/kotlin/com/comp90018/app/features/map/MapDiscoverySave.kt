package com.comp90018.app.features.map



internal fun saveRelicDiscovery(
    relic: MapRelic,
    onCollectTreasure: (String, (String?) -> Unit) -> Unit,
    onComplete: (String?) -> Unit,
) = onCollectTreasure(relic.id, onComplete)
