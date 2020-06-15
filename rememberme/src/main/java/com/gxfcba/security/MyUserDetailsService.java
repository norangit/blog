package com.gxfcba.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Value("${spring.security.user.password}")
    private String defaultPassword;

    @Value("${spring.security.user.name}")
    private String defaultAdmin;

    @Resource
    private PasswordEncoder passwordEncoder;

    private String encodedPassword;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        List <GrantedAuthority> authorities = new ArrayList <>();

        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        authorities.add(new SimpleGrantedAuthority("ENDPOINT_ADMIN"));

        //为了省事，不建数据库了，直接使用配置文件，入股有数据库的话，这个加密的密码是从数据库读出来的
        if(encodedPassword == null){
            encodedPassword = passwordEncoder.encode(defaultPassword);
        }

        User user = new User(defaultAdmin, encodedPassword, true, true, true, true, authorities);

        return user;
    }
}
