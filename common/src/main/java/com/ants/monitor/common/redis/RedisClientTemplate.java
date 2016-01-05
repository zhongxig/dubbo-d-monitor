package com.ants.monitor.common.redis;


import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by zxg on 15/7/20.
 */
public interface RedisClientTemplate {
    /*=================普通String懒操作==============*/
    <T> T lazyGet(String key, Class<?> cls);

    <T> List<T> lazyGetList(String key, Class<?> cls);

    String lazySet(String key, Object value, Integer expire);

    /*
    *   缓存数据库不存在的数据，避免短时间重复查询数据库
    * */
    void setNone(String key);

    boolean isNone(String redisString);
    /*=============普通String的操作===========================*/

    /**
     * 设置单个值
     * <p/>
     * 将字符串值 value 关联到 key 。
     * 如果 key 已经持有其他值， setString 就覆写旧值，无视类型。
     * 对于某个原本带有生存时间（TTL）的键来说， 当 setString 成功在这个键上执行时， 这个键原有的 TTL 将被清除。
     * 时间复杂度：O(1)
     *
     * @param key   key
     * @param value string value
     * @return 在设置操作成功完成时，才返回 OK 。
     */
    public String set(String key, String value);

    /**
     * 将值 value 关联到 key ，并将 key 的生存时间设为 expire (以秒为单位)。
     * 如果 key 已经存在， 将覆写旧值。
     * 类似于以下两个命令:
     * SET key value
     * EXPIRE key expire # 设置生存时间
     * 不同之处是这个方法是一个原子性(atomic)操作，关联值和设置生存时间两个动作会在同一时间内完成，在 Redis 用作缓存时，非常实用。
     * 时间复杂度：O(1)
     *
     * @param key    key
     * @param value  string value
     * @param expire 生命周期,单位（秒）
     * @return 设置成功时返回 OK 。当 expire 参数不合法时，返回一个错误。
     */
    public String setStringWithTime(final String key, final String value, Integer expire);


    /**
     * 获取单个值
     *
     * @param key
     * @return
     */
    public String get(String key);


    //判断key值存在不存在
    public Boolean exists(String key);

    /*============hashMap========================*/

    /**
     * 将哈希表 key 中的域 field 的值设为 value 。
     * 如果 key 不存在，一个新的哈希表被创建并进行 hashSet 操作。
     * 如果域 field 已经存在于哈希表中，旧值将被覆盖。
     * 时间复杂度: O(1)
     *
     * @param key   key 存在缓存中的key
     * @param field 域 －－－即hashmap的key 值
     * @param value string value
     * @return 如果 field 是哈希表中的一个新建域，并且值设置成功，返回 1 。如果哈希表中域 field 已经存在且旧值已被新值覆盖，返回 0 。
     */
    public Long setMapKey(String key, String field, String value);


    public String getMapKey(String key, String field);

 /*=================================List=============================*/

    //头添加数据
    public Long LpushList(String key, String value);

    //尾部添加数据
    public Long RpushList(String key, String value);

    //尾部删除
    public String RpopList(String key);

    //list长度
    public Integer listSize(String key);

    //获取list长度
    public List<String> getList(String key,Integer start,Integer end);




    /**==============set==========================**/

    Long addSet(String key,String value);

    Set<String> getSet(String key);
}
