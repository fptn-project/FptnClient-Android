package org.fptn.vpn.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(tableName = "app_info")
public class AppInfoEntity {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "package_name")
    private String packageName;

    @ColumnInfo(name = "allowed")
    private boolean allowed;

    @ColumnInfo(name = "disallowed")
    private boolean disallowed;
}
