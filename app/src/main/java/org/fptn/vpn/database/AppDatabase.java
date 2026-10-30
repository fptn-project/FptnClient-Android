/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */

package org.fptn.vpn.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.fptn.vpn.database.dao.AppInfoDAO;
import org.fptn.vpn.database.dao.ServerDAO;
import org.fptn.vpn.database.dao.SniDao;
import org.fptn.vpn.database.entity.AppInfoEntity;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.database.entity.SniEntity;
import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import org.fptn.vpn.database.dao.TokenDAO;
import org.fptn.vpn.database.entity.TokenEntity;

@Database(
        entities = {ServerEntity.class, AppInfoEntity.class, SniEntity.class, TokenEntity.class},
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String FPTN_DATABASE = "FptnDatabase";

    public abstract ServerDAO serverDAO();

    public abstract AppInfoDAO appInfoDAO();

    public abstract SniDao sniDAO();

    public abstract TokenDAO tokenDAO();

    private static AppDatabase instance;

    /**
     * Migration 2 → 3: add the token_table and the tokenId field to server_table.
     *
     * Existing servers are preserved — a default "Default" token is created for them
     * and linked via UPDATE. The user does not lose any data when updating the app.
     *
     * ⚠️ IMPORTANT: use an explicit migration instead of fallbackToDestructiveMigration(true).
     * Previously, updating the database would have deleted all the user's servers.
     */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 1. Create the new token_table (isVisible = 1 by default)
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS token_table (" +
                            "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "serviceName TEXT, " +
                            "rawToken TEXT DEFAULT '', " +
                            "username TEXT DEFAULT '', " +
                            "password TEXT DEFAULT '', " +
                            "isVisible INTEGER NOT NULL DEFAULT 1, " +
                            "createdAt INTEGER NOT NULL DEFAULT 0, " +
                            "updatedAt INTEGER NOT NULL DEFAULT 0" +
                            ")"
            );

            // 2. Add the tokenId column to server_table
            database.execSQL("ALTER TABLE server_table ADD COLUMN tokenId INTEGER NOT NULL DEFAULT 0");

            // 3. Create the default token (isVisible = 1 so that servers are displayed)
            long now = System.currentTimeMillis();
            database.execSQL(
                    "INSERT INTO token_table (serviceName, rawToken, username, password, isVisible, createdAt, updatedAt) " +
                            "VALUES ('Default', '', 'unknown', '', 1, " + now + ", " + now + ")"
            );

            // 4. Link the servers to the default token
            database.execSQL(
                    "UPDATE server_table SET tokenId = (SELECT id FROM token_table LIMIT 1)"
            );
        }
    };

    /**
     * Migration 3 → 4: adds the sortIndex column to server_table.
     * Used to preserve the original server order from the JSON token.
     */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE server_table ADD COLUMN sortIndex INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, FPTN_DATABASE)
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build();
        }
        return instance;
    }
}