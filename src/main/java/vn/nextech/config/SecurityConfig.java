package vn.nextech.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import vn.nextech.service.CustomerAccountService;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            @Value("${shop.admin.username}") String adminUsername,
            @Value("${shop.admin.password}") String adminPassword,
            PasswordEncoder encoder,
            CustomerAccountService customers) {
        // Giữ tài khoản ADMIN demo cũ; khách hàng đăng nhập bằng email đã đăng ký.
        String adminHash = encoder.encode(adminPassword);
        return username -> {
            if (adminUsername.equals(username)) {
                return User.withUsername(adminUsername)
                    .password(adminHash).roles("ADMIN").build();
            }
            return customers.findByEmail(username)
                .map(account -> User.withUsername(account.email())
                    .password(account.passwordHash())
                    .roles("CUSTOMER")
                    .disabled(!account.active())
                    .build())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        };
    }

    @Bean
    SecurityFilterChain filterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http)
            throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/quan-tri", "/quan-tri/**").hasRole("ADMIN")
                    .requestMatchers("/tai-khoan").hasRole("CUSTOMER")
                    .anyRequest().permitAll())
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, ex) -> {
                    boolean customerPage = request.getRequestURI().equals(request.getContextPath() + "/tai-khoan");
                    response.sendRedirect(request.getContextPath() +
                        (customerPage ? "/tai-khoan/dang-nhap" : "/dang-nhap"));
                }))
                .formLogin(login -> login.loginPage("/dang-nhap")
                    .loginProcessingUrl("/dang-nhap")
                    .successHandler((request, response, authentication) -> {
                        boolean admin = authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                        response.sendRedirect(request.getContextPath() + (admin ? "/quan-tri" : "/tai-khoan"));
                    })
                    .failureHandler((request, response, ex) -> {
                        boolean customer = "customer".equals(request.getParameter("loginType"));
                        response.sendRedirect(request.getContextPath() +
                            (customer ? "/tai-khoan/dang-nhap?error" : "/dang-nhap?error"));
                    }).permitAll())
                .logout(logout -> logout.logoutUrl("/dang-xuat").logoutSuccessUrl("/").permitAll())
                .build();
    }
}
