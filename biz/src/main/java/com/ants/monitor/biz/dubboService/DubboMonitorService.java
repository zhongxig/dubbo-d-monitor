package com.ants.monitor.biz.dubboService;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.monitor.MonitorService;
import com.ants.monitor.bean.UUIDGenerator;
import com.ants.monitor.bean.bizBean.HostBO;
import com.ants.monitor.bean.entity.InvokeDO;
import com.ants.monitor.biz.support.service.HostService;
import com.ants.monitor.common.tools.TimeUtil;
import com.ants.monitor.dao.mapper.InvokeDOMapper;
import com.ants.monitor.dao.redisManager.InvokeRedisManager;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zxg on 15/11/2.
 * 15:54
 */
@Service
@Slf4j
public class DubboMonitorService implements MonitorService {

//    private Thread saveInvokeThread;

    @Autowired
    private HostService hostService;

    private BlockingQueue<URL> queue;

    private static final String POISON_PROTOCOL = "poison";


    //方法最后的消费时间
    private static final Map<String,  String> serviceFinalTimeMap = new ConcurrentHashMap<>();

//    private volatile boolean running = true;


    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;
    @Autowired
    private InvokeRedisManager invokeRedisManager;
    @Resource(name="invokeDOMapper")
    private InvokeDOMapper invokeDOMapper;


    @PostConstruct
    private void init() {
        queue = new LinkedBlockingQueue<URL>(Integer.parseInt(ConfigUtils.getProperty("dubbo.monitor.queue", "100000")));

//        saveInvokeThread = new Thread(new Runnable() {
//            public void run() {
//                while(running) {
//                    try {
//                        saveInvoke();
//                    } catch (Throwable t) { // 防御性容错
//                        log.error("Unexpected error occur at write stat log, cause: " + t.getMessage(), t);
//                        try {
//                            Thread.sleep(5000); // 失败延迟
//                        } catch (Throwable t2) {
//                        }
//                    }
//                }
//            }
//        });
//        saveInvokeThread.setDaemon(true);
//        saveInvokeThread.setName("DubboMonitorAsyncWriteLogThread");
//        saveInvokeThread.start();
    }
    //获得service最后被消费的时间
    public String getServiceConsumerTime(String serviceName){
        return serviceFinalTimeMap.get(serviceName);
    }

    @Override
    public void collect(URL statistics) {
        queue.offer(statistics);

        StartInvokeProcess startInvokeProcess = new StartInvokeProcess();
        taskExecutor.execute(startInvokeProcess);
    }

    @Override
    public List<URL> lookup(URL url) {
        return null;
    }

    //save  数据
    private void saveInvoke() throws Exception {
        if(queue.isEmpty()){
            return;
        }
        URL statistics = queue.take();
        if (POISON_PROTOCOL.equals(statistics.getProtocol())) {
            return;
        }
        String timestamp = statistics.getParameter(Constants.TIMESTAMP_KEY);
        Date now;
        if (timestamp == null || timestamp.length() == 0) {
            now = new Date();
        }else if (timestamp.length() == "yyyyMMddHHmmss".length()) {
            now = new SimpleDateFormat("yyyyMMddHHmmss").parse(timestamp);
        }  else {
            now = new Date(Long.parseLong(timestamp));
        }

        HostBO hostBO = null;
        InvokeDO dubboInvoke = new InvokeDO();

        dubboInvoke.setUuId(UUIDGenerator.getUUID());
        if (statistics.hasParameter(PROVIDER)) {
            dubboInvoke.setAppType(CONSUMER);
            dubboInvoke.setConsumerHost(statistics.getHost());
            String provider = statistics.getParameter(PROVIDER);
            int i = provider.indexOf(':');
            if (i > 0) {
                String[] providerArray = provider.split(":");
                dubboInvoke.setProviderHost(providerArray[0]);
                dubboInvoke.setProviderPort(providerArray[1]);
                hostBO = new HostBO(providerArray[0],providerArray[1]);
            }else{
                dubboInvoke.setProviderHost(provider);
            }

        } else {
            //不存储提供者记录，暂时无用
            return;
//            dubboInvoke.setAppType(PROVIDER);
//            dubboInvoke.setProviderHost(statistics.getHost());
//            dubboInvoke.setProviderPort(String.valueOf(statistics.getPort()));
//
//            String consumer = statistics.getParameter(CONSUMER);
//            int i = consumer.indexOf(':');
//            if (i > 0) {
//                String[] consumerArray = consumer.split(":");
//                dubboInvoke.setConsumerHost(consumerArray[0]);
//                dubboInvoke.setConsumerPort(consumerArray[1]);
//            }else{
//                dubboInvoke.setConsumerHost(consumer);
//            }
        }
        dubboInvoke.setApplication(statistics.getParameter(APPLICATION, ""));
        dubboInvoke.setService(statistics.getServiceInterface());
        dubboInvoke.setMethod(statistics.getParameter(METHOD));
        dubboInvoke.setInvokeTime(statistics.getParameter(TIMESTAMP, System.currentTimeMillis()));
        dubboInvoke.setSuccess(statistics.getParameter(SUCCESS, 0));
        dubboInvoke.setFailure(statistics.getParameter(FAILURE, 0));
        dubboInvoke.setElapsed(statistics.getParameter(ELAPSED, 0));
        dubboInvoke.setConcurrent(statistics.getParameter(CONCURRENT, 0));
        dubboInvoke.setMaxElapsed(statistics.getParameter(MAX_ELAPSED, 0));
        dubboInvoke.setMaxConcurrent(statistics.getParameter(MAX_CONCURRENT, 0));


        String date = TimeUtil.getDateString(now);
        String hour = TimeUtil.getHourString(now);
        dubboInvoke.setInvokeDate(date);
        dubboInvoke.setInvokeHour(hour);

        if (dubboInvoke.getSuccess() == 0 && dubboInvoke.getFailure() == 0 && dubboInvoke.getElapsed() == 0
                && dubboInvoke.getConcurrent() == 0 && dubboInvoke.getMaxElapsed() == 0 && dubboInvoke.getMaxConcurrent() == 0) {
            return;
        }



        SaveInvokeThread saveInvokeThread = new SaveInvokeThread(dubboInvoke,hour);
        taskExecutor.execute(saveInvokeThread);

        //保存其最后被消费时间
        if(hostBO != null) {
            String time = TimeUtil.getTimeString(now);
            String this_service = statistics.getServiceKey();
            Set<String> serviceSet = hostService.getServiceByHost(hostBO);
            for(String service:serviceSet) {
                if(service.startsWith(this_service)) {
                    serviceFinalTimeMap.put(service, time);
                    break;
                }
            }
        }
    }


    //内部线程类，利用线程池异步存储发送过来的统计数据
    @AllArgsConstructor
    @NoArgsConstructor
    private class SaveInvokeThread implements Runnable {
        private InvokeDO invokeDO;

        private String hour;
        @Override
        public void run() {
            // 缓存放一份
            invokeRedisManager.saveInvoke(hour, invokeDO);
            // 持久化放一份
            invokeDOMapper.insertSelective(invokeDO);
        }
    }

    //内部线程类，开始处理url数据
    private class StartInvokeProcess implements Runnable {
        @Override
        public void run() {
            try {
                saveInvoke();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
