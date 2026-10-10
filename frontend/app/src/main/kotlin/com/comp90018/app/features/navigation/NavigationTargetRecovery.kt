package com.comp90018.app.features.navigation

/*
 * Decides whether guidance should close after a requested treasure disappears.
 * Keeps the target pending while the catalogue is still loading.
 */

/** A loading catalogue may still resolve the requested target; a settled miss exits guidance. */
internal fun shouldCancelRelicNavigation(
    requestedTargetId: String?,
    targetResolved: Boolean,
    catalogueLoading: Boolean,
): Boolean = requestedTargetId != null && !targetResolved && !catalogueLoading
