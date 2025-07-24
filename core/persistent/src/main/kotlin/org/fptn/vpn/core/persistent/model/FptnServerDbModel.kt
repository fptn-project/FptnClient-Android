package org.fptn.vpn.core.persistent.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.fptn.vpn.core.common.Constants.DEFAULT_DATA_ID
import org.fptn.vpn.core.model.FptnServerDomain

@Entity(tableName = "server_table")
data class FptnServerDbModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "isSelected") val isSelected: Boolean,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "password") val password: String,
    @ColumnInfo(name = "host") val host: String,
    @ColumnInfo(name = "port") val port: Int,
    @ColumnInfo(name = "countryCode") val countryCode: String,
    @ColumnInfo(name = "md5ServerFingerprint") val md5ServerFingerprint: String,
    @ColumnInfo(name = "censured") val censured: Boolean,
)

fun FptnServerDomain.toDbModel(
    username: String,
    password: String,
    censured: Boolean,
) = FptnServerDbModel(
    id = DEFAULT_DATA_ID,
    isSelected = false,
    name = name,
    username = username,
    password = password,
    host = host,
    port = port,
    countryCode = "US",
    md5ServerFingerprint = md5Fingerprint,
    censured = censured,
)
