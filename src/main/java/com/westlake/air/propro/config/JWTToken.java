package com.westlake.air.propro.config;

import org.apache.shiro.authc.AuthenticationToken;


/***
 * @Author          TangTao
 * @CreateTime      2019-7-22 00:39:08
 * @UpdateTime
 * @Achieve         实现 AuthenticationToken interface
 * @Copyright       西湖 PROPRO http://www.proteomics.pro/
 *
 */
public class JWTToken implements AuthenticationToken {

    private String token;

    public JWTToken(String token) {
        this.token = token;
    }


    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }


}