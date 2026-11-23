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

package org.fptn.vpn.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.NoArgsConstructor;

/**
 * Stores user FPTN tokens.
 *
 * One token = one "account" (username) with a set of servers.
 * A token with isVisible=true has its servers displayed in the dropdown list on the home screen.
 */
@Entity(tableName = "token_table")
@NoArgsConstructor
public class TokenEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "serviceName")
    private String serviceName;

    @ColumnInfo(name = "rawToken", defaultValue = "")
    private String rawToken;

    @ColumnInfo(name = "username", defaultValue = "")
    private String username;

    @ColumnInfo(name = "password", defaultValue = "")
    private String password;

    /** true if the servers associated with this token should be displayed in the general list on the home screen */
    @ColumnInfo(name = "isVisible", defaultValue = "1")
    private boolean isVisible;

    @ColumnInfo(name = "createdAt", defaultValue = "0")
    private long createdAt;

    @ColumnInfo(name = "updatedAt", defaultValue = "0")
    private long updatedAt;

    public TokenEntity(int id, String serviceName, String rawToken, String username,
                       String password, boolean isVisible, long createdAt, long updatedAt) {
        this.id = id;
        this.serviceName = serviceName;
        this.rawToken = rawToken;
        this.username = username;
        this.password = password;
        this.isVisible = isVisible;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getRawToken() { return rawToken; }
    public void setRawToken(String rawToken) { this.rawToken = rawToken; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { isVisible = visible; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
