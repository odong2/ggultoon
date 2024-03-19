package com.architecture.admin.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Locale;

/*****************************************************
 * 언어셋 설정
 ****************************************************/
@Configuration
public class LangConfig implements WebMvcConfigurer {
    /**
     * 쿠키를 이용해서 Locale 정보를 저장
     */
    @Bean
    public LocaleResolver localeResolver() {

        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setDefaultLocale(Locale.getDefault());
        // Locale 정보 쿠키이름 설정
        resolver.setCookieName("lang");

        return resolver;
    }

    /**
     * 파라미터를 이용해서 Locale 정보를 저장
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        // Locale 정보 변경 가능 HttpMethod 설정 GET,POST,PUT...
        interceptor.setHttpMethods("GET");
        // Locale 정보 ParamName
        interceptor.setParamName("lang");

        return interceptor;
    }

    /**
     * 메세지 가져오기
     */
    @Bean
    public MessageSource messageSource() {
        // 지정한 시간마다 다시 리로드 하도록 한다.
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

        // 언어 리소스들이 있는 경로를 지정한다.
        messageSource.setBasenames(
                "classpath:/languages/common",
                "classpath:/languages/login",
                "classpath:/languages/member",
                "classpath:/languages/content",
                "classpath:/languages/episode",
                "classpath:/languages/coin",
                "classpath:/languages/payment",
                "classpath:/languages/purchase",
                "classpath:/languages/setting",
                "classpath:/languages/notification",
                "classpath:/languages/community",
                "classpath:/languages/board",
                "classpath:/languages/event"
        );

        // 기본 인코딩을 지정한다.
        messageSource.setDefaultEncoding("UTF-8");
        // 프로퍼티 파일의 변경을 감지할 시간 간격을 지정한다.(초)
        messageSource.setCacheSeconds(600);
        // 없는 메세지일 경우 예외를 발생시키는 대신 코드를 기본 메세지로 한다.
        messageSource.setUseCodeAsDefaultMessage(true);

        return messageSource;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
