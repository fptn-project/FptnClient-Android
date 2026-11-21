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

import java.util.ArrayList;
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

    /** Toggles the favorite flag of a single server in one atomic statement. */
    @Query("UPDATE server_table SET favorite = CASE WHEN id = :id THEN 1 - favorite ELSE favorite END")
    void toggleFavorite(int id);

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

    /**
     * Servers for the dropdown: same ordering as getVisibleServerList(), but with
     * user-starred (favorite) servers floated to the top. Display-only — auto-select
     * (FptnService), ping cycles and SNI checks keep reading getVisibleServerList().
     */
    @Query("SELECT server_table.* FROM server_table INNER JOIN token_table ON server_table.tokenId = token_table.id WHERE token_table.isVisible = 1 ORDER BY server_table.favorite DESC, token_table.id ASC, server_table.censured ASC, server_table.sortIndex ASC")
    List<ServerEntity> getServersForDisplay();

    @Query("UPDATE server_table SET countryCode = :code WHERE id = :id")
    void updateCountryCode(int id, String code);

    /**
     * All servers of one token (for the favorite snapshot on token update).
     */
    @Query("SELECT * FROM server_table WHERE tokenId = :tokenId")
    List<ServerEntity> getServersByTokenId(int tokenId);

    /**
     * Restores the favorite flag of one server after a token refresh,
     * matched by (host, port) inside its own token.
     */
    @Query("UPDATE server_table SET favorite = 1 WHERE tokenId = :tokenId AND host = :host AND port = :port")
    void setFavoriteByHostPort(int tokenId, String host, int port);

    /**
     * Replaces all servers of one token with a fresh list (token update flow),
     * keeping the favorite flag of servers that kept the same (host, port).
     * A server that changed its address loses its star; favorites of other
     * tokens are not touched. Runs atomically.
     */
    @Transaction
    default void replaceServersPreservingFavorites(List<ServerEntity> servers, int tokenId) {
        List<ServerEntity> favorites = new ArrayList<>();
        for (ServerEntity server : getServersByTokenId(tokenId)) {
            if (server.isFavorite()) {
                favorites.add(server);
            }
        }
        deleteByTokenId(tokenId);
        insertAll(servers);
        for (ServerEntity server : favorites) {
            setFavoriteByHostPort(tokenId, server.getHost(), server.getPort());
        }
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