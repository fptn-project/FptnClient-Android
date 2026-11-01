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
    int getCountSync();

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

    /**
     * Deletes all servers belonging to a specific token.
     * Used when updating or deleting a token in Token Management.
     */
    @Query("DELETE FROM server_table WHERE tokenId = :tokenId")
    void deleteByTokenId(int tokenId);

    /**
     * Returns all servers belonging to tokens where isVisible = true.
     * Sorts by token ID (insertion order), then by sortIndex (order in the JSON token).
     */
    @Query("SELECT server_table.* FROM server_table INNER JOIN token_table ON server_table.tokenId = token_table.id WHERE token_table.isVisible = 1 ORDER BY token_table.id ASC, server_table.censured ASC, server_table.sortIndex ASC")
    List<ServerEntity> getVisibleServerList();

    @Query("UPDATE server_table SET countryCode = :code WHERE id = :id")
    void updateCountryCode(int id, String code);

    @Transaction
    default void deleteAndInsert(List<ServerEntity> servers) {
        deleteAll();
        insertAll(servers);
    }

    @Query("SELECT * FROM server_table WHERE id = :serverId")
    ServerEntity getById(int serverId);

    /**
     * Returns all VISIBLE servers (isVisible = 1), filtered by censured.
     * Sorts by token ID (insertion order), then by sortIndex (order in the JSON token).
     */
    @Query("SELECT server_table.* FROM server_table INNER JOIN token_table ON server_table.tokenId = token_table.id WHERE token_table.isVisible = 1 AND server_table.censured = :censured ORDER BY token_table.id ASC, server_table.sortIndex ASC")
    List<ServerEntity> getVisibleServerList(boolean censured);
}