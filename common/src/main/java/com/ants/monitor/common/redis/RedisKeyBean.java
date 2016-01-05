package com.ants.monitor.common.redis;

/**
 * Created by zxg on 15/9/10.
 * redis key 的管理
 */

public interface RedisKeyBean {
    /*===========变量自动为 final static*/
    //redis变量key的系统前缀
    String SYSTEM_PREFIX = "ants_monitor_";

    String NULL_OBJECT = "None";
    /*=====================自定义的各种key=========================================================================*/

    // 该日期的invoke对象
    String invokeListDate = SYSTEM_PREFIX+"invoke_list_date_%s";

    /**list key**/
    String recentInsertKey = SYSTEM_PREFIX+"recent_app_change_insert_list";
    String recentDeleteKey = SYSTEM_PREFIX+"recent_app_change_delete_list";

    //按日期存储详细变化
    String dayChangeKey = SYSTEM_PREFIX+"month_app_change_list_%s";
    //存这月多少日期有变化
    String monthDayKey = SYSTEM_PREFIX+"month_day_app_change_%s";

    /**app 的主要变更 纪录 用作对比比较**/
    String appChangeCacheKey = SYSTEM_PREFIX+"app_change_key";


    /**每日用户的ip：name**/
    String userIpNameKey = SYSTEM_PREFIX+"user_ip_name_map_%s";
    String userIpNameFieldKey = SYSTEM_PREFIX+"field_%s";

    /*=============失效时间=======================================================================*/
    /**
     * 缓存时效 1分钟
     */
    int RREDIS_EXP_MINUTE = 60;

    /**
     * 缓存时效 10分钟
     */
    int RREDIS_EXP_MINUTES = 60 * 10;

    /**
     * 缓存时效 60分钟
     */
    int RREDIS_EXP_HOURS = 60 * 60;

    /**
     * 缓存时效 半天
     */
    int RREDIS_EXP_HALF_DAY = 3600 * 12;
    /**
     * 缓存时效 1天
     */
    int RREDIS_EXP_DAY = 3600 * 24;

    /**
     * 缓存时效 1周
     */
    int RREDIS_EXP_WEEK = 3600 * 24 * 7;

    /**
     * 缓存时效 1月
     */
    int RREDIS_EXP_MONTH = 3600 * 24 * 30 * 7;
}
