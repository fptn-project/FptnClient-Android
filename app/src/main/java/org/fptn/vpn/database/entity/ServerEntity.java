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

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.utils.CountryFlags;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Entity(tableName = "server_table")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServerEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private boolean selected;
    private String name;
    private String username;
    private String password;
    private String host;
    private int port;
    private String countryCode;
    private String md5ServerFingerprint;
    private boolean censured;

    @Ignore // not save in DB (Room ORM annotation)
    @Getter
    @Setter
    private long pingMs = -1;

    // Written out explicitly alongside the Lombok @Getter/@Setter above: Kotlin's Java-interop
    // stub generation runs before the Lombok annotation processor, so Kotlin call sites can't
    // see the Lombok-generated accessors here.
    public long getPingMs() {
        return pingMs;
    }

    public void setPingMs(long pingMs) {
        this.pingMs = pingMs;
    }

    public boolean IsAuto() {
        return Objects.equals(name, "Auto");
    }

    // Written out explicitly (instead of relying on Lombok's @Data) for the subset of
    // accessors Compose screens need: Kotlin's Java-interop stub generation runs before the
    // Lombok annotation processor, so Kotlin call sites can't see Lombok-generated members.
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public boolean isCensured() {
        return censured;
    }

    public boolean isSelected() {
        return selected;
    }

    public String getServerInfo() {
        String flag = CountryFlags.getCountryFlagByCountryCode(countryCode);
        return flag != null ? name + " " + flag : name;
    }

    public static final ServerEntity AUTO = ServerEntity.builder()
            .id(Constants.SELECTED_SERVER_ID_AUTO)
            .selected(false)
            .name("Auto")
            .username("Auto")
            .password("Auto")
            .host("")
            .port(0)
            .countryCode(null)
            .md5ServerFingerprint(null)
            .censured(false)
            .build();
}
