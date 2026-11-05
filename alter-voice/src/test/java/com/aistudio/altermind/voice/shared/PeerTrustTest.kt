package com.aistudio.altermind.voice.shared

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeerTrustTest {
    private val certificate = "a".repeat(64)
    private val host = peer(PeerTrust.KEYBOARD, 10001)
    private fun peer(name: String, uid: Int) = InstalledPeer(name, uid, setOf(name), setOf(certificate))
    private val alter = peer(PeerTrust.ALTER, 10002)

    @Test fun matchingCurrentCertificatesAllowBothPeers() {
        assertTrue(PeerTrust.accepts(host, alter, alter.uid))
        val fitdrobe = peer(PeerTrust.FITDROBE, 10003)
        assertTrue(PeerTrust.accepts(host, fitdrobe, fitdrobe.uid))
    }

    @Test fun packageOrUidSpoofingIsRejected() {
        assertFalse(PeerTrust.accepts(host, alter, 10003))
        assertFalse(PeerTrust.accepts(host, peer("untrusted.app", 10002), 10002))
        assertFalse(PeerTrust.accepts(host, alter.copy(uid = host.uid), host.uid))
        assertFalse(PeerTrust.accepts(host, host, host.uid))
    }

    @Test fun sharedUidsAreRejectedOnEitherSide() {
        assertFalse(PeerTrust.accepts(host, alter.copy(packagesForUid = setOf(alter.packageName, "other")), alter.uid))
        assertFalse(PeerTrust.accepts(host.copy(packagesForUid = emptySet()), alter, alter.uid))
    }

    @Test fun signerMismatchOrMissingIdentityFailsClosed() {
        assertFalse(PeerTrust.accepts(host, alter.copy(currentSigners = setOf("b".repeat(64))), alter.uid))
        assertFalse(PeerTrust.accepts(host, alter.copy(currentSigners = emptySet()), alter.uid))
        assertFalse(PeerTrust.accepts(host, alter.copy(currentSigners = setOf(certificate, "b".repeat(64))), alter.uid))
        assertFalse(PeerTrust.accepts(host, alter.copy(currentSigners = setOf("not a certificate")), alter.uid))
    }

    @Test fun invalidHostCannotAuthorizePeers() {
        assertFalse(PeerTrust.accepts(host.copy(packageName = "other"), alter, alter.uid))
        assertFalse(PeerTrust.accepts(host.copy(currentSigners = emptySet()), alter, alter.uid))
    }
}
