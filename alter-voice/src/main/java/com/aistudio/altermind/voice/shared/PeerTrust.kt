package com.aistudio.altermind.voice.shared

/** Current installed identity only. Signing history is deliberately not a trust input. */
internal data class InstalledPeer(
    val packageName: String,
    val uid: Int,
    val packagesForUid: Set<String>,
    val currentSigners: Set<String>
)

internal object PeerTrust {
    const val KEYBOARD = "com.aistudio.altermind.keyboard"
    const val ALTER = "com.aistudio.altermind.ai"
    const val FITDROBE = "com.aistudio.fitdrobe.wkbvca"
    const val ACTION = "com.alter.localai.v1.BIND"
    val packages = setOf(KEYBOARD, ALTER, FITDROBE)
    private val sha256 = Regex("[0-9a-f]{64}")

    fun accepts(host: InstalledPeer, peer: InstalledPeer, callingUid: Int): Boolean =
        host.packageName == KEYBOARD && peer.packageName in packages &&
            peer.packageName != host.packageName && callingUid == peer.uid &&
            host.uid != peer.uid && valid(host) && valid(peer) &&
            host.currentSigners == peer.currentSigners

    private fun valid(peer: InstalledPeer): Boolean = peer.uid >= 0 &&
        peer.packagesForUid == setOf(peer.packageName) && peer.currentSigners.isNotEmpty() &&
        peer.currentSigners.all { sha256.matches(it) }
}
