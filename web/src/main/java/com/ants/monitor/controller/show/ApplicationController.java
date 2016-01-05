package com.ants.monitor.controller.show;

import com.alibaba.dubbo.common.Constants;
import com.ants.monitor.bean.ResultVO;
import com.ants.monitor.bean.bizBean.ApplicationBO;
import com.ants.monitor.bean.bizBean.HostBO;
import com.ants.monitor.bean.bizBean.ServiceBO;
import com.ants.monitor.bean.entity.InvokeDO;
import com.ants.monitor.dao.redisManager.InvokeRedisManager;
import com.ants.monitor.biz.support.service.ApplicationService;
import com.ants.monitor.biz.support.service.HostService;
import com.ants.monitor.common.tools.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

/**
 * Created by zxg on 15/11/7.
 * 16:39
 */
@Controller
@RequestMapping("/monitor/application")
public class ApplicationController {
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private HostService hostService;

    @Autowired
    private InvokeRedisManager invokeRedisManager;

    //主页
    @RequestMapping(value = "main")
    public ModelAndView main() {
        return new ModelAndView("monitorView/application/appIndex");
    }


    //所有服务和其相关信息
    @RequestMapping(value = "/getAllAPPAndRelation", method = RequestMethod.GET)
    public
    @ResponseBody
    ResultVO getAllAPPAndRelation() {
        try {
            Map<String, Object> resultMap = new HashMap<>();

            Map<String, ApplicationBO> allApplicationsMap = applicationService.getApplicationsBOMap();
            List<ApplicationBO> appList = new ArrayList<>();

            Set<String> groupSet = new HashSet<>();
            for (Map.Entry<String, ApplicationBO> applicationBOEntry : allApplicationsMap.entrySet()) {
                ApplicationBO applicationBO = applicationBOEntry.getValue();
                String organization = applicationBO.getOrganization();
                groupSet.add(organization);

                Set<ServiceBO> serviceSet = applicationBO.getServiceSet();
                Set<String> providersSet = applicationBO.getProvidersSet();
                Set<String> consumersSet = applicationBO.getConsumersSet();
                if (null != serviceSet) applicationBO.setServiceSum(serviceSet.size());
                if (null != providersSet) applicationBO.setProviderSum(providersSet.size());
                if (null != consumersSet) applicationBO.setConsumerSum(consumersSet.size());
                appList.add(applicationBO);
            }


            resultMap.put("appSum", allApplicationsMap.keySet().size());
            resultMap.put("groupSum", groupSet.size());
            resultMap.put("appList", appList);
            resultMap.put("allApp", allApplicationsMap);
            return ResultVO.wrapSuccessfulResult(resultMap);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.wrapErrorResult(e.getMessage());
        }

    }

    //获得target消耗source在时间内频次
    @RequestMapping(value = "/getSuccessByConsumer", method = RequestMethod.GET)
    public
    @ResponseBody
    ResultVO getSuccessByConsumer(String type, String source) {
        Map<String, Object> resultMap = new HashMap<>();

        List<String> recentDateList = getRecentDay(type);
        ApplicationBO applicationBO = applicationService.getApplicationsBOMap().get(source);
        Set<String> providerSet = applicationBO.getProvidersSet();
        Set<String> consumerSet = applicationBO.getConsumersSet();
        if (providerSet == null) providerSet = new HashSet<>();
        if (consumerSet == null) consumerSet = new HashSet<>();

        Map<String, Integer> providerMap = new HashMap<>();
        Map<String, Integer> consumerMap = new HashMap<>();
        // 即是消费者 又是提供者
        Map<String, Map<String, Integer>> providerConsumerMap = new HashMap<>();
        resultMap.put("provider", providerMap);
        resultMap.put("consumer", consumerMap);
        resultMap.put("providerConsumer", providerConsumerMap);

        // 已appStyle 为 consumer 为主 统计
        for(String date : recentDateList) {
            List<InvokeDO> invokeDOList = invokeRedisManager.getInvokeByDate(date);
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
                if(source.equals(appName)){
                    Integer success = invokeDO.getSuccess();
                    // app 作为消费者，被提供
                    Integer providerSum = providerMap.get(providerName) == null? 0:providerMap.get(providerName);
                    providerSum += success;
                    providerMap.put(providerName,providerSum);
                }
                if(source.equals(providerName)) {
                    // app 作为提供者，被消费
                    Integer success = invokeDO.getSuccess();
                    Integer consumerSum = consumerMap.get(appName) == null ? 0:consumerMap.get(appName);
                    consumerSum += success;
                    consumerMap.put(appName,consumerSum);
                }
            }
        }

