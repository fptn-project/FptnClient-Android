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

package org.fptn.vpn.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.fptn.vpn.database.entity.AppInfoEntity;

import java.util.List;

@Dao
public interface AppInfoDAO {
    @Query("SELECT * FROM app_info")
    List<AppInfoEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppInfoEntity appInfo);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AppInfoEntity> apps);

    @Query("DELETE FROM app_info WHERE package_name = :packageName")
    void delete(String packageName);

    @Query("DELETE FROM app_info")
    void deleteAll();
}
