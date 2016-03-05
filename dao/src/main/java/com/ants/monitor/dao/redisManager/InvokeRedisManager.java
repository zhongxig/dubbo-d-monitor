package com.ants.monitor.dao.redisManager;

import com.ants.monitor.bean.entity.InvokeDO;
import com.ants.monitor.common.redis.RedisClientTemplate;
import com.ants.monitor.common.redis.RedisKeyBean;
import com.ants.monitor.common.tools.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zxg on 15/11/6.
 * invoke核心数据存储类
 */
@Slf4j
@Service
public class InvokeRedisManager {


    @Autowired
    private RedisClientTemplate redisClientTemplate;

    //存 invoke,有效期是2天
    public void saveInvoke(String date,InvokeDO invokeDO){
        String key = String.format(RedisKeyBean.invokeListDate, date);
        String jsonString = JsonUtil.objectToJsonStr(invokeDO);

        redisClientTemplate.rPushList(key, jsonString, RedisKeyBean.RREDIS_EXP_DAY*2);
    }

    // 获得该日期的所有invoker对象
    public List<InvokeDO> getInvokeByDate(String date){
        List<InvokeDO> resultList = new ArrayList<>();
        String key = String.format(RedisKeyBean.invokeListDate, date);

        List<String> jsonList = redisClientTemplate.getList(key, 0, -1);

        for(String jsonString : jsonList){
            InvokeDO invokeDO = JsonUtil.jsonStrToObject(jsonString,InvokeDO.class);
            resultList.add(invokeDO);
        }

        return resultList;
    }



}
