package net.tenbo.hubble.core.transport

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class InMemoryTransportTest {
    @Test fun `bytes sent on one end arrive on the other`() = runTest {
        val (alice, bob) = InMemoryTransport.pair()
        launch { alice.send(byteArrayOf(1, 2, 3)) }
        assertContentEquals(byteArrayOf(1, 2, 3), bob.receive())
    }
}
