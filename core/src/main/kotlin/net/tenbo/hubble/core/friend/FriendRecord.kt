package net.tenbo.hubble.core.friend

/** A persisted friendship established in person. rootKey seeds later E2E comms. */
data class FriendRecord(
    val hubbleId: String,
    val displayName: String,
    val signingPublicKey: ByteArray,
    val agreementPublicKey: ByteArray,
    val rootKey: ByteArray,
    val establishedAtEpochMs: Long,
)
