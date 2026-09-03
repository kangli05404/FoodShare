package com.example.foodshare;

import android.app.Application;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

/** Initializes Cloudinary once for the whole app. */
public class FoodShareApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "wcd8kiue");
        config.put("secure", "true");

        // Application.onCreate() runs once per app process, so initialize
        // Cloudinary directly before any upload code can call MediaManager.get().
        MediaManager.init(this, config);
    }
}
