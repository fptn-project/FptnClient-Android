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
    ListenableFuture<Integer> getCount();

    @Query("SELECT * FROM server_table WHERE censured = :censured")
    List<ServerEntity> getServerList(boolean censured);

    @Query("UPDATE server_table SET selected = CASE WHEN id = :id THEN 1 ELSE 0 END")
    void setSelected(int id);

    @Query("SELECT * FROM server_table WHERE selected = 1")
    ServerEntity getSelected();

    @Query("UPDATE server_table SET selected = 0 WHERE 1")
    void resetSelected();

    @Query("DELETE FROM server_table")
    void deleteAll();

    @Transaction
    default void deleteAndInsert(List<ServerEntity> servers) {
        deleteAll();
        insertAll(servers);
    }

}
