package org.fptn.vpn.core.persistent

import androidx.room.Database
import androidx.room.RoomDatabase
import org.fptn.vpn.core.persistent.AppDatabase.Companion.VERSION_NUMBER
import org.fptn.vpn.core.persistent.model.FptnServerDbModel

@Database(entities = [FptnServerDbModel::class], version = VERSION_NUMBER, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): FptnServerDbModel

    companion object {
        const val VERSION_NUMBER = 10
    }
}
