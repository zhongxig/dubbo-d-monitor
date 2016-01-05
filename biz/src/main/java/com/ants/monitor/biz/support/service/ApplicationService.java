package com.ants.monitor.biz.support.service;

import com.ants.monitor.bean.bizBean.ApplicationBO;
import com.ants.monitor.bean.bizBean.HostBO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by zxg on 15/11/11.
 */
public interface ApplicationService {

    Set<String> getAllApplications();

    // app名称，app对象
    Map<String,ApplicationBO> getApplicationsBOMap();
}
