package com.ants.monitor.bean.entity;

import lombok.Data;

import java.util.Date;

@Data
public class InvokeDO {
    private Integer autoId;

    private String id;

    private String application;

    private String service;

    private String method;

    private String consumerHost;

    private String consumerPort;

    private String providerHost;

    private String providerPort;

    private String appType;

    private Long invokeTime;

    private Integer success;

    private Integer failure;

    private Integer elapsed;

    private Integer concurrent;

    private Integer maxElapsed;

    private Integer maxConcurrent;

    private Date invokeDate;

    private Date gmtCreate;
    private Date gmtModified;

//
//    public String getAppType() {
//        if (appType.isEmpty()) {
//            return "provider";
//        }
//        return appType;
//    }


    // =============辅助==
    private String invokeDateString;
}