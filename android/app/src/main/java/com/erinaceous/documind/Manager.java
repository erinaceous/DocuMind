package com.erinaceous.documind;

import android.content.Context;

import androidx.room.Room;

import com.erinaceous.documind.dao.AppDatabase;
import com.erinaceous.documind.network.QwenV1Api;

public class Manager {
    private Context context;
    private static volatile Manager instance;
    private QwenV1Api qwenV1Api;
    private AppDatabase appDatabase;


    private Manager(Context context) {
        this.context = context;
        qwenV1Api = QwenV1Api.createApi();
        appDatabase = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "documind-database"
        ).build();
    }

    public static Manager getInstance(Context context) {
        if (instance == null) {
            synchronized (Manager.class) {
                // Double-check inside synchronized block
                if (instance == null) {
                    instance = new Manager(context);
                }
            }
        }
        return instance;
    }

    public QwenV1Api getQwenV1Api() {
        return qwenV1Api;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }
}
