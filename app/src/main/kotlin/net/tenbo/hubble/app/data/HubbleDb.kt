package net.tenbo.hubble.app.data

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Process-wide singleton for the encrypted Room database. Both the UI (MainActivity)
 * and the background poll worker must talk to the *same* instance — Room warns against
 * opening two handles on one file. Keyed by the Keystore-wrapped DB key.
 */
object HubbleDb {
    @Volatile private var instance: HubbleDatabase? = null

    fun get(context: Context, vault: KeystoreVault): HubbleDatabase =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext, vault).also { instance = it }
        }

    private fun build(appContext: Context, vault: KeystoreVault): HubbleDatabase {
        // Remove any legacy plaintext database from before encryption was added.
        appContext.deleteDatabase("hubble.db")
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(vault.databaseKey())
        return Room.databaseBuilder(appContext, HubbleDatabase::class.java, "hubble-secure.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }
}
