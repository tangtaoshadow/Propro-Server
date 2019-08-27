package com.westlake.air.propro.utils;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.shiro.SecurityUtils;

import java.util.Date;


/***
 * @Author TangTao
 * @CreateTime 2019-7-22 00:37:20
 * @UpdateTime 2019-8-27 16:30:24
 * @Achieve 定义 JWT 生成 token 的规则 如:  有效时间 携带的用户信息 加密协议
 * @Copyright 西湖 PROPRO http://www.proteomics.pro/
 *
 */
public class JWTUtil {

    // 过期时间 4 小时
    private static final long EXPIRE_TIME = 4 * 60 * 60 * 1000;

    // 密钥
    private static final String SECRET = "propro-http://www.proteomics.pro/";

    /**
     * 登录时通过 loginController
     * 生成 token, 5min 后过期
     *
     * @param username 用户名
     * @return 加密的 token
     */
    public static String createToken(String username) {

        try {

            Date date = new Date(System.currentTimeMillis() + EXPIRE_TIME);

            Algorithm algorithm = Algorithm.HMAC512(SECRET);

            System.out.println(">algorithm " + algorithm);

            // 返回 token
            // 附带username信息
            return JWT.create()
                    .withClaim("username", username)
                    // 为了让每次 token 不一样 生成一个随机数 目的是产生更大的随机性 这个参数也可以不必要
                    .withClaim("propro", System.currentTimeMillis())
                    // 到期时间
                    .withExpiresAt(date)
                    // 创建一个新的JWT，并使用给定的算法进行标记
                    .sign(algorithm);

        } catch (Exception e) {

            return null;
        }

    }

    /**
     * 校验 token 是否正确
     *
     * @param token    密钥
     * @param username 用户名
     * @return 是否正确
     */
    public static boolean verify(String token, String username) {

        System.out.println(">执行 verify");

        System.out.println(SecurityUtils.getSubject().getPrincipal());


        try {

            Algorithm algorithm = Algorithm.HMAC512(SECRET);

            // 在token中附带了 username 信息
            JWTVerifier verifier = JWT.require(algorithm)
                    .withClaim("username", username)
                    .build();

            // 验证 token
            verifier.verify(token);

            return true;

        } catch (Exception exception) {
            // 出错就返回 false
            return false;
        }
    }

    /**
     * 根据token 获取 username
     * 获得token中的信息，无需 secret 解密也能获得
     *
     * @return token 中包含的 username
     */
    public static String getUsername(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("username").asString();
        } catch (JWTDecodeException e) {
            return null;
        }
    }

}
