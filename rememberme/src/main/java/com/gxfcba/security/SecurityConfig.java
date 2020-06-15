package com.gxfcba.security;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.Resource;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Resource
    private UserDetailsService userDetailsService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.authorizeRequests()
                .mvcMatchers("/home/**").hasRole("ADMIN")
                .anyRequest().permitAll()
                .and()

                .formLogin()
                .defaultSuccessUrl("/home")
                .loginPage("/login")
                .permitAll()
                .and()

                .rememberMe()
                .rememberMeParameter("rememberme")
                .key("uniqueAndSecret")
                .userDetailsService(userDetailsService)
                .tokenValiditySeconds(864000);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}
