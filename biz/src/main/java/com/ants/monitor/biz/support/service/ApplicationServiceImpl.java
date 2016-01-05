package com.ants.monitor.biz.support.service;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.ants.monitor.bean.MonitorConstants;
import com.ants.monitor.bean.bizBean.ApplicationBO;
import com.ants.monitor.bean.bizBean.HostBO;
import com.ants.monitor.bean.bizBean.ServiceBO;
import com.ants.monitor.biz.dubboService.RegistryContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by zxg on 15/11/11.
 */
@Service
public class ApplicationServiceImpl implements ApplicationService {


    @Autowired
    private RegistryContainer registryContainer;

    @Autowired
    private ServicesService servicesService;

    @Override
    public Set<String> getAllApplications() {
        Set<String> resultApplications = new ConcurrentHashSet<>();

        Map<String, Map<String, Set<URL>>> registry = registryContainer.getRegistryCache();

        Map<String, Set<URL>> providersServices = registry.get(Constants.PROVIDERS_CATEGORY);
        Map<String, Set<URL>> consumersServices = registry.get(Constants.CONSUMERS_CATEGORY);
        for (Map.Entry<String, Set<URL>> serviceEntry : providersServices.entrySet()) {
            Set<URL> urls = serviceEntry.getValue();
            for (URL url : urls) {
                String application = url.getParameter(Constants.APPLICATION_KEY);
                if (null != application) {
                    resultApplications.add(application);
                }
            }
        }
        for (Map.Entry<String, Set<URL>> serviceEntry : consumersServices.entrySet()) {
            Set<URL> urls = serviceEntry.getValue();
            for (URL url : urls) {
                String application = url.getParameter(Constants.APPLICATION_KEY);
                if (null != application) {
                    resultApplications.add(application);
                }
            }
        }

        return resultApplications;
    }

