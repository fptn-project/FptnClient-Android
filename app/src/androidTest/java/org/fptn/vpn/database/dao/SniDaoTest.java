package org.fptn.vpn.database.dao;

import static org.assertj.core.api.Assertions.assertThat;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.fptn.vpn.database.AppDatabase;
import org.fptn.vpn.database.entity.SniEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SniDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private SniDao sniDao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        sniDao = db.sniDAO();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void insertAndGetAllSni() {
        SniEntity sni1 = new SniEntity("sni1", false);
        SniEntity sni2 = new SniEntity("sni2", false);
        List<SniEntity> snis = Arrays.asList(sni1, sni2);

        sniDao.insertAll(snis);

        List<SniEntity> allSni = sniDao.getAll();

        assertThat(allSni).hasSize(2);
        assertThat(allSni.get(0).getSni()).isEqualTo("sni1");
        assertThat(allSni.get(1).getSni()).isEqualTo("sni2");
    }

    @Test
    public void insertDuplicateSni() {
        SniEntity sni1 = new SniEntity("sni1", false);
        SniEntity sni2 = new SniEntity("sni1", false); // Duplicate
        List<SniEntity> snis = Arrays.asList(sni1, sni2);

        sniDao.insertAll(snis);

        List<SniEntity> allSni = sniDao.getAll();

        assertThat(allSni).hasSize(1);
        assertThat(allSni.get(0).getSni()).isEqualTo("sni1");
    }

    @Test
    public void deleteAll() {
        SniEntity sni1 = new SniEntity("sni1", false);
        List<SniEntity> snis = List.of(sni1);

        sniDao.insertAll(snis);
        sniDao.deleteAll();

        List<SniEntity> allSni = sniDao.getAll();

        assertThat(allSni).isEmpty();
    }

}
