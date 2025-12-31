package site.protoa.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

        @Override
        public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                                .allowedOriginPatterns(
                                                "http://localhost:3000",
                                                "http://localhost:3001",
                                                "http://localhost:4000",
                                                "http://localhost:8000",
                                                "http://127.0.0.1:3000",
                                                "http://127.0.0.1:3001",
                                                "http://127.0.0.1:4000",
                                                "http://127.0.0.1:8000")
                                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                                .allowedHeaders("*")
                                .exposedHeaders("*")
                                .allowCredentials(true)
                                .maxAge(3600);
        }

        @Bean
        public CorsFilter corsFilter() {
                CorsConfiguration corsConfig = new CorsConfiguration();

                // 허용할 Origin 목록
                // allowCredentials: true일 때는 와일드카드(*) 사용 불가
                // 로컬 개발 환경을 위한 포트 추가
                corsConfig.setAllowedOriginPatterns(Arrays.asList(
                                "http://localhost:3000",
                                "http://localhost:3001",
                                "http://localhost:4000",
                                "http://localhost:8000",
                                "http://127.0.0.1:3000",
                                "http://127.0.0.1:3001",
                                "http://127.0.0.1:4000",
                                "http://127.0.0.1:8000"));

                // 허용할 HTTP 메서드
                corsConfig.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

                // 허용할 헤더
                corsConfig.setAllowedHeaders(Arrays.asList("*"));

                // Credentials 허용
                corsConfig.setAllowCredentials(true);

                // 노출할 헤더
                corsConfig.setExposedHeaders(Arrays.asList("*"));

                // Preflight 요청 캐시 시간
                corsConfig.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", corsConfig);

                return new CorsFilter(source);
        }
}
