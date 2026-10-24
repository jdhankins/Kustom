package com.hank.kustom.data.repository

import android.content.Context
import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.HardwareType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repository wrapper for [KarooSystemService] that manages connection lifecycle
 * and provides reactive [Flow]s for data streaming, user profile, ride state, and system configuration.
 */
class KarooSystemRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    val karooSystem: KarooSystemService = KarooSystemService(appContext)

    init {
        connect()
    }

    /**
     * Connects to [KarooSystemService] if not already connected.
     */
    fun connect() {
        if (!karooSystem.connected) {
            karooSystem.connect { connected ->
                Log.d(TAG, "KarooSystemService connected: $connected")
            }
        }
    }

    /**
     * Disconnects from [KarooSystemService] and removes all consumers.
     */
    fun disconnect() {
        karooSystem.disconnect()
    }

    /**
     * Exposes a reactive [Flow] of [StreamState] updates for a given [dataTypeId].
     */
    fun streamDataFlow(dataTypeId: String): Flow<StreamState> = callbackFlow {
        connect()
        val consumerId = karooSystem.addConsumer<OnStreamState>(
            params = OnStreamState.StartStreaming(dataTypeId),
            onError = { error ->
                Log.e(TAG, "Error in streamDataFlow for $dataTypeId: $error")
            },
            onEvent = { (state) ->
                trySend(state)
            },
        )
        awaitClose {
            Log.d(TAG, "Closing streamDataFlow for $dataTypeId, removing consumer: $consumerId")
            karooSystem.removeConsumer(consumerId)
        }
    }

    /**
     * Exposes a reactive [Flow] of [UserProfile] updates (FTP, zones, preferences).
     */
    fun userProfileFlow(): Flow<UserProfile?> = callbackFlow {
        connect()
        val consumerId = karooSystem.addConsumer<UserProfile>(
            onError = { error ->
                Log.e(TAG, "Error in userProfileFlow: $error")
            },
            onEvent = { profile ->
                trySend(profile)
            }
        )
        awaitClose {
            Log.d(TAG, "Closing userProfileFlow, removing consumer: $consumerId")
            karooSystem.removeConsumer(consumerId)
        }
    }

    /**
     * Exposes a reactive [Flow] of [RideState] updates (Idle, Recording, Paused).
     */
    fun rideStateFlow(): Flow<RideState> = callbackFlow {
        connect()
        val consumerId = karooSystem.addConsumer<RideState>(
            onError = { error ->
                Log.e(TAG, "Error in rideStateFlow: $error")
            },
            onEvent = { state ->
                trySend(state)
            }
        )
        awaitClose {
            Log.d(TAG, "Closing rideStateFlow, removing consumer: $consumerId")
            karooSystem.removeConsumer(consumerId)
        }
    }

    /**
     * Exposes a reactive [Flow] emitting the connected Karoo [HardwareType] (e.g. K2 vs KAROO).
     */
    fun systemConfigFlow(): Flow<HardwareType> = callbackFlow {
        connect()
        val currentHw = karooSystem.hardwareType ?: HardwareType.UNKNOWN
        trySend(currentHw)

        karooSystem.connect { connected ->
            if (connected) {
                trySend(karooSystem.hardwareType ?: HardwareType.UNKNOWN)
            }
        }

        awaitClose {
            Log.d(TAG, "Closing systemConfigFlow")
        }
    }

    companion object {
        private const val TAG = "KarooSystemRepository"

        @Volatile
        private var INSTANCE: KarooSystemRepository? = null

        fun getInstance(context: Context): KarooSystemRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KarooSystemRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
