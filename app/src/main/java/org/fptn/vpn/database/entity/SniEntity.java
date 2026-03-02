package org.fptn.vpn.database.entity;

import androidx.annotation.NonNull;
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
@Entity(tableName = "sni_table")
public class SniEntity {

    @PrimaryKey
    @NonNull
    private String sni;

    private boolean checked = false;
}
