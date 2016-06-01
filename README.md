
# 项目介绍

## 注意：branch
#### `master` - redis版本
#### `monitor-for-mysql` - mysql版本

## 界面效果图：
![image](https://github.com/zhongxig/ants-monitor-on-Redis/raw/master/monitor-dashboard.png)

---

## 一、特性：
1. 修复dubbo-admin的bug:本地应用启动停止后，若其相同的应用名服务仍有其他版本号，此本地应用的版本号不会注销。
2. 添加实时监控应用的启动和停止，防止应用停止影响公司业务。
3. 支持单点重启，监控到监控中心停止时，注册到zk的应用--启动和停止情况。
4. 以图像形式直观描述服务间的调用情况，方便服务结构调优。
5. 数据统计精准到方法级别。

---

## 二、模块说明:
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

---

# 三、项目使用的技术

1. 数据库：redis（分支：mysql）
2. 前端：freemarker+metronic+echarts
3. 框架：springmvc
4. dubbo版本号：dubbo-2.5.3
5. 支持的启动容器：jetty 和 tomcat
6. 其他：autoconfig、maven、ShardedJedisPool

---

# 四、使用帮助
在IDEA中
#### 1.初始化配置：
    web/src/main/resources/application.properties

		dubbo.application.name = ants-monitor
		dubbo.port = 20882 #此处为port地址
		zookeeper.address = 127.0.0.1:2181 # 此处为zk地址
		redis.host  = redis://ants-monitor:123456@127.0.0.1:6379/2 #此处为redis数据库 密码@ip:host/db号

	com.ants.monitor.bean.MonitorConstants的initEcsMap方法，在内存中存入自定义线上ip地址和测试环境ip地址。

> **注意：**因用autoconfig，若使用mvn clean package -Dmaven.test.skip -U打包，则需在 `web/src/main/webapps/META-INFO/autoconf/auto-conf.xml`中进行初始化数据更改。

#### 2.容灾设置，未启动的应用自定义实现报警
    `AppStopTaskController` 类的todo之处
    
#### 3.两种启动方式，选一种即可（本地推荐法一）：
    1）jetty: web/test/java/AntsMonitorServer ->main 方法启动即可
    2）tomcat: 先`mvn clean package -Dmaven.test.skip -U` 打包,而后将war包放在tomcat webapps下启动tomcat即可

---

# 五、更改历史

### 2016.05.01-`monitor-for-mysql`
很久没更新了～！！ 为了优化存储和数据展示，参考了zabbix 的部分思路，将数据按时间存储不一致的粒度，且更改数据库存储为mysql，redis仅做缓存，由于该系统目前维护就我*一个人*，因此后续就维护 mysql版本，redis版本暂时不维护：
> monitor-on-redis 1.1.0:
1. 新增分支：monitor-for-mysql
2. 更改项目名为：Dmonitor
2. 优化了页面展示
2. 变更持久化数据存储到mysql中，且保存30天，过期的数据定时器删除
3. 及时纪录系统间的调用数据，根据2个小时，一天的粒度进行数据划分，短期存入redis，每日凌晨定时器将数据存入mysql
4. fix 部分隐藏bug


### 2016.03.09
> monitor-on-redis 1.0.4:
1. 内存占用仍旧很夸张，毕竟是用redis做数据库，invoke 大数据 过期时间改为1天，待后续优化方案
2. redis释放资源 改为 shardedJedis.close();fix redis 无法释放资源的bug


### 2016.03.05
由于运维发现此应用占用redis内存过高，且无用，因此更改redis的存储时间
> monitor-on-redis 1.0.3:
1. 拆分invoke每日数据，每日凌晨统计数据报表。redis 数据基本都作过期化存储。

### 2016.01.20
> monitor-on-redis 1.0.2:
1. 新增——本地非法起1.0.0.daily 和测试 非法起1.0.0 应用的警告页面，帮助用户快速定位问题
2. 新增对 禁止掉的以override为头的url 记录，页面暂时不实现禁止的内容。展示所有禁止的数据。

### 2016.01.08
> 1. 优化pom依赖，取出zk的重复引入

### 2016.01.07
> monitor-on-redis 1.0.1:
1. 修复cpu占用过大的bug
2. 更新readme

### 2016.01.05
> 1. 上线1.0.0-redis版本的监控中心


e