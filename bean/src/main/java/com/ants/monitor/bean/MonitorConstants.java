package com.ants.monitor.bean;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zxg on 15/11/16.
 * 常量
 */
public class MonitorConstants {
    public static final String OWNER = "owner";
    public static final String ORGANICATION = "organization";

    public static final String SESSION_USER_NAME = "SESSION_USER_NAME";




    /**=============ip 所在的服务器===================================**/

    //存所有的ip：name
    public static final Map<String,String> ecsMap = new HashMap<>();
    // 双向map
    public static final BiMap<String,String> ecsBiMap = HashBiMap.create();
    //测试环境的ip
    public static final Map<String,String> ecsTestMap = new HashMap<>();

    public static void initEcsMap(){
        //todo 自定义初始化上述map。将ip放置在内存中。

    }


}
