package com.architecture.admin.config;

import com.architecture.admin.config.interceptor.LoginCheckInterceptor;
import com.architecture.admin.libraries.TelegramLibrary;
import com.architecture.admin.libraries.filter.Filter;
import com.architecture.admin.libraries.filter.IpCheckFilterLibrary;
import com.architecture.admin.libraries.filter.LogFilterLibrary;
import com.architecture.admin.libraries.filter.LoginApiCheckFilterLibrary;
import com.architecture.admin.config.interceptor.JwtInterceptor;
import com.architecture.admin.libraries.jwt.JwtLibrary;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*****************************************************
 * 필터 등록
 ****************************************************/
@Configuration
public class WebConfig implements WebMvcConfigurer {


    @Bean
    public CookieSameSiteSupplier cookieSameSiteSupplier() {
        return CookieSameSiteSupplier.ofNone();
    }

    //API 접근 시 CORS 방지
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 요청을 허용할 출처 목록을 작성한다.
                .allowedOrigins(
                        "https://webtoon-admin.devlabs.co.kr:18080" //개발 - 관리자
                        ,"https://toon-www.devlabs.co.kr"           //개발 - 사용자
                        ,"http://localhost:3000"                    // FE 로컬
                        ,"http://localhost:18081"                   // 로컬
                        ,"https://toon-www.uxp.team"                // 개발웹서버
                        ,"https://www.gtdev.kr"                     // 스테이지서버 PC
                        ,"https://m.gtdev.kr"                       // 스테이지서버 모바일
                        ,"https://www.ggultoons.com"                // 운영서버 PC
                        ,"https://m.ggultoons.com"                  // 운영서버 모바일
                        ,"https://webtoon-front.devlabs.co.kr"      // 글로벌 페이지- dev
                        ,"https://front.gtdev.kr"                   // 글로벌 페이지- stg
                        ,"https://front.ggultoons.com"              // 글로벌 페이지- prd
                )
                .allowedHeaders("*") // 어떤 헤더들을 허용할 것인지
                .allowedMethods("*") // 어떤 메서드를 허용할 것인지 (GET, POST...)
                .allowCredentials(true); // 쿠키 요청을 허용한다(다른 도메인 서버에 인증하는 경우에만 사용해야하며, true 설정시 보안상 이슈가 발생할 수 있다)
    }


    @Bean
    public FilterRegistrationBean<Filter> logFilter() {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new LogFilterLibrary()); // 등록 할 필터를 지정
        filterRegistrationBean.setOrder(1);  // 순서가 낮을수록 먼저 동작한다.
        filterRegistrationBean.addUrlPatterns("/*"); // 필터를 적용할 URL 패턴을 지정

        return filterRegistrationBean;
    }

    // IP 체크 필터 - 허용 IP 이외 차단
    /*
    @Bean
    public FilterRegistrationBean<Filter> IpCheckFilter() {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new IpCheckFilterLibrary());
        filterRegistrationBean.setOrder(2);
        filterRegistrationBean.addUrlPatterns("/*");

        return filterRegistrationBean;
    }
    */


    @Override
    public void addInterceptors(InterceptorRegistry reg){
        reg.addInterceptor(jwtInterceptor())
                .order(2)
                .addPathPatterns("/**"); //interceptor 작업이 필요한 path를 모두 추가한다.
        //.excludePathPatterns("app/accounts","/app/accounts/auth","app/videos/**");
        // 인가작업에서 제외할 API 경로를 따로 추가할수도 있으나, 일일히 따로 추가하기 어려우므로 어노테이션을 따로 만들어 해결한다.
        reg.addInterceptor(loginCheckInterceptor())
                .order(1)
                .addPathPatterns("/**");
    }


    @Bean
    public JwtInterceptor jwtInterceptor() {
        JwtLibrary jwtLibrary = new JwtLibrary();
        TelegramLibrary telegramLibrary = new TelegramLibrary();
        return new JwtInterceptor(jwtLibrary,telegramLibrary);
    }

    @Bean
    public LoginCheckInterceptor loginCheckInterceptor() {
        TelegramLibrary telegramLibrary = new TelegramLibrary();
        return new LoginCheckInterceptor(telegramLibrary);
    }
}