        // 生成二者共有的，并补充无记录数的数据
        Set<String> consumerKeySet = consumerMap.keySet();
        Set<String> providerKeySet = providerMap.keySet();
        Integer sum = 0;

        for (String providerName : providerSet) {
            if (consumerSet.contains(providerName)) {
                Integer providerSum = providerMap.get(providerName) == null ? 0 : providerMap.get(providerName);
//                Integer consumerSum = consumerMap.get(providerName);
                Map<String, Integer> map = new HashMap<>();
                map.put("provider", providerSum);
                providerConsumerMap.put(providerName, map);
                providerMap.remove(providerName);
            }else{
                if(!providerKeySet.contains(providerName)) {
                    providerMap.put(providerName, sum);
                }
            }
        }

        for (String consumerName : consumerSet) {
            if (providerSet.contains(consumerName)) {
                Integer consumerSum = consumerMap.get(consumerName) == null ? 0 : consumerMap.get(consumerName);
                providerConsumerMap.get(consumerName).put("consumer", consumerSum);
                consumerMap.remove(consumerName);
            } else {
                if(!consumerKeySet.contains(consumerName)) {
                    consumerMap.put(consumerName, sum);
                }
            }
        }
        return ResultVO.wrapSuccessfulResult(resultMap);
    }

    // 按时间
    @RequestMapping(value = "/getSuccessByConsumerOnHour", method = RequestMethod.GET)
    public
    @ResponseBody
    ResultVO getSuccessByConsumerOnHour(String type, String source) {
        //{consumers : time : success/fial}
        Map<String, Object> resultMap = new HashMap<>();


        ApplicationBO applicationBO = applicationService.getApplicationsBOMap().get(source);
        Set<String> consumerSet = applicationBO.getConsumersSet();
        if (consumerSet == null) consumerSet = new HashSet<>();

        List<String> recentDateList = getRecentDay(type);

        for(String date : recentDateList) {
            List<InvokeDO> invokeDOList = invokeRedisManager.getInvokeByDate(date);
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
                if(source.equals(providerName)) {
                    // app 作为提供者，被消费
                    Integer success = invokeDO.getSuccess();
                    Integer fail = invokeDO.getFailure();
                    // 时间转化为小时
                    Long invokeTime = invokeDO.getInvokeTime();
                    Long afterHour = (invokeTime / (60 * 1000 * 60)) * (60 * 60 * 1000);
                    String hourTime = TimeUtil.getMinuteString(new Date(afterHour));
                    // 存储
                    Map<String, Object> hourSumMap = (Map<String, Object>) resultMap.get(appName);
                    if(null == hourSumMap){
                        hourSumMap = new HashMap<>();
                        resultMap.put(appName,hourSumMap);
                    }
                    Map<String,Integer> sumMap = (Map<String, Integer>) hourSumMap.get(hourTime);
                    if(sumMap == null){
                        sumMap = new HashMap<>();
                        sumMap.put("success",success);
                        sumMap.put("fail",fail);
                        hourSumMap.put(hourTime,sumMap);
                    }else{
                        success += sumMap.get("success");
                        fail += sumMap.get("fail");
                        sumMap.put("success",success);
                        sumMap.put("fail",fail);
                    }
                }
            }
        }

        //补充为0的数据
        for (String consumerName : consumerSet) {
            Map<String, Object> hourSumMap = (Map<String, Object>) resultMap.get(consumerName);
            if(null == hourSumMap){
                hourSumMap = new HashMap<>();
                resultMap.put(consumerName,hourSumMap);
            }
        }

        return ResultVO.wrapSuccessfulResult(resultMap);

    }


    //=======private======
    private List<String> getRecentDay(String type) {
        Integer limit = 0;
        Date date = new Date(System.currentTimeMillis());

        List<String> recentDateList = new ArrayList<>();

        if (type.equals("Month")) {
            String nowDate = TimeUtil.getDateString(date);
            Date firstDate = TimeUtil.getMinMonthDate(nowDate);
            String firstDateString = TimeUtil.getDateString(firstDate);
            while (!firstDateString.equals(nowDate)) {
                recentDateList.add(firstDateString);
                limit++;
                firstDateString = TimeUtil.getBeforDateByNumber(firstDate, limit);
            }
        } else {
            switch (type) {
                case "Seven_DAY":
                    limit = 7;
                    break;
                case "Fifteen_DAT":
                    limit = 15;
                    break;
                case "Yesterday":
                    limit = 1;
                    break;
            }
            for (Integer amount = -limit; amount < 0; amount++) {
                recentDateList.add(TimeUtil.getBeforDateByNumber(date, amount));
            }
        }
        return recentDateList;
    }
}
