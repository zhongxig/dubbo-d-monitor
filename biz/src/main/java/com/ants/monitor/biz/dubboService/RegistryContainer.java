package com.ants.monitor.biz.dubboService;

import com.alibaba.dubbo.common.URL;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * RegistryContainer
 * 
 * @author zxg 2015-11-03
 */
public interface RegistryContainer {

    Map<String, Map<String, Set<URL>>> getRegistryCache();

    // 获得当前更新时间
    Date getFinalUpdateTime();

    //获得方法最后消费时间
    String getServiceConsumerTime(String serviceName);

    // 初始化启动函数
    void start() ;

    // 重启函数()
    void restart();

    void stop();


    //初始化changeApp--redis取出，比较
    void initRedisChangeAppCaChe();
}