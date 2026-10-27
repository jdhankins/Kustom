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
import kotlinx.coroutines.flow.onStart

/**
 * Repository wrapper for [KarooSystemService] managing connection lifecycle
 * and providing reactive [Flow]s for data streaming, user profile, ride state, and hardware configuration.
 */
class KarooSystemRepository private constructor(context: Context) {

    private val appContext = context.applicationContext

    /** Underlying Karoo OS system service connection instance. */
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
     * Exposes a reactive [Flow] of [StreamState] updates for a specified [dataTypeId].
     *
     * @param dataTypeId The SDK data type identifier string (e.g. "TYPE_POWER_ID", "TYPE_HEART_RATE_ID").
     * @return Reactive [Flow] emitting [StreamState] updates (Streaming, Searching, NotAvailable, Idle).
     */
    fun streamDataFlow(dataTypeId: String): Flow<StreamState> = callbackFlow {
        connect()
        var consumerId: String? = null
        runCatching {
            consumerId = karooSystem.addConsumer<OnStreamState>(
                params = OnStreamState.StartStreaming(dataTypeId),
                onError = { error ->
                    Log.w(TAG, "Notice in streamDataFlow for $dataTypeId: $error")
                    trySend(StreamState.NotAvailable)
                },
                onEvent = { (state) ->
                    trySend(state)
                },
            )
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to register consumer for $dataTypeId", throwable)
            trySend(StreamState.NotAvailable)
        }

        awaitClose {
            val cid = consumerId
            if (cid != null) {
                Log.d(TAG, "Closing streamDataFlow for $dataTypeId, removing consumer: $cid")
                karooSystem.removeConsumer(cid)
            }
        }
    }.onStart { emit(StreamState.Idle) }

    /**
     * Exposes a reactive [Flow] of [UserProfile] updates (FTP, max HR, target zones, unit preferences).
     *
     * @return Reactive [Flow] emitting the latest [UserProfile], or null if uninitialized.
     */
    fun userProfileFlow(): Flow<UserProfile?> = callbackFlow<UserProfile?> {
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
    }.onStart { emit(null) }

    /**
     * Exposes a reactive [Flow] of [RideState] updates (Idle, Recording, Paused).
     *
     * @return Reactive [Flow] emitting current [RideState].
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
    }.onStart { emit(RideState.Idle) }

    /**
     * Exposes a reactive [Flow] emitting the connected Karoo [HardwareType] (e.g. K2 vs KAROO).
     *
     * @return Reactive [Flow] emitting connected [HardwareType].
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
    }.onStart { emit(karooSystem.hardwareType ?: HardwareType.UNKNOWN) }

    companion object {
        private const val TAG = "KarooSystemRepository"

        @Volatile
        private var INSTANCE: KarooSystemRepository? = null

        /**
         * Returns singleton instance of [KarooSystemRepository].
         *
         * @param context Application or activity context.
         * @return Singleton [KarooSystemRepository] instance.
         */
        fun getInstance(context: Context): KarooSystemRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KarooSystemRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
