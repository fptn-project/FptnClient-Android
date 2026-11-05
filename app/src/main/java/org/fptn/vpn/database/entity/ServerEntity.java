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
    private int tokenId;
    private int sortIndex;

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

    public int getTokenId() {
        return tokenId;
    }

    public void setTokenId(int tokenId) {
        this.tokenId = tokenId;
    }

    public int getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(int sortIndex) {
        this.sortIndex = sortIndex;
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
            .tokenId(0)
            .sortIndex(0)
            .build();
}