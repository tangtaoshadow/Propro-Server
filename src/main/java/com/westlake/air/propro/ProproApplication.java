package com.westlake.air.propro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import java.util.Locale;

@SpringBootApplication
@EnableAsync
public class ProproApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ProproApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ProproApplication.class);
        springApplication.addListeners(new StartListener());
        springApplication.run(args);
//        SpringApplication.run(ProproApplication.class, args);
    }

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver localeResolver = new CookieLocaleResolver();
        localeResolver.setCookieName("Language");
        //设置默认区域
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        localeResolver.setCookieMaxAge(604800);//设置cookie有效期.
        return localeResolver;
//        String i18n_language = req.getParameter(I18N_LANGUAGE);
//        Locale locale = Locale.getDefault();
//        if(!StringUtils.isEmpty(i18n_language)) {
//            String[] language = i18n_language.split("_");
//            locale = new Locale(language[0], language[1]);
//
//            //将国际化语言保存到session
//            HttpSession session = req.getSession();
//            session.setAttribute(I18N_LANGUAGE_SESSION, locale);
//        }else {
//            //如果没有带国际化参数，则判断session有没有保存，有保存，则使用保存的，也就是之前设置的，避免之后的请求不带国际化参数造成语言显示不对
//            HttpSession session = req.getSession();
//            Locale localeInSession = (Locale) session.getAttribute(I18N_LANGUAGE_SESSION);
//            if(localeInSession != null) {
//                locale = localeInSession;
//            }
//        }
//        return locale;
    }
}
