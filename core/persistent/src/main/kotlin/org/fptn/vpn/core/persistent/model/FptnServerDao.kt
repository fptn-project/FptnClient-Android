package org.fptn.vpn.core.persistent.model

import androidx.room.Dao
import androidx.room.Upsert

@Dao
interface FptnServerDao {
    @Upsert
    suspend fun insert(user: FptnServerDbModel)
}
