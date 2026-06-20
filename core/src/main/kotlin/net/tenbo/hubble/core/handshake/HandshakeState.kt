package net.tenbo.hubble.core.handshake

import net.tenbo.hubble.core.friend.FriendRecord

/** Observable states of the handshake state machine. */
sealed interface HandshakeState {
    data object Idle : HandshakeState
    data object HelloSent : HandshakeState

    /** Secret derived; UI must show [sasEmoji] for in-person comparison. */
    data class AwaitingSasConfirmation(val sasEmoji: List<String>) : HandshakeState
    data class Completed(val friend: FriendRecord) : HandshakeState
    data class Aborted(val reason: String) : HandshakeState
}
