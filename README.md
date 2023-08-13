# nju - 云原生大作业说明文档

## springboot基础功能实现

### 1. 导入依赖

除了springboot项目的一些maven依赖外，关于json对象和限流需要导入以下依赖：

![picture](https://pic4.58cdn.com.cn/nowater/webim/big/n_v25271e705c0634cd1bd725bbf7c677f24.png)

其中json对象使用了alibaba的fastjson库，而限流使用的是Google开源工具包Guava。

### 2.构建项目 实现接口和初步限流

![picture](https://pic2.58cdn.com.cn/nowater/webim/big/n_v2841540e6ed884371aafd18ffe56ae3ec.png)

### 3.  对基本功能和限流功能的测试

使用jmeter工具对接口进行测试，测试的url为localhost:8080/hello

![picture](https://pic5.58cdn.com.cn/nowater/webim/big/n_v27312a66fcf514461a1447d7e113c8bd5.png)

线程组设置：启动20个线程在一秒内循环一次，即一秒内对url发送20次请求

结果如下：有正常被接受的请求，也有因为限流策略被拒绝的请求

![image.png](https://pic1.58cdn.com.cn/nowater/webim/big/n_v2af8c6400b14c4d9197637fd31739c55d.png)

![image.png](https://pic1.58cdn.com.cn/nowater/webim/big/n_v28e97142d0ae3454abb2d825cdedd60af.png)

### bonus：对实例进行统一限流

使用tomcat完成相关功能

下载与安装：

```bash
wget https://downloads.apache.org/tomcat/tomcat-9/v9.0.52/bin/apache-tomcat-9.0.52.tar.gz
tar -xzf apache-tomcat-9.0.52.tar.gz
```

在tomcat安装目录下的conf/server.xml中进行如下配置：

```xml
<Connector port="8080" protocol="HTTP/1.1"
           connectionTimeout="5000" maxThreads="100"
           redirectPort="8080" />
```

- `port`: 指定连接器监听的端口号，这里设置为8080，表示springboot应用启动的短裤。
- `protocol`: 指定连接器使用的协议，这里使用的是HTTP/1.1。
- `connectionTimeout`: 定义连接的超时时间，即如果连接在指定的时间内没有活动，则会被关闭。这里设置为5000毫秒（5秒）。
- `maxThreads`: 定义了最大线程数，即Tomcat容器可以同时处理的最大请求数量。当并发请求数超过此限制时，新的请求将排队等待处理。这里设置为100，表示最大线程数为100。
- `redirectPort`: 指定重定向端口，当连接器接收到安全请求（例如HTTPS）时，它会将请求重定向到指定的端口。这里设置为8080，表示不进行重定向。