package com.ants.monitor.biz.bussiness;

import com.ants.monitor.bean.bizBean.MethodRankBO;

import java.util.List;

/**
 * Created by zxg on 16/7/4.
 * 11:39
 * no bug,以后改代码的哥们，祝你好运~！！
 */
public interface InvokeBiz {

    /**
     * 根据app名称获得前15位方法排行榜
     * @param appName 应用名称
     * @return 按使用次数从大到小的最多15位排行
     */
    List<MethodRankBO> getMethodRankByAppName(String appName);



}
