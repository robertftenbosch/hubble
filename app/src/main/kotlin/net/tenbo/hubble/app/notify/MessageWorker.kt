package net.tenbo.hubble.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import net.tenbo.hubble.app.data.HubbleDb
import net.tenbo.hubble.app.data.KeystoreVault
import net.tenbo.hubble.app.net.HubbleApi

/**
 * Periodically polls our relay mailbox in the background and raises a local notification
 * for each new message. No FCM / Google push — the device pulls its own opaque mailbox,
 * so the server still learns nothing. Near-real-time (WorkManager's floor is ~15 min);
 * instant delivery rides the WebRTC data channel when the app is open.
 */
class MessageWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val vault = KeystoreVault(applicationContext)
        if (!vault.hasIdentity()) return Result.success() // nothing to poll for yet

        val db = HubbleDb.get(applicationContext, vault)
        val api = HubbleApi()

        val fresh = runCatching {
            Inbox.poll(
                context = applicationContext,
                vault = vault,
                api = api,
                friendDao = db.friendDao(),
                matchDao = db.matchDao(),
                messageDao = db.messageDao(),
                postDao = db.postDao(),
                blockDao = db.blockDao(),
            )
        }.getOrElse { return Result.retry() } // transient (network) — let WorkManager back off

        fresh.forEach { Notifications.showMessage(applicationContext, it.matchHubbleId, it.name, it.text) }
        return Result.success()
    }
}
