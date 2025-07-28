package org.fptn.vpn.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.fptn.vpn.core.common.Constants.SELECTED_SERVER_ID_AUTO
import org.fptn.vpn.utils.CountryFlags

@Entity(tableName = "server_table")
data class FptnServerDto(
    @JvmField
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @JvmField
    val isSelected: Boolean,
    @JvmField val name: String,
    @JvmField val username: String,
    @JvmField val password: String,
    @JvmField val host: String,
    @JvmField val port: Int,
    @JvmField val countryCode: String?,
    @JvmField val md5ServerFingerprint: String?,
    @JvmField val censured: Boolean,
) {
    val serverInfo: String
        get() = "$name (${CountryFlags.getCountryFlagByCountryCode(countryCode) ?: host})"

    companion object {
        @JvmField
        val AUTO: FptnServerDto =
            FptnServerDto(SELECTED_SERVER_ID_AUTO, false, "Auto", "Auto", "Auto", "", 0, null, null, false)
    }
}
