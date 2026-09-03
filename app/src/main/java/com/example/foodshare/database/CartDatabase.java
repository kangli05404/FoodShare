package com.example.foodshare.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {CartItem.class}, version = 2, exportSchema = false)
public abstract class CartDatabase extends RoomDatabase {

    public abstract CartDao cartDao();

    private static volatile CartDatabase instance;

    public static synchronized CartDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    CartDatabase.class,
                    "foodshare_cart_db"
            ).fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}