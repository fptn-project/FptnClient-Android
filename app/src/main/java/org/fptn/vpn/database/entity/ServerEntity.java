package org.fptn.vpn.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.utils.CountryFlags;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    public String getServerInfo() {
        String flag = CountryFlags.getCountryFlagByCountryCode(countryCode);
        return name + " (" + (flag != null ? flag : host) + ")";
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
