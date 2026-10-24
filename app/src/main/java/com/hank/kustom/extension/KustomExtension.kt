package com.hank.kustom.extension

import com.hank.kustom.data.model.KustomFieldCatalog
import com.hank.kustom.data.repository.KarooSystemRepository
import com.hank.kustom.data.repository.KustomSettingsRepository
import com.hank.kustom.extension.types.KustomDataType
import com.hank.kustom.util.KustomLogger
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension

/**
 * Primary Karoo Extension entry point for Kustom.
 */
class KustomExtension : KarooExtension("kustom", "1") {

    override fun onCreate() {
        KustomLogger.installCrashHandler(this)
        super.onCreate()
        KustomLogger.d("KustomExtension created")
    }

    override val types: List<DataTypeImpl> by lazy {
        val karooRepo = KarooSystemRepository.getInstance(applicationContext)
        val settingsRepo = KustomSettingsRepository(applicationContext)
        KustomFieldCatalog.allFields.map { fieldDef ->
            KustomDataType(fieldDef, karooRepo, settingsRepo)
        }
    }
}
