package com.example.bytedance;

import android.app.Application;
import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import java.io.File;

public class ByteDanceApplication extends Application {
    
    public static SimpleCache simpleCache;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        if (simpleCache == null) {
            File cacheDir = new File(getExternalCacheDir(), "media_cache");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            DatabaseProvider databaseProvider = new StandaloneDatabaseProvider(this);
            // Cache up to 100MB
            LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024);
            simpleCache = new SimpleCache(cacheDir, evictor, databaseProvider);
        }
    }
}
