package com.ants.monitor.controller.task;

import com.alibaba.dubbo.common.Constants;
import com.ants.monitor.bean.MonitorConstants;
import com.ants.monitor.bean.bizBean.HostBO;
import com.ants.monitor.bean.entity.InvokeDO;
import com.ants.monitor.biz.support.service.ApplicationService;
import com.ants.monitor.biz.support.service.HostService;
import com.ants.monitor.common.tools.TimeUtil;
import com.ants.monitor.dao.redisManager.InvokeRedisManager;
import com.ants.monitor.dao.redisManager.InvokeReportManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Created by zxg on 16/1/28.
 * 13:49
 * 报表数据，每日凌晨00：01分开始统计
 */
@RestController
@RequestMapping("/monitor/invokeReportTask")
@Slf4j
public class InvokeReportTaskController {
    @Autowired
    private InvokeReportManager invokeReportManager;
    @Autowired
    private InvokeRedisManager invokeRedisManager;

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private HostService hostService;

    //每天凌晨 00:01 统计每个应用昨天的相互调用情况
    @Scheduled(cron = "0 1 0 * * ?")
//    @Scheduled(cron = "0 58 20 * * ?")
    public void appSumOnDay() {
        String yesterDay = TimeUtil.getBeforDateByNumber(new Date(), -1);
        Set<String> allApplication = applicationService.getAllApplications();


        List<InvokeDO> invokeDOList = invokeRedisManager.getInvokeByDate(yesterDay);
        for (String applicationName : allApplication) {
            Map<String, Map<String, Integer>> appMap = new HashMap<>();
            Map<String, Integer> providerMap = new HashMap<>();
            Map<String, Integer> consumerMap = new HashMap<>();
            appMap.put(Constants.PROVIDER, providerMap);
            appMap.put(Constants.CONSUMER, consumerMap);

            Boolean has_pro = false;
            Boolean has_consu = false;
            for (InvokeDO invokeDO : invokeDOList) {
                String invokeType = invokeDO.getAppType();
                if (invokeType.equals(Constants.PROVIDER)) {
                    // 做为提供者，提供服务--找不到消费者
                    continue;
                }
                String providerHost = invokeDO.getProviderHost();
                String providerPort = invokeDO.getProviderPort();
                Set<String> nameSet = hostService.getAppNameByHost(new HostBO(providerHost, providerPort));
                if (nameSet.isEmpty() || nameSet.size() > 1) {
                    // 有且只有一个
                    continue;
                }
                String appName = invokeDO.getApplication();
                String providerName = nameSet.iterator().next();
                if (applicationName.equals(appName)) {
                    Integer success = invokeDO.getSuccess();
                    // app 作为消费者，被提供
                    Integer providerSum = providerMap.get(providerName) == null ? Integer.valueOf(0) : providerMap.get(providerName);
                    providerSum += success;
                    providerMap.put(providerName, providerSum);
                    has_pro = true;
                }
                if (applicationName.equals(providerName)) {
                    // app 作为提供者，被消费
                    Integer success = invokeDO.getSuccess();
                    Integer consumerSum = consumerMap.get(appName) == null ? Integer.valueOf(0) : consumerMap.get(appName);
                    consumerSum += success;
                    consumerMap.put(appName, consumerSum);
                    has_consu = true;
                }
            }
            if(!has_pro){
                appMap.remove(Constants.PROVIDER);
            }
            if(!has_consu){
                appMap.remove(Constants.CONSUMER);
            }
            if(has_consu || has_pro) {
                invokeReportManager.saveAppRelationByAppOnDay(applicationName, yesterDay, appMap);
            }
        }
    }



    //每天凌晨 00:01 统计每个应用昨天的每小时消费者消费情况
    @Scheduled(cron = "0 1 0 * * ?")
//    @Scheduled(cron = "0 58 20 * * ?")
    public void appConsumerOnHour() {
        String yesterDay = TimeUtil.getBeforDateByNumber(new Date(), -1);
        Set<String> allApplication = applicationService.getAllApplications();


        List<InvokeDO> invokeDOList = invokeRedisManager.getInvokeByDate(yesterDay);
        for (String applicationName : allApplication) {
            Map<String, Map<String,?>> saveMap = new HashMap<>();
            Boolean is_ok = false;

            for(InvokeDO invokeDO : invokeDOList){
                String invokeType = invokeDO.getAppType();
                if(invokeType.equals(Constants.PROVIDER)){
                    // 做为提供者，提供服务--找不到消费者
                    continue;
                }
                String providerHost = invokeDO.getProviderHost();
                String providerPort = invokeDO.getProviderPort();
                Set<String> nameSet = hostService.getAppNameByHost(new HostBO(providerHost, providerPort));
                if(nameSet.isEmpty() || nameSet.size() > 1){
                    // 有且只有一个
                    continue;
                }
                String appName = invokeDO.getApplication();
                String providerName = nameSet.iterator().next();
                if(applicationName.equals(providerName)) {
                    is_ok = true;
                    // app 作为提供者，被消费
                    Integer success = invokeDO.getSuccess();
                    Integer fail = invokeDO.getFailure();
                    // 时间转化为小时
                    Long invokeTime = invokeDO.getInvokeTime();
                    Long afterHour = (invokeTime / (60 * 1000 * 60)) * (60 * 60 * 1000);
                    String hourTime = TimeUtil.getMinuteString(new Date(afterHour));
                    // 存储
                    Map<String, Object> hourSumMap = (Map<String, Object>) saveMap.get(appName);
                    if(null == hourSumMap){
                        hourSumMap = new HashMap<>();
                        saveMap.put(appName, hourSumMap);
                    }
                    Map<String,Integer> sumMap = (Map<String, Integer>) hourSumMap.get(hourTime);
                    if(sumMap == null){
                        sumMap = new HashMap<>();
                        sumMap.put(MonitorConstants.SUCCESS,success);
                        sumMap.put(MonitorConstants.FAIL,fail);
                        hourSumMap.put(hourTime,sumMap);
                    }else{
                        success += sumMap.get(MonitorConstants.SUCCESS);
                        fail += sumMap.get(MonitorConstants.FAIL);
                        sumMap.put(MonitorConstants.SUCCESS,success);
                        sumMap.put(MonitorConstants.FAIL,fail);
                    }
                }
            }

            if(is_ok) {
                invokeReportManager.saveConsumerByAppOnHour(applicationName, yesterDay, saveMap);
            }
        }
    }
}
