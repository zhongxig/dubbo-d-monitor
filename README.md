
# 项目介绍
界面效果图：![image](https://github.com/zhongxig/ants-monitor-on-Redis/raw/master/monitor-dashboard.png)

## 特性：
1. 修复dubbo-admin的bug:本地应用启动停止后，若其相同的应用名服务仍有其他版本号，此本地应用的版本号不会注销。
2. 添加实时监控应用的启动和停止，防止应用停止影响公司业务。
3. 支持单点重启，监控到监控中心停止时，注册到zk的应用--启动和停止情况。
4. 以图像形式直观描述服务间的调用情况，方便服务结构调优。
5. 数据统计精准到方法级别。

## 模块说明:
1. 首页：
	* 只观查看应用、方法、host的数量和每个应用之间的调用情况，有助于优化系统间的调用关系
	* 即时监控应用的停止和启动，其会触发NotifyAppChangeProcessor中的方法，可根据自身业务写自身逻辑代码
	* 页面上显示应用的停止和启动记录
2. application:
	* 查看应用的host:port，是否是消费者或使用者
	* 其内部的方法和最后调用时间，有助于排除无用的方法
	* 每日每月的应用调用情况记录和应用之间的调用记录
3. service:
	* 查看service中的具体方法的调用情况和统计数据————TPS:每秒查询率（次/秒） ART:每次平均耗时（毫秒/次）
	* 高亮显示异常的service，方便排错
4. host:
	* 记录每个服务器上的应用部署情况


# 项目使用的技术

1. 数据库：redis
2. 前端：freemarker+metronic+echarts
3. 框架：springmvc
4. dubbo版本号：dubbo-2.5.3
5. 支持的启动容器：jetty 和 tomcat
6. 其他：autoconfig、maven、ShardedJedisPool

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




