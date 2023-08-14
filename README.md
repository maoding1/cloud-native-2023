# nju - 云原生大作业说明文档

项目地址： https://github.com/maoding1/cloud-native-2023

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

### 构建时单元测试

在/test目录下加入测试类HelloControllerTest:

```java
package com.example.hello;

import com.google.common.util.concurrent.RateLimiter;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class HelloControllerTests {

    @Mock
    private RateLimiter rateLimiter;

    @InjectMocks
    private HelloController helloController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testHelloWithTokenAcquired() throws JSONException {
        // Mock the rateLimiter.tryAcquire() method to return true
        when(rateLimiter.tryAcquire(500, TimeUnit.MILLISECONDS)).thenReturn(true);

        // Call the hello() method
        helloController.SetRateLimiter(rateLimiter);
        String response = helloController.hello();

        // Verify the response
        JSONObject jsonObject = new JSONObject(response);
        assertEquals(200, jsonObject.getInt("code"));
        assertEquals("hello cloud_native!", jsonObject.getString("msg"));
    }

    @Test
    public void testHelloWithTokenNotAcquired() throws JSONException {
        // Mock the rateLimiter.tryAcquire() method to return false
        when(rateLimiter.tryAcquire(500, TimeUnit.MILLISECONDS)).thenReturn(false);

        // Call the hello() method
        helloController.SetRateLimiter(rateLimiter);
        String response = helloController.hello();

        // Verify the response
        JSONObject jsonObject = new JSONObject(response);
        assertEquals(429, jsonObject.getInt("code"));
        assertEquals("too many requests", jsonObject.getString("msg"));
    }
}
```

在pipeline打包时使用 mvn clean test package插入执行单元测试步骤

本地执行测试结果如下：

