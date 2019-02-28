package com.nat.cloudstorage.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;


import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Autowired
    private RESTAuthenticationEntryPoint authenticationEntryPoint;
    @Autowired
    private RESTAuthenticationFailureHandler authenticationFailureHandler;
    @Autowired
    private RESTAuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    private DataSource dataSource;

    @Value("${spring.queries.users-query}")
    private String usersQuery;

    @Value("${spring.queries.roles-query}")
    private String rolesQuery;


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        logger.debug("configure");
        // http.authorizeRequests().antMatchers("/login").permitAll();
        http.authorizeRequests().antMatchers("/loginform").authenticated();
        http.authorizeRequests().antMatchers("/signup").permitAll();

        http.authorizeRequests().antMatchers("/listfiles").authenticated();
        http.authorizeRequests().antMatchers("/upload").authenticated();
        http.authorizeRequests().antMatchers("/addcloud").authenticated();
        http.authorizeRequests().antMatchers("/getclouds").authenticated();
        http.authorizeRequests().antMatchers("/downloadFile").authenticated();
        http.authorizeRequests().antMatchers("/deletefile").authenticated();
        http.authorizeRequests().antMatchers("/addfolder").authenticated();
        http.authorizeRequests().antMatchers("/renamefile").authenticated();
        http.authorizeRequests().antMatchers("/getcloudstree").authenticated();
        http.authorizeRequests().antMatchers("/copy").authenticated();
        http.authorizeRequests().antMatchers("/removecloud").authenticated();

        http.csrf().disable();

        http.exceptionHandling().authenticationEntryPoint(authenticationEntryPoint);

        http.formLogin().loginPage("/loginform")
                .usernameParameter("email").passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .failureHandler(authenticationFailureHandler);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth)
            throws Exception {
        auth.
                jdbcAuthentication()
                .passwordEncoder(passwordEncoder())
                .usersByUsernameQuery(usersQuery)
                .authoritiesByUsernameQuery(rolesQuery)
                .dataSource(dataSource);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web
                .ignoring()
                .antMatchers("/resources/**", "/static/**", "/css/**", "/js/**", "/images/**", "/index.html");
    }
}
