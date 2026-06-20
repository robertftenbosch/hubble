package net.tenbo.hubble.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import net.tenbo.hubble.core.friend.FriendRecord
import net.tenbo.hubble.core.message.ChatMessage

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val hubbleId: String,
    val displayName: String,
    val signingPublicKey: ByteArray,
    val agreementPublicKey: ByteArray,
    val rootKey: ByteArray,
    val establishedAtEpochMs: Long,
) {
    fun toRecord() = FriendRecord(
        hubbleId, displayName, signingPublicKey, agreementPublicKey, rootKey, establishedAtEpochMs,
    )

    companion object {
        fun from(r: FriendRecord) = FriendEntity(
            r.hubbleId, r.displayName, r.signingPublicKey, r.agreementPublicKey, r.rootKey,
            r.establishedAtEpochMs,
        )
    }
}

@Dao
interface FriendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friend: FriendEntity)

    @Query("SELECT * FROM friends ORDER BY establishedAtEpochMs DESC")
    suspend fun all(): List<FriendEntity>
}

/**
 * An ephemeral post in the local feed — either one we authored (`mine = true`) or
 * one received from a friend. [expiresAtEpochMs] drives TTL filtering in queries.
 */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorHubbleId: String,
    val authorName: String,
    val text: String,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val mine: Boolean,
)

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(post: PostEntity)

    /** Non-expired posts, newest first. */
    @Query("SELECT * FROM posts WHERE expiresAtEpochMs > :nowMs ORDER BY createdAtEpochMs DESC")
    suspend fun active(nowMs: Long): List<PostEntity>

    @Query("DELETE FROM posts WHERE expiresAtEpochMs <= :nowMs")
    suspend fun purgeExpired(nowMs: Long)
}

/**
 * Someone you physically crossed paths with (via BLE proximity + profile exchange).
 * Carries their profile plus the encounter metadata that drives the coordinate stamp:
 * [place], [lastSeenEpochMs], and [count] of crossings. [decided]/[liked] track your call.
 */
@Entity(tableName = "encounters")
data class EncounterEntity(
    @PrimaryKey val hubbleId: String,
    val name: String,
    val age: Int,
    val city: String,
    val bio: String,
    val promptAnswer: String,
    val photoPath: String?,
    val place: String,
    val lastSeenEpochMs: Long,
    val count: Int,
    val decided: Boolean,
    val liked: Boolean,
    /** Whether this person already liked you — a like from you completes the match. */
    val likesYouBack: Boolean,
    // Keys from the in-person handshake, carried into a match for E2E chat.
    val signingPublicKey: ByteArray,
    val agreementPublicKey: ByteArray,
    val rootKey: ByteArray,
)

@Dao
interface EncounterDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNew(encounter: EncounterEntity)

    /** Undecided crossings, most recent first. */
    @Query("SELECT * FROM encounters WHERE decided = 0 ORDER BY lastSeenEpochMs DESC")
    suspend fun pending(): List<EncounterEntity>

    @Query("SELECT COUNT(*) FROM encounters")
    suspend fun total(): Int

    @Query("UPDATE encounters SET decided = 1, liked = :liked WHERE hubbleId = :hubbleId")
    suspend fun decide(hubbleId: String, liked: Boolean)

    @Query("DELETE FROM encounters WHERE hubbleId = :hubbleId")
    suspend fun delete(hubbleId: String)
}

/** A mutual like: someone you crossed paths with who also liked you. */
@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val hubbleId: String,
    val name: String,
    val age: Int,
    val city: String,
    val photoPath: String?,
    val place: String,
    /** Keys from the in-person handshake; seed E2E chat. */
    val signingPublicKey: ByteArray,
    val agreementPublicKey: ByteArray,
    val rootKey: ByteArray,
    val matchedAtEpochMs: Long,
    val lastActivityEpochMs: Long,
) {
    /** The record used to seal/open E2E messages with this match. */
    fun toFriendRecord() = FriendRecord(
        hubbleId, name, signingPublicKey, agreementPublicKey, rootKey, matchedAtEpochMs,
    )
}

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val matchHubbleId: String,
    val fromMe: Boolean,
    val text: String,
    val sentAtEpochMs: Long,
    /** "text" or "voice". Voice rows carry [audioPath] + [durationMs]; [text] is a label. */
    val kind: String = "text",
    val audioPath: String? = null,
    val durationMs: Long = 0,
)

@Dao
interface MatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(match: MatchEntity)

    @Query("SELECT * FROM matches ORDER BY lastActivityEpochMs DESC")
    suspend fun all(): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE hubbleId = :hubbleId")
    suspend fun byId(hubbleId: String): MatchEntity?

    @Query("UPDATE matches SET lastActivityEpochMs = :at WHERE hubbleId = :hubbleId")
    suspend fun touch(hubbleId: String, at: Long)

    @Query("DELETE FROM matches WHERE hubbleId = :hubbleId")
    suspend fun delete(hubbleId: String)
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE matchHubbleId = :matchId ORDER BY sentAtEpochMs ASC")
    suspend fun forMatch(matchId: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE matchHubbleId = :matchId")
    suspend fun deleteForMatch(matchId: String)
}

/** Someone you've blocked: hidden from discovery, can't reach you, optionally reported. */
@Entity(tableName = "blocks")
data class BlockEntity(
    @PrimaryKey val hubbleId: String,
    val reported: Boolean,
    val atEpochMs: Long,
)

@Dao
interface BlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun block(b: BlockEntity)

    @Query("SELECT hubbleId FROM blocks")
    suspend fun allIds(): List<String>
}

@Database(
    entities = [
        FriendEntity::class, PostEntity::class, EncounterEntity::class,
        MatchEntity::class, MessageEntity::class, BlockEntity::class,
    ],
    version = 7,
)
abstract class HubbleDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
    abstract fun postDao(): PostDao
    abstract fun encounterDao(): EncounterDao
    abstract fun matchDao(): MatchDao
    abstract fun messageDao(): MessageDao
    abstract fun blockDao(): BlockDao
}
