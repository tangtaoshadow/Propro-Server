package com.westlake.air.propro.config;

import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {


    // 创建 ShiroFilterFactoryBean
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager) {
        System.out.println(">执行 shiroFilterFactoryBean");

        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();

        // 默认拦截的 网址
        shiroFilterFactoryBean.setLoginUrl("/login/login");

        shiroFilterFactoryBean.setSuccessUrl("/index");

        // 设置安全管理器 安全管理器会从 DefaultWebSecurityManager 传入
        shiroFilterFactoryBean.setSecurityManager(securityManager);

        // 设置未授权的默认地址
        shiroFilterFactoryBean.setUnauthorizedUrl("/login/login");


        // 添加自定义的过滤器并且取名为 jwt
        Map<String, Filter> filterMap = new LinkedHashMap<>();
        // 设置自定义的 JWT 过滤器
        filterMap.put("jwt", new JWTFilter());

        shiroFilterFactoryBean.setFilters(filterMap);

        // 加入要拦截的map
        shiroFilterFactoryBean.setFilterChainDefinitionMap(shiroFilterMap());

        return shiroFilterFactoryBean;

    }


    /***
     * @UpdateAuthor TangTao
     * @Change 关闭了之前shiro配置的 setCacheManager  setRememberMeManager
     *                  启用 jwt 所以关闭了 shiro 的 session
     * @UpdateTime 2019-8-7 01:51:39
     * @param           shiroRealm
     * @return DefaultWebSecurityManager
     * @Achieve web应用管理配置 注入 DefaultWebSecurityManager
     */
    @Bean
    //  CacheManager cacheManager
    public DefaultWebSecurityManager securityManager(Realm shiroRealm, CacheManager cacheManager) {

        System.out.println(">执行 securityManager");


        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();


        securityManager.setCacheManager(cacheManager);

        // 记住 Cookie
        // securityManager.setRememberMeManager(manager);

        // 关联 shiroRealm
        securityManager.setRealm(shiroRealm);

        // System.out.println(">Shiro拦截器工厂类注入成功");

        /*
         * 关闭shiro自带的 session
         * http://shiro.apache.org/session-management.html#SessionManagement-StatelessApplications%28Sessionless%29
         */
        DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();

        DefaultSessionStorageEvaluator defaultSessionStorageEvaluator = new DefaultSessionStorageEvaluator();

        defaultSessionStorageEvaluator.setSessionStorageEnabled(false);

        subjectDAO.setSessionStorageEvaluator(defaultSessionStorageEvaluator);

        securityManager.setSubjectDAO(subjectDAO);

        return securityManager;
    }


    /***
     * 配置它的作用在于 不用每次都去数据库查询
     * 通过 token 得出 username 然后就直接知道了角色
     * @return
     */
    @Bean
    public CacheManager cacheManager() {

        // System.out.println(">执行 cacheManager");
        // 使用内存作为缓存
        MemoryConstrainedCacheManager cacheManager = new MemoryConstrainedCacheManager();
        return cacheManager;
    }


    //配置realm，用于认证和授权
    @Bean
    public AuthorizingRealm shiroRealm() {

        // System.out.println(">执行 shiroRealm");
        ShiroRealm shiroRealm = new ShiroRealm();
        return shiroRealm;
    }


    // //启用shiro方言，这样能在页面上使用shiro标签
    // @Bean
    // public ShiroDialect shiroDialect() {
    //     return new ShiroDialect();
    // }


    /**
     * 启用shiro注解
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor getAuthorizationAttributeSourceAdvisor(org.apache.shiro.mgt.SecurityManager securityManager) {

        System.out.println(">执行 getAuthorizationAttributeSourceAdvisor");

        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }


    @Bean
    public DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator() {
        System.out.println(">执行 advisorAutoProxyCreator");

        DefaultAdvisorAutoProxyCreator advisorAutoProxyCreator = new DefaultAdvisorAutoProxyCreator();
        advisorAutoProxyCreator.setProxyTargetClass(true);

        return advisorAutoProxyCreator;

    }


    // 设置要拦截的路径
    private Map<String, String> shiroFilterMap() {

        // 设置路径映射， LinkedHashMap
        LinkedHashMap<String, String> filterChainDefinitionMap = new LinkedHashMap<>();

        // // 这些都是允许访问的 anon 无需认证
        // filterChainDefinitionMap.put("/assets/**", "anon");
        // filterChainDefinitionMap.put("/instances", "anon");
        // filterChainDefinitionMap.put("/actuator/**", "anon");
        // // /login/** 包括了 dologin
        // filterChainDefinitionMap.put("/login/**", "anon");
        // filterChainDefinitionMap.put("/test/**", "anon");
        //
        // // perms 授权过滤器
        // // filterChainDefinitionMap.put("/test/**", "perms[user:add]");
        //
        // // user 如果使用 remember 可以直接访问
        // filterChainDefinitionMap.put("/**", "user");

        // 注意 因为这里是 LinkedHashMap 所以要保持先后顺序
        filterChainDefinitionMap.put("/error", "anon");
        filterChainDefinitionMap.put("/login/**", "anon");
        // filterChainDefinitionMap.put("/login/test1", "admin");


        // 所有请求通过我们自己的JWT Filter
        filterChainDefinitionMap.put("/**", "jwt");
        // filterChainDefinitionMap.put("/login/test1", "roles[admin]");

        return filterChainDefinitionMap;

    }


}