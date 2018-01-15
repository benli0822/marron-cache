package com.marron.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * com.marron.cache: TODO Input functional description here
 * <p>
 * Date: 15/01/2018 Time: 11:13 AM
 *
 * @author Created by benlijin
 * @see [CLASS/METHOD](Optional)
 * @since [PRODUCT/MODULE_VERSION](Optional)
 */
@Slf4j
@Getter
@Setter
public abstract class AbstractGuavaLoadingCache<K, V> implements IBaseCacheService<K, V> {

    //用于初始化cache的参数及其缺省值
    //最大缓存条数，子类在构造方法中调用setMaximumSize(int size)来更改
    private long maximumSize = 1000L;
    //数据存在时长，子类在构造方法中调用setExpireAfterWriteDuration(int duration)来更改
    private long expireAfterWriteDuration = 60;
    private long refreshAfterWriteDuration = 120;
    //时间单位（分钟）
    private TimeUnit timeUnit = TimeUnit.MINUTES;
    //Cache初始化或被重置的时间
    private Date resetTime;
    //历史最高记录数
    private long highestSize = 0;
    //创造历史记录的时间
    private Date highestTime;
    // Cache实体
    private volatile LoadingCache cache;
    // 更新策略
    private volatile CacheLoader strategy;
    // 异步刷新线程池
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 通过调用getCache().get(key)来获取数据
     *
     * @return cache cache
     */
    public LoadingCache<K, V> getCache() {
// 单子双校验
        if (cache == null) {
            synchronized (this) {
                if (cache == null) {
//缓存数据的最大条目，也可以使用.maximumWeight(weight)代替
                    cache = CacheBuilder.newBuilder().maximumSize(maximumSize)
//数据被创建多久后被移除
                        .expireAfterWrite(expireAfterWriteDuration, timeUnit)
//数据被创建多久后被刷新
                        .refreshAfterWrite(refreshAfterWriteDuration, timeUnit)
                        .recordStats() //启用统计
// 配置刷新策略F
                        .build(initStrategy());
                    this.resetTime = new Date();
                    this.highestTime = new Date();
                    log.debug("本地缓存{}初始化成功", new Object[]{this.getClass().getSimpleName()});
                }
            }
        }
        return cache;
    }

    private CacheLoader<K, V> initStrategy() {
// toto check thread pool
        if (strategy == null) {
            synchronized (this) {
                if (null == strategy) {
                    strategy = new CacheLoader<K, V>() {
                        @Override
                        public V load(K key) {
                            return fetchData(key);
                        }

                        @Override
                        public ListenableFuture<V> reload(final K key, V value) {
// asynchronous!
                            ListenableFutureTask<V> task = ListenableFutureTask.create((Callable) () -> fetchData(key));
                            threadPoolExecutor.execute(task);
                            return task;
                        }
                    };
                }
            }
        }
        return strategy;
    }

    /**
     * 根据key从数据库或其他数据源中获取一个value，并被自动保存到缓存中。
     *
     * @param key the key
     * @return value, 连同key一起被加载到缓存中的。
     */
    protected abstract V fetchData(K key);

    /**
     * 从缓存中获取数据（第一次自动调用fetchData从外部获取数据），并处理异常
     *
     * @param key 键
     * @return Value 值
     * @throws ExecutionException 执行异常
     */
    protected V getValue(K key) throws ExecutionException {
        V result = getCache().get(key);
        if (getCache().size() > highestSize) {
            highestSize = getCache().size();
            highestTime = new Date();
        }
        return result;
    }

    @Override
    public boolean fetch(final K key) {
        return null != this.cache.getIfPresent(key);
    }

    /**
     * Del.
     *
     * @param key the key
     */
    @Override
    public int del(K key) {
        // If exists
        if (fetch(key)) {
            getCache().invalidate(key);
            return 1;
        } else {
            // Otherwise not exists
            return -1;
        }
    }

    /**
     * Gets reset time.
     *
     * @return the reset time
     */
    public final Date getResetTime() {
        if (resetTime == null) {
            return null;
        }
        return (Date) (resetTime).clone();
    }

    /**
     * Sets reset time.
     *
     * @param paramResetTime the param reset time
     */
    public final void setResetTime(final Date paramResetTime) {
        if (paramResetTime == null) {
            this.resetTime = null;
        } else {
            this.resetTime = (Date) (paramResetTime).clone();
        }
    }

    /**
     * Gets highest time.
     *
     * @return the highest time
     */
    public final Date getHighestTime() {
        if (highestTime == null) {
            return null;
        }
        return (Date) (highestTime).clone();
    }

    /**
     * Sets highest time.
     *
     * @param paramHighestTime the param highest time
     */
    public final void setHighestTime(final Date paramHighestTime) {
        if (paramHighestTime == null) {
            this.highestTime = null;
        } else {
            this.highestTime = (Date) (paramHighestTime).clone();
        }
    }
}
