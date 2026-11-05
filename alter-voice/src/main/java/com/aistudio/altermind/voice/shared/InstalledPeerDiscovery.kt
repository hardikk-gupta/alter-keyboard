package com.aistudio.altermind.voice.shared

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import java.security.MessageDigest

/** Trust/discovery groundwork only: never binds, starts a service, or reads model files. */
internal class InstalledPeerDiscovery(context: Context) {
    private val packageManager = context.packageManager
    private val hostPackage = context.packageName

    /** Call synchronously inside each future AIDL method, before clearing Binder identity. */
    fun requireCaller(): String {
        val uid = Binder.getCallingUid()
        val names = packageManager.getPackagesForUid(uid)?.toSet().orEmpty()
        if (names.size != 1) throw SecurityException("Shared AI requires an exclusive package UID")
        val name = names.single()
        requireTrusted(name, uid)
        return name
    }

    /** Recheck both before binding and in onServiceConnected; discovery is not a trust cache. */
    fun requireProvider(component: ComponentName) {
        val info = packageManager.getServiceInfo(component, 0)
        if (!info.enabled || !info.exported || !info.applicationInfo.enabled) {
            throw SecurityException("Shared AI service is unavailable")
        }
        requireTrusted(component.packageName, info.applicationInfo.uid)
    }

    /** Package-specific queries only. Candidates still need a protocol/initialization handshake. */
    fun discover(): List<ComponentName> = (PeerTrust.packages - hostPackage).sorted().mapNotNull { name ->
        try {
            requireTrusted(name, installed(name).uid)
            val matches = packageManager.queryIntentServices(Intent(PeerTrust.ACTION).setPackage(name), 0)
            val service = matches.singleOrNull()?.serviceInfo ?: return@mapNotNull null
            val component = ComponentName(service.packageName, service.name)
            requireProvider(component)
            component
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    private fun requireTrusted(name: String, uid: Int) {
        if (name !in PeerTrust.packages || hostPackage != PeerTrust.KEYBOARD ||
            !PeerTrust.accepts(installed(hostPackage), installed(name), uid)) {
            throw SecurityException("Shared AI signer or UID mismatch; compatible signing is required")
        }
    }

    @Suppress("DEPRECATION")
    private fun installed(name: String): InstalledPeer {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
            else PackageManager.GET_SIGNATURES
        val info = packageManager.getPackageInfo(name, flags)
        val uid = info.applicationInfo?.uid ?: throw SecurityException("Missing installed application identity")
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners
            else info.signatures
        val digests = signatures.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }.toSet()
        return InstalledPeer(name, uid, packageManager.getPackagesForUid(uid)?.toSet().orEmpty(), digests)
    }
}
