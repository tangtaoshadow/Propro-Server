# PROPRO-SERVER-Token







# Java VM Params
 - [Optional]   -Xmx10000M 
 - [Required]   -Ddbpath=localhost:27017
 - [Required]   -Dadmin_username=Admin
 - [Required]   -Dadmin_password=propro
 - [Required]   -Dmultiple=1
 - [Required]   -Drepository=\\172.16.55.75\ProproNas\data\
 - [Required]   -Ddingtalk_robot=https://oapi.dingtalk.com/robot/send?access_token=f2fb029431f174e678106b30c2db5fb0e40e921999386a61031bf864f18beb77



## [PROPRO-SERVER](http://www.propro.club/login/login)

## [PROPRO-HOME](http://www.proteomics.pro/)

@Author：`唐涛`

# 登录页面

**创建**：`2019-8-3 14:06:48`

**修改**：`2019-8-3 14:00:38`

![propro](http://cdn.promiselee.cn/share_static/propro-login-20190803140517.png)



# 待完善的功能

- 标准库设置为public和private `2019-8-30 14:14:21`
- 标准库实现自定义排序，默认排序 `2019-8-30 14:14:06`









# 存在的bug



### 上传 GoldStandardLibrary.csv 标准库没有数据 

`2019-8-30 14:14:42`

还原现象

上传，上传成功没有报错，查看数据是0，点击生成伪肽段出现404错误

```bash
2019-08-30 14:17:56.540  INFO 34200 --- [p-nio-80-exec-4] c.w.a.propro.controller.DecoyController  : 正在删除原有伪肽段
2019-08-30 14:17:56.543  INFO 34200 --- [p-nio-80-exec-4] c.w.a.propro.controller.DecoyController  : 原有伪肽段删除完毕
2019-08-30 14:17:56.545  INFO 34200 --- [p-nio-80-exec-4] c.w.a.p.algorithm.decoy.BaseGenerator    : 伪肽段生成完毕,总计:0个
2019-08-30 14:17:56.548 ERROR 34200 --- [p-nio-80-exec-4] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed; nested exception is java.lang.IllegalArgumentException: state should be: writes is not an empty list] with root cause

java.lang.IllegalArgumentException: state should be: writes is not an empty list
	at com.mongodb.assertions.Assertions.isTrueArgument(Assertions.java:99) ~[mongo-java-driver-3.10.2.jar:na]
	at com.mongodb.operation.MixedBulkWriteOperation.<init>(MixedBulkWriteOperation.java:111) ~[mongo-java-driver-3.10.2.jar:na]
	at com.mongodb.internal.operation.Operations.bulkWrite(Operations.java:399) ~[mongo-java-driver-3.10.2.jar:na]
	at com.mongodb.internal.operation.SyncOperations.bulkWrite(SyncOperations.java:183) ~[mongo-java-driver-3.10.2.jar:na]
	at com.mongodb.client.internal.MongoCollectionImpl.executeBulkWrite(MongoCollectionImpl.java:468) ~[mongo-java-driver-3.10.2.jar:na]
	at com.mongodb.client.internal.MongoCollectionImpl.bulkWrite(MongoCollectionImpl.java:448) ~[mongo-java-driver-3.10.2.jar:na]
	at org.springframework.data.mongodb.core.DefaultBulkOperations.lambda$execute$0(DefaultBulkOperations.java:279) ~[spring-data-mongodb-2.1.9.RELEASE.jar:2.1.9.RELEASE]
	at org.springframework.data.mongodb.core.MongoTemplate.execute(MongoTemplate.java:545) ~[spring-data-mongodb-2.1.9.RELEASE.jar:2.1.9.RELEASE]
	at org.springframework.data.mongodb.core.DefaultBulkOperations.execute(DefaultBulkOperations.java:278) ~[spring-data-mongodb-2.1.9.RELEASE.jar:2.1.9.RELEASE]
	at com.westlake.air.propro.dao.PeptideDAO.updateDecoyInfos(PeptideDAO.java:142) ~[classes/:na]
	at com.westlake.air.propro.service.impl.PeptideServiceImpl.updateDecoyInfos(PeptideServiceImpl.java:145) ~[classes/:na]
	at com.westlake.air.propro.controller.DecoyController.generate(DecoyController.java:87) ~[classes/:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:na]
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:567) ~[na:na]
	at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:190) ~[spring-web-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:138) ~[spring-web-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:104) ~[spring-webmvc-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:892) ~[spring-webmvc-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:797) ~[spring-webmvc-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1039) ~[spring-webmvc-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:942) ~[spring-webmvc-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1005) ~[spring-webmvc-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:897) ~[spring-webmvc-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:634) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:882) ~[spring-webmvc-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:741) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53) ~[tomcat-embed-websocket-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.springframework.boot.actuate.web.trace.servlet.HttpTraceFilter.doFilterInternal(HttpTraceFilter.java:88) ~[spring-boot-actuator-2.1.6.RELEASE.jar:2.1.6.RELEASE]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:109) ~[spring-web-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.shiro.web.servlet.ProxiedFilterChain.doFilter(ProxiedFilterChain.java:61) ~[shiro-web-1.4.1.jar:1.4.1]
	at org.apache.shiro.web.servlet.AdviceFilter.executeChain(AdviceFilter.java:108) ~[shiro-web-1.4.1.jar:1.4.1]
	at org.apache.shiro.web.servlet.AdviceFilter.doFilterInternal(AdviceFilter.java:137) ~[shiro-web-1.4.1.jar:1.4.1]
	at org.apache.shiro.web.servlet.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:125) ~[shiro-web-1.4.1.jar:1.4.1]
	at org.apache.shiro.web.servlet.ProxiedFilterChain.doFilter(ProxiedFilterChain.java:66) ~[shiro-web-1.4.1.jar:1.4.1]
	at org.apache.shiro.web.servlet.AbstractShiroFilter.executeChain(AbstractShiroFilter.java:449) ~[shiro-web-1.4.1.jar:1.4.1]
	at org.apache.shiro.web.servlet.AbstractShiroFilter$1.call(AbstractShiroFilter.java:365) ~[shiro-web-1.4.1.jar:1.4.1]
	at org.apache.shiro.subject.support.SubjectCallable.doCall(SubjectCallable.java:90) ~[shiro-core-1.4.1.jar:1.4.1]
	at org.apache.shiro.subject.support.SubjectCallable.call(SubjectCallable.java:83) ~[shiro-core-1.4.1.jar:1.4.1]
	at org.apache.shiro.subject.support.DelegatingSubject.execute(DelegatingSubject.java:387) ~[shiro-core-1.4.1.jar:1.4.1]
	at org.apache.shiro.web.servlet.AbstractShiroFilter.doFilterInternal(AbstractShiroFilter.java:362) ~[shiro-web-1.4.1.jar:1.4.1]
	at org.apache.shiro.web.servlet.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:125) ~[shiro-web-1.4.1.jar:1.4.1]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:99) ~[spring-web-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:109) ~[spring-web-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:92) ~[spring-web-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:109) ~[spring-web-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.springframework.web.filter.HiddenHttpMethodFilter.doFilterInternal(HiddenHttpMethodFilter.java:93) ~[spring-web-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:109) ~[spring-web-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter.filterAndRecordMetrics(WebMvcMetricsFilter.java:114) ~[spring-boot-actuator-2.1.6.RELEASE.jar:2.1.6.RELEASE]
	at org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter.doFilterInternal(WebMvcMetricsFilter.java:104) ~[spring-boot-actuator-2.1.6.RELEASE.jar:2.1.6.RELEASE]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:109) ~[spring-web-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:200) ~[spring-web-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:109) ~[spring-web-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:202) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:96) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:490) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:139) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:343) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:408) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:66) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:853) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1587) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128) ~[na:na]
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628) ~[na:na]
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
	at java.base/java.lang.Thread.run(Thread.java:835) ~[na:na]


```









