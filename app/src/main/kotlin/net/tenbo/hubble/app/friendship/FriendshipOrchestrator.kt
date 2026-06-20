package net.tenbo.hubble.app.friendship

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.tenbo.hubble.app.data.FriendDao
import net.tenbo.hubble.app.data.FriendEntity
import net.tenbo.hubble.core.crypto.BouncyCastleCrypto
import net.tenbo.hubble.core.handshake.Handshake
import net.tenbo.hubble.core.handshake.HandshakeState
import net.tenbo.hubble.core.identity.Identity
import net.tenbo.hubble.core.transport.Transport

/**
 * Bridges a connected BLE [Transport] to the pure-core [Handshake], surfaces the
 * [HandshakeState] (including the SAS) to the UI, and persists the resulting
 * [FriendEntity] on success.
 */
class FriendshipOrchestrator(
    private val self: Identity,
    private val displayName: String,
    private val dao: FriendDao,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {
    private val crypto = BouncyCastleCrypto()
    private val _state = MutableStateFlow<HandshakeState>(HandshakeState.Idle)
    val state: StateFlow<HandshakeState> = _state

    private lateinit var handshake: Handshake

    /** Exchange keys and derive the SAS; transitions to AwaitingSasConfirmation. */
    suspend fun begin(transport: Transport) {
        handshake = Handshake(crypto, self, displayName, transport, clockMs)
        _state.value = handshake.start()
    }

    /** Called once the user confirms the SAS matched in person. */
    suspend fun confirmSas() {
        val result = handshake.confirmSas()
        if (result is HandshakeState.Completed) {
            dao.upsert(FriendEntity.from(result.friend))
        }
        _state.value = result
    }

    fun reset() { _state.value = HandshakeState.Idle }
}
