package com.ants.monitor.common.cache;

/**
 * Created by zxg on 16/11/16.
 * 12:13
 * no bug,以后改代码的哥们，祝你好运~！！
 * 公用的缓存曾
 */
public interface Cache {

    void put(Object key, Object value);

    Object get(Object key);

}