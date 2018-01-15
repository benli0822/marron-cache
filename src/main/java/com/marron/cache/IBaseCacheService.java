package com.marron.cache;

/**
 * com.marron.cache: TODO Input functional description here
 * <p>
 * Date: 15/01/2018 Time: 10:51 AM
 *
 * @param <K> the type parameter
 * @param <V> the type parameter
 * @author Created by benlijin
 * @see [CLASS/METHOD](Optional)
 * @since [PRODUCT /MODULE_VERSION](Optional)
 */
public interface IBaseCacheService<K, V> {

    /**
     * Get v.
     *
     * @param key the key
     * @return the v
     */
    V get(K key);

    /**
     * Fetch boolean.
     *
     * @param key the key
     * @return the boolean
     */
    boolean fetch(K key);

    /**
     * Put int.
     *
     * @param key   the key
     * @param value the value
     * @return the int
     */
    int put(K key, V value);

    /**
     * Del int.
     *
     * @param key the key
     * @return the int
     */
    int del(K key);
}
