# 使用基础的Java 18镜像
FROM openjdk:18

# 设置工作目录
WORKDIR /app

# 将编译后的Spring Boot JAR文件复制到容器中
COPY ./target/hello-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
# 设置启动命令
CMD ["java", "-jar", "app.jar"]