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
apiVersion: v1
kind: Deployment
metadata:
  name: project-group34
  namespace: nju34
spec:
  replicas: 2
  selector:
    matchLabels:
      app: cloud-native-34 #表示selector管理的容器标签
    template:
      metadata:
        labels:
          app: cloud-native-34 #项目标签
      spec:
        containers:
          - name: group34-containers
            image: 34_images #镜像名称，与jenkins的构建阶段镜像名称匹配
            imagePullPolicy: Never #表示只从本地已有镜像中选择

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

使用kubectl查看部署情况：

![image.png](https://pic1.58cdn.com.cn/nowater/webim/big/n_v293a59a59a3d64f0290bba6628f7eda69.png)

## 扩容场景

