package com.ants.monitor.controller.show;

import com.alibaba.dubbo.common.Constants;
import com.ants.monitor.bean.MonitorConstants;
import com.ants.monitor.bean.ResultVO;
import com.ants.monitor.bean.bizBean.HostBO;
import com.ants.monitor.bean.bizBean.ServiceBO;
import com.ants.monitor.bean.entity.InvokeDO;
import com.ants.monitor.biz.support.service.HostService;
import com.ants.monitor.biz.support.service.ServicesService;
import com.ants.monitor.common.tools.TimeUtil;
import com.ants.monitor.dao.redisManager.InvokeRedisManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

/**
 * Created by zxg on 15/11/19.
 */
@Controller
@RequestMapping("/monitor/services")
public class ServicesController {
    @Autowired
    private ServicesService servicesService;
    @Autowired
    private InvokeRedisManager invokeRedisManager;
    @Autowired
    private HostService hostService;

    //主页
    @RequestMapping(value = "main")
    public ModelAndView main() {
        return new ModelAndView("monitorView/services/servicesIndex");
    }

    //所有service
    @RequestMapping(value = "/getAllService", method = RequestMethod.GET)
    public
    @ResponseBody
    ResultVO getAllService() {
        try {
            Map<String, Object> resultMap = new HashMap<>();

            Map<String, ServiceBO> allServicesMap = servicesService.getServiceBOMap();
            List<String> wrongMethodsList = new ArrayList<>();
            List<String> wrongAppList = new ArrayList<>();
            List<String> wrongHostServiceList = new ArrayList<>();

            for (Map.Entry<String, ServiceBO> serviceBOEntry : allServicesMap.entrySet()) {
                String serviceName = serviceBOEntry.getKey();
                ServiceBO serviceBO = serviceBOEntry.getValue();

                Set<String> ownerSet = serviceBO.getOwnerApp();
                Set<String> methodSet = serviceBO.getMethods();
                if(null != ownerSet && ownerSet.size() > 1){
                    wrongAppList.add(serviceName);
                }
                if(null != methodSet && methodSet.size() > 1){
                    wrongMethodsList.add(serviceName);
                }
                if(serviceBO.getIsHostWrong()){
                    wrongHostServiceList.add(serviceName);
                }

            }

            //测试环境url
            Set<String> testUrlSet = new HashSet<>();
            for(Map.Entry<String,String> entry : MonitorConstants.ecsTestMap.entrySet()){
                testUrlSet.add(entry.getKey());
                testUrlSet.add(entry.getValue());
            }

            resultMap.put("wrongAppList", wrongAppList);
            resultMap.put("wrongMethodsList", wrongMethodsList);
            resultMap.put("wrongHostServiceList", wrongHostServiceList);
            resultMap.put("allServicesMap", allServicesMap);
            resultMap.put("testUrlSet", testUrlSet);

            return ResultVO.wrapSuccessfulResult(resultMap);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.wrapErrorResult(e.getMessage());
        }

    }

    //获得此service下的 方法 当前时间的调用量
    @RequestMapping(value = "/getMethodSumOneDay", method = RequestMethod.GET)
    public
    @ResponseBody
    ResultVO getMethodSumOneDay(String serviceName,String methodName ,String type){
        List<String> recentDateList = getRecentDay(type);
        //判断 host 是否有这个 service
        List<String> haveList = new ArrayList<>();
        List<String> notHaveList = new ArrayList<>();

        Map<String,Object> resultMap = new HashMap<>();
        //消费的app
        Set<String> appList = new HashSet<>();

//        {hour:{success:xxx,elapsed:xxx}}
        Map<String,Object> dataMap = new HashMap<>();
        for(String date : recentDateList) {
            List<InvokeDO> methodList = invokeRedisManager.getInvokeByMethodDay(serviceName.split(":")[0],methodName,date);
            for(InvokeDO invokeDO : methodList){
                String providerHost = invokeDO.getProviderHost();
                String providerPort = invokeDO.getProviderPort();
                String providerKey = providerHost + "-" + providerPort;

                if(notHaveList.contains(providerKey)){
                    // 非此service
                    continue;
                }
                if(!haveList.contains(providerKey)) {
                    Set<String> serviceSet = hostService.getServiceByHost(new HostBO(providerHost, providerPort));
                    if (!serviceSet.contains(serviceName)) {
                        // 非此service
                        notHaveList.add(providerKey);
                        continue;
                    }
                    haveList.add(providerKey);
                }

                appList.add(invokeDO.getApplication());
                Integer successNum = invokeDO.getSuccess();
                Integer elapsedNum = invokeDO.getElapsed();

                String hourTime  = invokeDO.getInvokeHour();

                Map<String,Integer> hourMap = (Map<String, Integer>) dataMap.get(hourTime);
                if(hourMap == null){
                    hourMap = new HashMap<>();
                    dataMap.put(hourTime,hourMap);
                }
                Integer oldSuccessNum = hourMap.get("success") == null?0:hourMap.get("success");
                Integer oldElapsedNum = hourMap.get("elapsed") == null?0:hourMap.get("elapsed");

                hourMap.put("success",oldSuccessNum+successNum);
                hourMap.put("elapsed",oldElapsedNum+elapsedNum);
            }

        }

        resultMap.put("dataMap",dataMap);
        resultMap.put("appList",appList);

        return ResultVO.wrapSuccessfulResult(resultMap);
    }

