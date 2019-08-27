package com.westlake.air.propro.config;

import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.service.UserService;
import com.westlake.air.propro.utils.JWTUtil;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

public class ShiroRealm extends AuthorizingRealm {


    // 装载 对 user 表操作的 class
    @Autowired
    private UserService userService;


    /**
     * 必须重写此方法，不然会报错
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JWTToken;
    }


    // /**
    //  * 默认使用此方法进行用户名正确与否验证，错误抛出异常即可。
    //  */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {

        System.out.println(">执行 doGetAuthenticationInfo 身份认证");

        String token = (String) authenticationToken.getCredentials();

        // 解密获得 username 用于和数据库进行对比
        String username = JWTUtil.getUsername(token);

        if (null == username || !JWTUtil.verify(token, username)) {
            throw new AuthenticationException("token 认证失败");
        }


        UserDO userInfo = userService.getByUsername(username);
        if (userInfo == null) {
            // 抛出 AuthenticationException
            throw new AuthenticationException("Username or password error");
        }

        return new SimpleAuthenticationInfo(userInfo, token, getName());


    }

    /**
     * 只有当需要检测用户权限的时候才会调用此方法，例如checkRole,checkPermission之类的
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {

        System.out.println(">执行 doGetAuthorizationInfo 权限认证");
        // principals 传过来的是 token
        // System.out.println(">principals = "+principals);
        String username = JWTUtil.getUsername(principals.toString());

        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

        // 获取数据库中对应的用户
        UserDO userInfo = userService.getByUsername(username);

        // 如果查询为空 应该
        if (null == userInfo) {
            return info;
        }

        // 设置该用户拥有的角色和权限
        info.setRoles(userInfo.getRoles());

        info.setStringPermissions(userInfo.getPerms());

        return info;

    }


}
