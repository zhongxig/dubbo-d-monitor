
# 项目介绍
界面效果图：待添加
基于dubbo-monitor的二次开发，兼容了dubbo-admin的部分特性,数据库为redis

1. 数据库：redis
2. 前端：freemarker+metronic+echarts
3. 框架：springmvc
4. dubbo版本号：dubbo-2.5.3

#使用帮助
在IDEA中
#### 1.初始化配置：
    web/src/main/resources/application.properties

		dubbo.application.name = ants-monitor
		dubbo.port = 20882 #此处为port地址
		zookeeper.address = 127.0.0.1:2181 # 此处为zk地址
		redis.host  = redis://ants-monitor:123456@127.0.0.1:6379/2 #此处为redis数据库 密码@ip:host/db号


> **注意：**因用autoconfig，若使用mvn clean package -Dmaven.test.skip -U打包，则需在 `web/src/main/webapps/META-INFO/autoconf/auto-conf.xml`中进行初始化数据更改。

#### 2.两种启动方式，选一种即可（本地推荐法一）：
    1）jetty: web/test/java/AntsMonitorServer ->main 方法启动即可
    2）tomcat: 先`mvn clean package -Dmaven.test.skip -U` 打包,而后将war包放在tomcat webapps下启动tomcat即可




