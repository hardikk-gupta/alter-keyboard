package com.aistudio.altermind.voice

import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

/** Prevent a canceled worker finishing installation after the user deletes its model. */
internal object ModelOperations {
    private val locks = ConcurrentHashMap<String, Mutex>()
    fun lock(id: String): Mutex = locks.getOrPut(id) { Mutex() }
}