![image.png](https://pic3.58cdn.com.cn/nowater/webim/big/n_v2f756603c9ffa4f528abd110ca70b6d20.png)

### 实现接口访问指标，并暴露给Prometheus

maven中加入如下依赖：

```xml
<!-- Spring Boot Actuator -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>

		<!-- Prometheus Java Client -->
		<dependency>
			<groupId>io.prometheus</groupId>
			<artifactId>simpleclient_spring_boot</artifactId>
			<version>0.11.0</version>
		</dependency>
<dependency>
			<groupId>io.micrometer</groupId>
			<artifactId>micrometer-registry-prometheus</artifactId>
		</dependency>
```

在项目配置中加入如下配置，暴露指标：

```properties
# 启用Actuator的所有端点
management.endpoints.web.exposure.include=*

# 配置Prometheus指标端点
management.endpoint.metrics.enabled=true
management.endpoint.metrics.path=/actuator/prometheus
```



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

- `port`: 指定连接器监听的端口号，这里设置为8080，表示springboot应用启动的端口。
- `protocol`: 指定连接器使用的协议，这里使用的是HTTP/1.1。
- `connectionTimeout`: 定义连接的超时时间，即如果连接在指定的时间内没有活动，则会被关闭。这里设置为5000毫秒（5秒）。
- `maxThreads`: 定义了最大线程数，即Tomcat容器可以同时处理的最大请求数量。当并发请求数超过此限制时，新的请求将排队等待处理。这里设置为100，表示最大线程数为100。
- `redirectPort`: 指定重定向端口，当连接器接收到安全请求（例如HTTPS）时，它会将请求重定向到指定的端口。这里设置为8080，表示不进行重定向。

## DevOps功能实现

### docker镜像构建

dockerfile文件如下：

```dockerfile
# 使用基础的Java 18镜像
FROM openjdk:18

# 设置工作目录
WORKDIR /app

# 将编译后的Spring Boot JAR文件复制到容器中
COPY ./target/hello-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
# 设置启动命令
CMD ["java", "-jar", "app.jar"]
```

###  k8s deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: project-group34
  namespace: nju34
spec:
  replicas: 2
  selector:
    matchLabels:
      app: cloud-native-34
  template:
    metadata:
      labels:
        app: cloud-native-34
    spec:
      containers:
        - name: group34-containers
          image: harbor.edu.cn/nju34/34_images:VERSION
          resources:
            requests:
              memory: 50Mi
              cpu: 50m
      #需要提前创建secret资源对象，用于从私有仓库拉取镜像，否则容器会创建失败
      imagePullSecrets:
        - name: nju34

```

### k8s service.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
  name: cloud-native-34-svc
  namespace: nju34 #规定命名空间为nju34
spec:
  type: NodePort
  selector:
    app: cloud-native-34 #与depolyment中容器的标签匹配
    ports:
      - nodePort: 30034 # 外部访问端口
        port: 8888 #集群内部端口
        targetPort: 8080 #所有流量最终路由到的端口

```

### servermonitor.yaml

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: project-group34-monitor
  namespace: nju34
  labels:
    app: cloud-native-34
spec:
  namespaceSelector:
    matchNames:
      - nju34
  selector:
    matchLabels:
      app: cloud-native-34
  endpoints:
    - port: http
      interval: 15s
      path: /actuator/prometheus
```



### jenkins pipeline

```
pipeline{
    agent none
    stages {
        stage('Clone Code') {
            agent {
                label 'master'
            }
            steps {
                echo "1.Git Clone Code"
                sh 'curl "http://p.nju.edu.cn/portal_io/logout"'
                sh 'curl "http://p.nju.edu.cn/portal_io/login?username=xxx&password=xxx"'
                git url: "https://gitee.com/md2002/cloud-native-2023.git"
             # 设定了github仓库的地址，由于服务器无法访问外网，因此配置了github仓库与gitee仓库的同步
            }
        }

        stage('Maven Build') {
            agent {
                docker {
                    image 'maven:latest'
                    args ' -v /home/nju34:/home/nju34'
                }
            }
            steps {
                echo "2.Maven Build Stage"
                sh 'mvn clean install package \'-Dmaven.test.skip=true\''
            }
        }

        stage('Image Build') {
            agent {
                label 'master'
            }
            steps {
                echo "3.Image Build Stage"
                sh 'docker build -f Dockerfile --build-arg jar_name=target/hello-0.0.1-SNAPSHOT.jar -t 34_images:${BUILD_ID} .'
                sh 'docker tag 34_images:${BUILD_ID} harbor.edu.cn/nju34/34_images:${BUILD_ID}'
            }
        }

        stage('Push') {
            agent {
                label 'master'
            }
            steps {
                echo "4.Push Docker Image Stage"
                sh "docker login --username=nju34 harbor.edu.cn -p nju342023"
                sh 'docker push harbor.edu.cn/nju34/34_images:${BUILD_ID}'
            }
        }
    }
}
node('slave'){
    container('jnlp-kubectl'){
        stage('Clone YAML'){
            echo "5.Git Clone YAML to Slave"
            sh 'curl "http://p.nju.edu.cn/portal_io/logout"'
            sh 'curl "http://p.nju.edu.cn/portal_io/login?username=xxx&password=xxx"'
            git url: "https://gitee.com/md2002/cloud-native-2023.git"
        }
        stage('YAML'){
            echo"6.Change YAML File Stage"
            sh 'sed -i "s#{VERSION}#${BUILD_ID}#g"   deployment.yaml'
            sh 'sed -i "s#{VERSION}#${BUILD_ID}#g"   service.yaml'
        }
        stage('Deploy'){
            echo "7.Deploy To K8s Stage"
            sh "kubectl apply -f deployment.yaml -n nju34"
            sh "kubectl apply -f service.yaml -n nju34"
        }
    }
}
```

jenkins部署成功截图：

![image.png](https://pic2.58cdn.com.cn/nowater/webim/big/n_v2a01bac30113e43a9977ea0cb6d711736.png)

使用kubectl查看部署情况并使用curl命令测试：

![image.png](https://pic7.58cdn.com.cn/nowater/webim/big/n_v27ac6eb63e14d460cbd085e901cc84b6e.png)



## 扩容场景

### prometheus查看监控指标

创建servicemonitor对象后通过服务发现机制就能自动开始监控服务信息了：

![image.png](https://pic5.58cdn.com.cn/nowater/webim/big/n_v248782691a33e41ca899fa00da688ce30.png)

### Grafana 监控大屏

![image.png](https://pic8.58cdn.com.cn/nowater/webim/big/n_v2aa1dc9308af0479aaab97fe04c80ac50.png)

