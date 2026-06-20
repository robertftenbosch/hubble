package net.tenbo.hubble.core.identity

/**
 * A device identity. Private keys are present only in memory; the :app layer
 * persists the recovery phrase encrypted (Keystore-wrapped) and re-derives this.
 * [hubbleId] is the public fingerprint shown to users.
 */
data class Identity(
    val signingPublicKey: ByteArray,
    val signingPrivateKey: ByteArray,
    val agreementPublicKey: ByteArray,
    val agreementPrivateKey: ByteArray,
    val hubbleId: String,
)
