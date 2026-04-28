package org.fptn.vpn.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.fptn.vpn.database.dao.AppInfoDAO;
import org.fptn.vpn.database.dao.ServerDAO;
import org.fptn.vpn.database.dao.SniDao;
import org.fptn.vpn.database.entity.AppInfoEntity;
import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.database.entity.SniEntity;

@Database(entities = {ServerEntity.class, AppInfoEntity.class, SniEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public static final String FPTN_DATABASE = "FptnDatabase";

    public abstract ServerDAO serverDAO();

    public abstract AppInfoDAO appInfoDAO();

    public abstract SniDao sniDAO();

    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, FPTN_DATABASE)
                    .fallbackToDestructiveMigration(true)
                    .build();
        }
        return instance;
    }

}
