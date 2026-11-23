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

import org.fptn.vpn.database.entity.TokenEntity;

import java.util.List;

@Dao
public interface TokenDAO {
    @Insert
    long insert(TokenEntity token);

    @Query("SELECT * FROM token_table ORDER BY id ASC")
    List<TokenEntity> getAll();

    @Query("SELECT * FROM token_table WHERE username = :username LIMIT 1")
    TokenEntity findByUsername(String username);

    @Query("SELECT COUNT(*) FROM token_table")
    int getCount();

    @Query("UPDATE token_table SET isVisible = :isVisible WHERE id = :id")
    void setVisible(int id, boolean isVisible);

    @Query("UPDATE token_table SET serviceName = :serviceName, rawToken = :rawToken, username = :username, password = :password, updatedAt = :updatedAt WHERE id = :id")
    void updateTokenData(int id, String serviceName, String rawToken, String username, String password, long updatedAt);

    @Query("DELETE FROM token_table WHERE id = :id")
    void deleteById(int id);
}
