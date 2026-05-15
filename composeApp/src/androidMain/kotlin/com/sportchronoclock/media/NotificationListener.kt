package com.sportchronoclock.media

import android.service.notification.NotificationListenerService

/**
 * Empty NotificationListenerService — its existence is what lets the user grant
 * "Notification access" to SportChronoClock, which unlocks
 * [android.media.session.MediaSessionManager.getActiveSessions].
 */
class NotificationListener : NotificationListenerService()
