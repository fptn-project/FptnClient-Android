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

import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.database.entity.SniEntity;

import java.util.List;

@Dao
public interface SniDao {

    @Query("SELECT * FROM sni_table")
    List<SniEntity> getAll();

    @Query("SELECT * FROM sni_table where checked = 0")
    List<SniEntity> getAllUnchecked();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertAll(List<SniEntity> sniList);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SniEntity sni);

    @Query("DELETE FROM sni_table")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM sni_table")
    int count();

    @Query("SELECT COUNT(*) FROM sni_table where checked = 0")
    int countUnchecked();

    @Query("SELECT * FROM sni_table where checked = 0 ORDER BY RANDOM() limit :limit")
    List<SniEntity> getUnchecked(int limit);

    @Query("UPDATE sni_table SET checked = 0")
    void resetAll();

}
