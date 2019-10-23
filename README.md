# PROPRO-SERVER-Token



**CreateTime：**`2019-7-25 22:00:25`

**UpdateTime：**`2019-10-23 13:00:25`



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

@Author：`唐涛`   [Home](https://www.promiselee.cn/tao)

# 登录页面

**创建**：`2019-8-3 14:06:48`

**修改**：`2019-8-3 14:00:38`

![propro](http://cdn.promiselee.cn/share_static/propro-login-20190803140517.png)



# 分析概览

**作者：**`唐涛`

**创建**：`2019-9-18 20:10:39`

**修改**：`2019-9-18 20:18:54`

![](http://cdn.promiselee.cn/share_static/files/propro/propro-analysis-20190918104213.png)



# 分析详情

**作者：**`唐涛`  [Home](https://www.promiselee.cn/tao)

**创建**：`2019-9-18 20:11:59`

**修改**：`2019-9-18 20:12:32`

![](http://cdn.promiselee.cn/share_static/files/propro/propro-analysis-detail-20190918200624.png)



# 打分数据列表

**作者**：[`唐涛`](https://www.promiselee.cn/tao)

**创建**：`2019-9-28 21:34:05`

**修改**：`2019-9-28 21:34:09`

![](http://cdn.promiselee.cn/share_static/files/propro/propro-analysis-score-2019-9-28%20213140.png)



# 打分数据列表

**作者**：[`唐涛`](https://www.promiselee.cn/tao)

**创建**：`2019-9-28 21:34:05`

**修改**：`2019-9-28 21:34:09`

![](http://cdn.promiselee.cn/share_static/files/propro/propro-analysis-score-2019-9-28%20213140.png)



# 实验数据：列表

**作者**：[`唐涛`](https://www.promiselee.cn/tao)

**创建**：`2019-10-11 19:25:00`

**修改**：`2019-10-11 19:25:17`

![](http://cdn.promiselee.cn/share_static/files/propro/propro-experiment-list-20191011192358.png)



# 实验数据：详情

**作者**：[`唐涛`](https://www.promiselee.cn/tao)

**创建**：`2019-10-12 19:08:06`

**修改**：`2019-10-12 19:08:20`

![tangtao](http://cdn.promiselee.cn/share_static/files/propro/propro-experiment-detail-20191012191046.png)



# 实验数据：更新

**作者**：[`唐涛`](https://www.promiselee.cn/tao)

**创建**：`2019-10-13 02:50:21`

**修改**：`2019-10-13 02:51:44`

![tangtao](http://cdn.promiselee.cn/share_static/files/propro/propro-experiment-edit-20191013024934.png)



# 项目列表

**作者**：[`唐涛`](https://www.promiselee.cn/tao)

**创建**：`2019-10-16 12:11:21`

**修改**：`2019-10-16 12:11:58`

![tangtao](http://cdn.promiselee.cn/share_static/files/propro/propro-project-list-20191016121045.png)







# 项目列表：更新

**作者**：[`唐涛`](https://www.promiselee.cn/tao)

**创建**：`2019-10-23 12:56:34`

**修改**：`2019-10-23 12:59:18`

![tangtao](http://cdn.promiselee.cn/share_static/files/propro/propro-project-modify-20191023125508.png)





# 待完善的功能

- 标准库设置为public和private `2019-8-30 14:14:21`
- 标准库实现自定义排序，默认排序 `2019-8-30 14:14:06`
-  ` PermissionUtil.check(temp);` 异常处理

```java
package com.westlake.air.propro.controller;

 /***
     * @Archive 查询指定id的肽段列表
     * @param libraryId 查询的id
     * @param proteinName
     * @param peptideRef
     * @param sequence
     * @param currentPage 当前页面
     * @param uniqueFilter
     * @param pageSize 页大小
     * @return
     */
    @PostMapping(value = "/list")
    String peptideList(
            @RequestParam(value = "libraryId") String libraryId,
            @RequestParam(value = "proteinName", required = false) String proteinName,
            @RequestParam(value = "peptideRef", required = false) String peptideRef,
            @RequestParam(value = "sequence", required = false) String sequence,
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "uniqueFilter", required = false, defaultValue = "All") String uniqueFilter,
            @RequestParam(value = "pageSize", required = false, defaultValue = "1000") Integer pageSize) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            long startTime = System.currentTimeMillis();
            LibraryDO temp = libraryService.getById(libraryId);
            PermissionUtil.check(temp);
            // 把库的信息也发送回去
            data.put("libraryInfo", temp);
            data.put("proteinName", proteinName);
            data.put("peptideRef", peptideRef);
            data.put("pageSize", pageSize);
            data.put("uniqueFilter", uniqueFilter);
            data.put("libraries", getLibraryList(null, true));
            data.put("sequence", sequence);

            PeptideQuery query = new PeptideQuery();

            if (libraryId != null && !libraryId.isEmpty()) {
                query.setLibraryId(libraryId);
            }
            if (peptideRef != null && !peptideRef.isEmpty()) {
                query.setPeptideRef(peptideRef);
            }

            if (proteinName != null && !proteinName.isEmpty()) {
                query.setProteinName(proteinName);
            }

            if (sequence != null && !sequence.isEmpty()) {
                query.setSequence(sequence);
            }

            if (!uniqueFilter.equals("All")) {
                if (uniqueFilter.equals("Yes")) {
                    query.setIsUnique(true);
                } else if (uniqueFilter.equals("No")) {
                    query.setIsUnique(false);
                }
            }

            query.setPageSize(pageSize);
            query.setPageNo(currentPage);
            ResultDO<List<PeptideDO>> resultDO = peptideService.getList(query);

            data.put("peptideList", resultDO.getModel());
            data.put("totalPage", resultDO.getTotalPage());
            data.put("currentPage", currentPage);
            // 搜索用时
            data.put("searchTime", System.currentTimeMillis() - startTime);
            // 搜索结果共计
            data.put("searchNumbers", resultDO.getTotalNum());
            data.put("pageSize", pageSize);
            status = 0;
        } while (false);

        map.put("status", status);
        // 将数据再打包一次 简化前端数据处理逻辑
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }

```

类似有 ` PermissionUtil.check(temp);` 的函数并没有处理报错的情况，而是继续向下执行或者直接终止,这里的异常要重新定义并且捕获

```java

    public static void check(LibraryDO library) throws UnauthorizedAccessException {
        UserDO user = getCurrentUser();
        if(library == null || user == null){
            throw new UnauthorizedAccessException("redirect:/library/list");
        }

        if(library.isDoPublic()){
            return;
        }

        if(user.getRoles().contains(Roles.ROLE_ADMIN)){
            return;
        }

        if (!library.getCreator().equals(user.getUsername())) {
            if(library.getType().equals(Constants.LIBRARY_TYPE_IRT)){
                throw new UnauthorizedAccessException("redirect:/library/listIrt");
            }
            throw new UnauthorizedAccessException("redirect:/library/list");
        } else {
            return;
        }
    }
```









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



### 如果 `peptideList` 为空，这里就会报出错误

```java
package com.westlake.air.propro.dao;


public void updateDecoyInfos(List<PeptideDO> peptideList){
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, PeptideDO.class);
        for (PeptideDO peptide : peptideList) {

            Query query = new Query();
            query.addCriteria(Criteria.where("id").is(peptide.getId()));
            Update update = new Update();
            update.set("decoySequence", peptide.getDecoySequence());
            update.set("decoyUnimodMap", peptide.getDecoyUnimodMap());
            update.set("decoyFragmentMap", peptide.getDecoyFragmentMap());

            ops.updateOne(query, update);
        }
        ops.execute();
    }
```

错误情况

```bash
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
	at com.westlake.air.propro.service.impl.PeptideServiceImpl.updateDecoyInfos(PeptideServiceImpl.java:144) ~[classes/:na]
	at com.westlake.air.propro.controller.DecoyController.generate(DecoyController.java:102) ~[classes/:na]
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
	at org.springframework.web.servlet.FrameworkServlet.doPost(FrameworkServlet.java:908) ~[spring-webmvc-5.1.8.RELEASE.jar:5.1.8.RELEASE]
	at javax.servlet.http.HttpServlet.service(HttpServlet.java:660) ~[tomcat-embed-core-9.0.21.jar:9.0.21]
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



## 不允许json 格式化

**Author：**`唐涛`

**创建**：`2019-9-28 21:53:33`

**修改**：`2019-9-28 21:55:32`

**path：** `com/westlake/air/propro/domain/db/AnalyseDataDO.java`

```java
package com.westlake.air.propro.domain.db;

import com.alibaba.fastjson.annotation.JSONField;
import com.westlake.air.propro.domain.BaseDO;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 15:48
 */
@Data
@Document(collection = "analyseData")
public class AnalyseDataDO extends BaseDO {

    //鉴定成功
    public static Integer IDENTIFIED_STATUS_SUCCESS = 0;
    //不满足基础条件
    public static Integer IDENTIFIED_STATUS_NO_FIT = 1;
    //鉴定未成功
    public static Integer IDENTIFIED_STATUS_UNKNOWN = 2;
    //尚未鉴定
    public static Integer IDENTIFIED_STATUS_NOT_START = 3;

    @Id
    // @JSONField(serialize = false)
            String id;

    @Indexed
    // @JSONField(serialize = false)
            String overviewId;

    @Indexed
    String peptideRef;

    //是否是伪肽段
    @Indexed
    Boolean isDecoy = false;

    @Indexed
    String dataRef;

    //打分相关的字段
    @Indexed
    // @JSONField(serialize=false)
            int identifiedStatus = IDENTIFIED_STATUS_NOT_START;

    //最终给出的FDR打分
    @Indexed
    // @JSONField(serialize = false)
            Double fdr;

    //最终给出的qValue
    @Indexed
    // @JSONField(serialize = false)
            Double qValue;

    String proteinName;

    Boolean isUnique;

    //该肽段片段的理论rt值,从标准库中冗余所得
    Float rt;

    //该肽段的前体mz,从标准库中冗余所得
    Float mz;

    //对应的标准库的peptideId
    // @JSONField(serialize = false)
    String peptideId;

    //key为cutInfo, value为对应的mz
    HashMap<String, Float> mzMap = new HashMap<>();

    // @JSONField(serialize = false)
    List<FeatureScores> featureScoresList;

    //最终选出的最佳峰
    // @JSONField(serialize = false)
    Double bestRt;

    // @JSONField(serialize = false)
    Double intensitySum;

    //最终的定量值
    // @JSONField(serialize = false)
    String fragIntFeature;

    //*******************非数据库字段*******************************
    //排序后的rt,仅在解压缩的时候使用,不存入数据库
    @JSONField(serialize = false)
    @Transient
    Float[] rtArray;

    //key为cutInfo, value为对应的intensity值,仅在解压缩的时候使用,不存入数据库
    @JSONField(serialize = false)
    @Transient
    HashMap<String, Float[]> intensityMap = new HashMap<>();
}

```



### 页面响应很慢

**创建：**`2019-9-29 15:53:04`

**存在的问题：**前端发起请求时，即使只查询10条数据，也要很长时间才能响应

```java
package com.westlake.air.propro.controller;

@PostMapping(value = "/resultList")
    String resultList(
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(value = "overviewId") String overviewId,
            @RequestParam(value = "peptideRef", required = false) String peptideRef,
            @RequestParam(value = "proteinName", required = false) String proteinName,
            @RequestParam(value = "isIdentified", required = false) String isIdentified) {

        Map<String, Object> map = new HashMap<String, Object>();

        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            data.put("overviewId", overviewId);
            data.put("proteinName", proteinName);
            data.put("peptideRef", peptideRef);
            data.put("pageSize", pageSize);
            data.put("isIdentified", isIdentified);

            ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
            PermissionUtil.check(overviewResult.getModel());

            AnalyseDataQuery query = new AnalyseDataQuery();
            query.setIsDecoy(false);

            if (peptideRef != null && !peptideRef.isEmpty()) {
                query.setPeptideRef(peptideRef);
            }

            if (proteinName != null && !proteinName.isEmpty()) {
                query.setProteinName(proteinName);
            }

            if (isIdentified != null && isIdentified.equals("Yes")) {
                query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS);
            } else if (isIdentified != null && isIdentified.equals("No")) {
                query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_NO_FIT);
                query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_UNKNOWN);
            }
            query.setOverviewId(overviewId);

            List<AnalyseDataDO> analyseDataDOList = analyseDataService.getAll(query);

            if (peptideRef != null && peptideRef.length() > 0) {
                query.setPeptideRef(null);
                query.setProteinName(analyseDataDOList.get(0).getProteinName());
                analyseDataDOList = analyseDataService.getAll(query);
            }

            HashMap<String, List<AnalyseDataDO>> proteinMap = new HashMap<>();
            for (AnalyseDataDO dataDO : analyseDataDOList) {

                if (proteinMap.containsKey(dataDO.getProteinName())) {
                    proteinMap.get(dataDO.getProteinName()).add(dataDO);
                } else {
                    List<AnalyseDataDO> newList = new ArrayList<>();
                    newList.add(dataDO);
                    proteinMap.put(dataDO.getProteinName(), newList);
                }
            }

            List<String> protList = new ArrayList<>(proteinMap.keySet());
            int totalPage = (int) Math.ceil(protList.size() / (double) pageSize);
            HashMap<String, List<AnalyseDataDO>> pageProtMap = new HashMap<>();

            for (int i = pageSize * (currentPage - 1); i < protList.size() && i < pageSize * currentPage; i++) {
                pageProtMap.put(protList.get(i), proteinMap.get(protList.get(i)));
            }

            data.put("protMap", pageProtMap);
            data.put("overview", overviewResult.getModel());
            data.put("totalPage", totalPage);
            data.put("currentPage", currentPage);
            data.put("totalNum", protList.size());

            // 成功返回数据
            status = 0;

        } while (false);

        map.put("status", status);

        // 将数据再打包一次 简化前端数据处理逻辑
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);
    }

```