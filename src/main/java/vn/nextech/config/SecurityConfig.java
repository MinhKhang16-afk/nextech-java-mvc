package vn.nextech.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService userDetailsService(
            @Value("${shop.admin.username}") String username,
            @Value("${shop.admin.password}") String password,
            PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(encoder.encode(password)).roles("ADMIN").build());
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/quan-tri", "/quan-tri/**").hasRole("ADMIN")
                    .anyRequest().permitAll())
                .formLogin(login -> login.loginPage("/dang-nhap")
                    .loginProcessingUrl("/dang-nhap")
                    .defaultSuccessUrl("/quan-tri", true)
                    .failureUrl("/dang-nhap?error").permitAll())
                .logout(logout -> logout.logoutUrl("/dang-xuat").logoutSuccessUrl("/").permitAll())
                .build();
    }
}
