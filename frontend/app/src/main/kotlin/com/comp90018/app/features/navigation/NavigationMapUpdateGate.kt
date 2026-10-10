package com.comp90018.app.features.navigation

/*
 * Avoids redundant map-scene updates within one navigation session.
 * Guiding-thread animation has its own frame updates and is excluded from the scene-change key.
 */

/** Session-local gate: thread animation inputs deliberately do not belong to the scene key. */
internal class NavigationMapUpdateGate {
    private var previousScene: List<Any?>? = null
    var disposed: Boolean = false
        private set

    fun shouldUpdateScene(scene: List<Any?>): Boolean {
        if (disposed || scene == previousScene) return false
        previousScene = scene.toList()
        return true
    }

    fun dispose() {
        disposed = true
        previousScene = null
    }
}
