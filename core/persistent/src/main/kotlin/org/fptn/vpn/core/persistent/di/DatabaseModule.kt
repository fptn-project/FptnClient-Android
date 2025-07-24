package org.fptn.vpn.core.persistent.di

import androidx.room.Room
import androidx.room.RoomDatabase
import org.fptn.vpn.core.common.Constants.DATABASE_NAME
import org.fptn.vpn.core.persistent.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule =
    module {
        single<AppDatabase> {
            Room
                .databaseBuilder(androidContext(), AppDatabase::class.java, DATABASE_NAME)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build()
        }
    }