    //获得此service的tps和并发量,以分钟为单位 map结构-----{provider:时间：方法：成功失败值} 四层
    @RequestMapping(value = "/getChartByName", method = RequestMethod.GET)
    public
    @ResponseBody
    ResultVO getChartByName(String serviceName,String type){
        Map<String,ServiceBO> serviceBOMap = servicesService.getServiceBOMap();
        ServiceBO thisServiceBO = serviceBOMap.get(serviceName);
        if(null == thisServiceBO){
            return ResultVO.wrapErrorResult("serviceName is wrong");
        }
        Set<String> methodsSet = thisServiceBO.getMethods();
        if(methodsSet.isEmpty() || methodsSet.size() > 1){
            return ResultVO.wrapErrorResult("methods is not only one");
        }

        Map<String,Object> resultMap = new HashMap<>();
        Map<String,Object> providerMap = new HashMap<>();
        Map<String,Object> consumerMap = new HashMap<>();
        resultMap.put("provider",providerMap);
        resultMap.put("consumer", consumerMap);


        List<String> recentDateList = getRecentDay(type);
        // 根据service名称获得当日
        for(String date : recentDateList){
            List<InvokeDO> invokeDOList = invokeRedisManager.getInvokeByDate(date);
            for(InvokeDO invokeDO : invokeDOList){
                String providerHost = invokeDO.getProviderHost();
                String providerPort = invokeDO.getProviderPort();
                Set<String> serviceSet = hostService.getServiceByHost(new HostBO(providerHost, providerPort));
                if(!serviceSet.contains(serviceName)){
                    // 非此service
                    continue;
                }
                String service = invokeDO.getService();
                if (!serviceName.contains(service)) {
                    continue;
                }
                // 根据类型 指定不同map
                String invokeType = invokeDO.getAppType();
                Map<String,Object> map;
                if(invokeType.equals(Constants.PROVIDER)){
                    map = providerMap;
                }else{
                    map = consumerMap;
                }

                String method = invokeDO.getMethod();
                Integer success = invokeDO.getSuccess();
//                Integer fail = invokeDO.getFailure();
                Integer elapsed = invokeDO.getElapsed();
                // 时间转化为分钟.每10分钟
                Long invokeTime = invokeDO.getInvokeTime();
                Long afterMinute = (invokeTime / (60000*10)) * 60000*10;
                String minuteTime = TimeUtil.getMinuteString(new Date(afterMinute));


                //时间 map
                Map<String,Object> timeMap = (Map<String, Object>) map.get(minuteTime);
                if(timeMap == null){
                    timeMap = new HashMap<>();
                    map.put(minuteTime,timeMap);
                }

                //方法 map
                Map<String,Integer> methodMap = (Map<String, Integer>) timeMap.get(method);
                if(methodMap == null){
                    methodMap = new HashMap<>();
                    timeMap.put(method,methodMap);
                }


                Integer oldSuccess = methodMap.get("success") == null ? 0 :methodMap.get("success");
//                Integer oldFail = tpsMap.get("fail") == null ? 0 :tpsMap.get("fail");
                Integer oldElapsed = methodMap.get("elapsed") == null ? 0 :methodMap.get("elapsed");
                methodMap.put("success",oldSuccess+success);
//                tpsMap.put("fail",oldFail+fail);
                methodMap.put("elapsed", oldElapsed + elapsed);

            }
        }

        return ResultVO.wrapSuccessfulResult(resultMap);
    }



    // ========private
    //=======private======
    private List<String> getRecentDay(String type) {
        Integer limit = 0;
        Date date = new Date(System.currentTimeMillis());

        List<String> recentDateList = new ArrayList<>();

        if (type.equals("Today")) {
            String nowDate = TimeUtil.getDateString(date);
            recentDateList.add(nowDate);
        } else if (type.equals("Month")) {
            String nowDate = TimeUtil.getDateString(date);
            Date firstDate = TimeUtil.getMinMonthDate(nowDate);
            String firstDateString = TimeUtil.getDateString(firstDate);
            while (!firstDateString.equals(nowDate)) {
                recentDateList.add(firstDateString);
                limit++;
                firstDateString = TimeUtil.getBeforDateByNumber(firstDate, limit);
            }
        } else {
            if (type.equals("Seven_DAY")) {
                limit = 7;
            } else if (type.equals("Fifteen_DAT")) {
                limit = 15;
            }else if (type.equals("Yesterday")) {
                limit = 1;
            }
            for (Integer amount = -limit; amount < 0; amount++) {
                recentDateList.add(TimeUtil.getBeforDateByNumber(date, amount));
            }
        }
        return recentDateList;
    }
}
