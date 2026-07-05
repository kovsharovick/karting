package ru.vsu.cs.yesikov.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import ru.vsu.cs.yesikov.security.jwt.JwtAuthenticationFilter;
import ru.vsu.cs.yesikov.security.jwt.JwtUtils;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtUtils jwtUtils;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtils);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // API-эндпоинты для регистрации/входа – открыты
                        .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
                        // Статические ресурсы – открыты
                        .requestMatchers(new AntPathRequestMatcher("/static/**"),
                                new AntPathRequestMatcher("/webjars/**"),
                                new AntPathRequestMatcher("/h2-console/**")).permitAll()
                        // Все API-запросы (кроме /api/auth/…) требуют JWT
                        .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()
                        // ВСЕ ОСТАЛЬНЫЕ страницы (JSP) – свободны, проверка вручную в WebController
                        .anyRequest().permitAll()
                )
                // Отключаем стандартный formLogin (он нам не нужен, логин ручной)
                .formLogin(form -> form.disable())
                // Отключаем logout по умолчанию (тоже ручной)
                .logout(logout -> logout.disable())
                // Сессии создаются по необходимости (для JSP)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                // JWT-фильтр – только для /api, но он пропускает запросы без заголовка Authorization
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}