    @Override
    public Map<String, ApplicationBO> getApplicationsBOMap() {
        Map<String, ApplicationBO> appMap = new HashMap<>();

        Map<String, ServiceBO> serviceMap = new HashMap<>();

        Map<String, Map<String, Set<URL>>> registry = registryContainer.getRegistryCache();
        Map<String, Set<String>> serviceProviders = servicesService.getServiceProviders();

        Map<String, Set<URL>> providersServices = registry.get(Constants.PROVIDERS_CATEGORY);
        Map<String, Set<URL>> consumersServices = registry.get(Constants.CONSUMERS_CATEGORY);

        // 提供者---拼基本信息
        for (Map.Entry<String, Set<URL>> serviceEntry : providersServices.entrySet()) {
            String serviceName = serviceEntry.getKey();
            Set<URL> urls = serviceEntry.getValue();
            for (URL url : urls) {
                Set<HostBO> hostList = new HashSet<>();

                String application = url.getParameter(Constants.APPLICATION_KEY);
                ApplicationBO applicationBO = appMap.get(application);
                if (null == applicationBO) {
                    String organization = url.getParameter(MonitorConstants.ORGANICATION);
                    applicationBO = new ApplicationBO();
                    applicationBO.setApplicationName(application);
                    if(StringUtils.isEmpty(applicationBO.getOrganization())) applicationBO.setOrganization(organization == null ? "" : organization);
                    appMap.put(application, applicationBO);
                } else {
                    hostList = applicationBO.getHostList();
                }
                hostList.add(new HostBO(url.getHost(), String.valueOf(url.getPort())));
                applicationBO.setHostList(hostList);
                applicationBO.setIsProvider(true);

                String finalTime = registryContainer.getServiceConsumerTime(serviceName);
                ServiceBO serviceBO = serviceMap.get(serviceName);
                String methods = url.getParameter(Constants.METHODS_KEY);
                if(serviceBO == null){
                    serviceBO = new ServiceBO();
                    String owners = url.getParameter(MonitorConstants.OWNER);
                    serviceBO.setOwner(owners == null ? "" : owners);
                    serviceBO.setServiceName(serviceName);
                    Set<String> methodSet = serviceBO.getMethods() ;
                    if(null == methodSet) methodSet = new HashSet<>();
                    methodSet.add(methods);
                    serviceBO.setMethods(methodSet);

                    serviceBO.setFinalConsumerTime(finalTime);
                    serviceMap.put(serviceName, serviceBO);

                }else {
                    Set<String> methodSet = serviceBO.getMethods() ;
                    String oldMethods = methodSet.iterator().next();
                    List<String> old_list = Arrays.asList(oldMethods.split(","));
                    List<String> now_list = Arrays.asList(methods.split(","));
                    Boolean isWrong = false;
                    if(old_list.size() != now_list.size()) isWrong = true;
                    for(String one_method : now_list){
                        if(!old_list.contains(one_method)){
                            // 存在不同方法
                            isWrong = true;
                            break;
                        }
                    }
                    if(isWrong){
                        serviceBO.setIsWrong(true);
                        methodSet.add(methods);
                        serviceBO.setMethods(methodSet);
                        serviceMap.put(serviceName, serviceBO);
                    }
                }

            }

        }

        // 消费者--拼 依赖和消费的app
        for (Map.Entry<String, Set<URL>> serviceEntry : consumersServices.entrySet()) {
            String serviceName = serviceEntry.getKey();
            //其service的 提供方
            Set<String> providers = serviceProviders.get(serviceName);
            if (providers == null) {
                //无提供者者 暂时此处不处理
                providers = new HashSet<>();
            }
            Set<URL> urls = serviceEntry.getValue();
            for (URL url : urls) {
                Set<HostBO> hostList = new HashSet<>();

                String applicationName = url.getParameter(Constants.APPLICATION_KEY);

                String owners = url.getParameter(MonitorConstants.OWNER);

                ApplicationBO consumerApplicationBO = appMap.get(applicationName);
                if (null == consumerApplicationBO) {
                    // 此app 未提供服务
                    consumerApplicationBO = new ApplicationBO();
                    String organization = url.getParameter(MonitorConstants.ORGANICATION);
                    consumerApplicationBO.setApplicationName(applicationName);
                    if(StringUtils.isEmpty(consumerApplicationBO.getOrganization())) consumerApplicationBO.setOrganization(organization == null ? "" : organization);

                    hostList.add(new HostBO(url.getHost(),null));
                    consumerApplicationBO.setHostList(hostList);
                    appMap.put(applicationName, consumerApplicationBO);
                }
                if (!consumerApplicationBO.getIsProvider()) {
                    // 未提供app服务的host
                    hostList = consumerApplicationBO.getHostList();
                    hostList.add(new HostBO(url.getHost(),null));
                    consumerApplicationBO.setHostList(hostList);
                }
                Set<String> providersSet = consumerApplicationBO.getProvidersSet();
                if (null == providersSet) {
                    providersSet = new HashSet<>();
                }
                for (String provider : providers) {
                    providersSet.add(provider);
                }
                consumerApplicationBO.setProvidersSet(providersSet);
                consumerApplicationBO.setIsConsumer(true);
                consumerApplicationBO.setOwner(owners == null ? "" : owners);


                for (String provider : providers) {
                    ApplicationBO providerApplicationBO = appMap.get(provider);
                    if (null != providerApplicationBO) {
                        Set<String> consumersSet = providerApplicationBO.getConsumersSet();
                        if (null == consumersSet) consumersSet = new HashSet<>();
                        // 提供了这些app
                        consumersSet.add(applicationName);
                        providerApplicationBO.setConsumersSet(consumersSet);

                        ServiceBO serviceBO = serviceMap.get(serviceName);
                        if (null != serviceBO) {
                            // 此方法存在消费者
                            serviceBO.setIsConsumer(true);
                        }
                    }
                }

            }
        }

        // 存serviceBO
        for (Map.Entry<String, ServiceBO> serviceBOEntry : serviceMap.entrySet()) {
            String service = serviceBOEntry.getKey();
            ServiceBO serviceBO = serviceBOEntry.getValue();
            Set<String> appSet = serviceProviders.get(service);
            for (String appName : appSet) {
                ApplicationBO applicationBO = appMap.get(appName);
                Set<ServiceBO> serviceBOSet = applicationBO.getServiceSet();
                if (null == serviceBOSet) {
                    serviceBOSet = new HashSet<>();
                }
                serviceBOSet.add(serviceBO);
                applicationBO.setServiceSet(serviceBOSet);
            }
        }
        return appMap;
    }

}
