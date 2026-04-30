package com.haui.tech_shop.configurations;

import com.haui.tech_shop.services.Impl.CustomUserDetailsServiceImpl;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

    CustomUserDetailsServiceImpl customUserDetailsService;
    CustomAuthFailureHandler customAuthFailureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                // Tránh lỗi redirect loop
                .requestCache(requestCache -> requestCache.disable())

                .authorizeHttpRequests(request -> request
                        // Static resources
                        .requestMatchers("/user/assets/**").permitAll()
                        .requestMatchers("/user/customize/**").permitAll()
                        .requestMatchers("/admin/assets/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        // Public pages
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/register").permitAll()
                        .requestMatchers("/user/home").permitAll()
                        .requestMatchers("/api/chat").permitAll()
                        .requestMatchers("/user/contact").permitAll()
                        .requestMatchers("/user/about-us").permitAll()
                        .requestMatchers("/forgot-password").permitAll()
                        .requestMatchers("/test/**").permitAll()
                        .requestMatchers("/verify-account").permitAll()
                        .requestMatchers("/user/products/**").permitAll()

                        // Role pages
                        .requestMatchers("/user/**").hasAnyRole("ADMIN", "USER", "MANAGER", "SHIPPER")
                        .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/shipper/**").hasAnyRole("ADMIN", "SHIPPER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(new CustomizeSuccessHandler())
                        .failureHandler(customAuthFailureHandler)
                        .permitAll()
                )

                // Ghi nhớ đăng nhập 30 ngày
                .rememberMe(remember -> remember
                        .key("tech-shop-remember-me-key")
                        .userDetailsService(customUserDetailsService)
                        .tokenValiditySeconds(60 * 60 * 24 * 30)
                        .rememberMeCookieName("TECH_SHOP_REMEMBER_ME")
                        .alwaysRemember(true)
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "TECH_SHOP_REMEMBER_ME")
                        .logoutSuccessUrl("/user/home?logout=true")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder.userDetailsService(customUserDetailsService);

        return authenticationManagerBuilder.build();
    }
}
