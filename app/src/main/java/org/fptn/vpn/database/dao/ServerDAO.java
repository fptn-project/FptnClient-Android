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
import androidx.room.Query;
import androidx.room.Transaction;

import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.database.entity.ServerEntity;

import java.util.List;

@Dao
public interface ServerDAO {
    @Insert
    void insertAll(List<ServerEntity> servers);

    @Query("SELECT * FROM server_table")
    List<ServerEntity> getServerList();

    @Query("SELECT COUNT(*) FROM server_table")
    ListenableFuture<Integer> getCount();

    @Query("SELECT * FROM server_table WHERE censured = :censured")
    List<ServerEntity> getServerList(boolean censured);

    @Query("SELECT * FROM server_table WHERE censured = :censured")
    ListenableFuture<List<ServerEntity>> getServerListAsync(boolean censured);

    @Query("UPDATE server_table SET selected = CASE WHEN id = :id THEN 1 ELSE 0 END")
    void setSelected(int id);

    @Query("SELECT * FROM server_table WHERE selected = 1")
    ServerEntity getSelected();

    @Query("UPDATE server_table SET selected = 0 WHERE 1")
    void resetSelected();

    @Query("DELETE FROM server_table")
    void deleteAll();

    @Transaction
    default void deleteAndInsert(List<ServerEntity> servers) {
        deleteAll();
        insertAll(servers);
    }

    @Query("SELECT * FROM server_table WHERE id = :serverId")
    ServerEntity getById(int serverId);
}
