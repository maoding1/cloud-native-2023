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

