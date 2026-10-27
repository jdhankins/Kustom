package com.hank.kustom.extension

import com.hank.kustom.data.model.FieldCategory
import com.hank.kustom.data.model.KustomFieldCatalog
import com.hank.kustom.data.model.KustomFieldDefinition
import com.hank.kustom.data.repository.KarooSystemRepository
import com.hank.kustom.data.repository.KustomSettingsRepository
import com.hank.kustom.extension.types.KustomDataType
import com.hank.kustom.util.KustomLogger
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

/**
 * Primary Karoo Extension service entry point bound to extension ID "kustom".
 */
class KustomExtension : KarooExtension("kustom", "1") {

    override fun onCreate() {
        KustomLogger.installCrashHandler(this)
        super.onCreate()
        KustomLogger.d("KustomExtension created")
    }

    /**
     * Exposes all supported [DataTypeImpl] instances corresponding to static fields and user-created dynamic 3-in-1 combo blocks.
     *
     * @return List of [DataTypeImpl] implementations.
     */
    override val types: List<DataTypeImpl>
        get() {
            val karooRepo = KarooSystemRepository.getInstance(applicationContext)
            val settingsRepo = KustomSettingsRepository(applicationContext)

            val staticTypes = KustomFieldCatalog.allFields.map { fieldDef ->
                KustomDataType(fieldDef, karooRepo, settingsRepo)
            }

            val savedSettings = runCatching {
                runBlocking { settingsRepo.settingsFlow.firstOrNull() }
            }.getOrNull()

            val dynamicComboTypes = savedSettings?.customComboInstances?.map { instance ->
                val dynamicDef = KustomFieldDefinition(
                    typeId = instance.id,
                    childTitle = instance.name,
                    category = FieldCategory.COMBO,
                    description = "Dynamic 3-in-1 combo block (${instance.name})",
                    supportsSmoothing = true,
                    supportsZones = true,
                    mockValues = listOf(214.0, 235.0, 250.0, 220.0, 205.0),
                )
                KustomDataType(dynamicDef, karooRepo, settingsRepo)
            } ?: emptyList()

            return staticTypes + dynamicComboTypes
        }
}
