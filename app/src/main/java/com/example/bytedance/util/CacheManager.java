package com.example.bytedance.util;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheWriter;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.upstream.DataSource;

/**
 * Centralized ExoPlayer cache utilities: disk cache and prefetch.
 */
public class CacheManager {
    private static volatile SimpleCache cache;
    private static volatile DefaultDataSource.Factory upstreamFactory;
    private static volatile CacheDataSource.Factory cacheDataSourceFactory;
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    public static synchronized void init(Context context) {
        if (cache != null) return;
        File dir = new File(context.getCacheDir(), "media_cache");
        if (!dir.exists()) dir.mkdirs();
        // 1GB LRU cache for speed-first experience
        cache = new SimpleCache(dir, new LeastRecentlyUsedCacheEvictor(1024L * 1024L * 1024L));
        upstreamFactory = new DefaultDataSource.Factory(context, new DefaultHttpDataSource.Factory());
        cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    public static DefaultMediaSourceFactory mediaSourceFactory(Context context) {
        init(context.getApplicationContext());
        return new DefaultMediaSourceFactory(cacheDataSourceFactory);
    }

    public static void prefetchTopN(List<String> urls, int n) {
        if (urls == null || urls.isEmpty()) return;
        int count = Math.min(n, urls.size());
        for (int i = 0; i < count; i++) {
            final String url = urls.get(i);
            executor.execute(() -> prefetchUrl(url));
        }
    }

    public static void prefetchUrl(String url) {
        if (cache == null || cacheDataSourceFactory == null) return;
        try {
            com.google.android.exoplayer2.upstream.DataSpec dataSpec =
                    new com.google.android.exoplayer2.upstream.DataSpec(Uri.parse(url), 0, C.LENGTH_UNSET, null);
            CacheWriter writer = new CacheWriter(cacheDataSourceFactory.createDataSource(), dataSpec, null, null);
            writer.cache();
        } catch (Exception ignored) {
        }
    }
}