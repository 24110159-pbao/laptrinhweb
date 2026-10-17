package example.com.ltw.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disabled for simplicity of standard MVC form posts
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/home",
                    "/catalog/**",
                    "/product/**",
                    "/login",
                    "/register",
                    "/verify-otp",
                    "/resend-otp",
                    "/forgot-password",
                    "/reset-password",
                    "/logout",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/webjars/**",
                    "/favicon.ico"
                ).permitAll()
                .anyRequest().permitAll() // Allow public browse & standard session-based user checks
            )
            .formLogin(login -> login.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}
