package org.fptn.vpn.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.database.entity.SniEntity;

import java.util.List;

@Dao
public interface SniDao {

    @Query("SELECT * FROM sni_table")
    List<SniEntity> getAll();

    @Query("SELECT * FROM sni_table where checked = 0")
    List<SniEntity> getAllUnchecked();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    ListenableFuture<Void> insertAll(List<SniEntity> sniList);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SniEntity sni);

    @Query("DELETE FROM sni_table")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM sni_table")
    int count();

    @Query("SELECT COUNT(*) FROM sni_table where checked = 0")
    int countUnchecked();

    @Query("SELECT * FROM sni_table where checked = 0 limit :limit")
    List<SniEntity> getUnchecked(int limit);

    @Query("UPDATE sni_table SET checked = 0")
    void resetAll();

}
