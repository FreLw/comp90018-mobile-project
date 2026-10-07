package com.comp90018.app.features.navigation

/** A loading catalogue may still resolve the requested target; a settled miss exits guidance. */
internal fun shouldCancelRelicNavigation(
    requestedTargetId: String?,
    targetResolved: Boolean,
    catalogueLoading: Boolean,
): Boolean = requestedTargetId != null && !targetResolved && !catalogueLoading
