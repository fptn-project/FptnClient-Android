package org.fptn.vpn.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.fptn.vpn.database.entity.AppInfoEntity;

import java.util.List;

@Dao
public interface AppInfoDAO {
    @Query("SELECT * FROM app_info")
    List<AppInfoEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppInfoEntity appInfo);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AppInfoEntity> apps);

    @Query("DELETE FROM app_info WHERE package_name = :packageName")
    void delete(String packageName);

    @Query("DELETE FROM app_info")
    void deleteAll();
}
