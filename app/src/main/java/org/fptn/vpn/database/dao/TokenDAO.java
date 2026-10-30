package org.fptn.vpn.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.database.entity.TokenEntity;

import java.util.List;

@Dao
public interface TokenDAO {
    @Insert
    long insert(TokenEntity token);

    @Query("SELECT * FROM token_table ORDER BY id ASC")
    List<TokenEntity> getAll();

    @Query("SELECT * FROM token_table ORDER BY id ASC")
    ListenableFuture<List<TokenEntity>> getAllAsync();

    @Query("SELECT * FROM token_table WHERE id = :id")
    TokenEntity getById(int id);

    @Query("SELECT * FROM token_table WHERE username = :username LIMIT 1")
    TokenEntity findByUsername(String username);

    @Query("SELECT COUNT(*) FROM token_table")
    int getCount();

    @Query("SELECT COUNT(*) FROM token_table")
    ListenableFuture<Integer> getCountAsync();

    @Query("UPDATE token_table SET isVisible = :isVisible WHERE id = :id")
    void setVisible(int id, boolean isVisible);

    @Query("UPDATE token_table SET serviceName = :serviceName, rawToken = :rawToken, username = :username, password = :password, updatedAt = :updatedAt WHERE id = :id")
    void updateTokenData(int id, String serviceName, String rawToken, String username, String password, long updatedAt);

    @Query("DELETE FROM token_table WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM token_table")
    void deleteAll();
}