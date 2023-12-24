package com.example.security;

import com.example.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final UserService userService;
    private final JwtFilter jwtFilter;

    @Autowired
    public SecurityConfig(UserService userService, JwtFilter jwtFilter) {
        this.userService = userService;
        this.jwtFilter = jwtFilter;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(this.userService);
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .cors()
                .and()
                .csrf()
                .disable()
                .authorizeRequests()
                .antMatchers("/auth").permitAll()
                .antMatchers(HttpMethod.POST, "/users").hasAuthority("can_create_users")
                .antMatchers(HttpMethod.GET, "/users").hasAuthority("can_read_users")
                .antMatchers(HttpMethod.PUT, "/users/**").hasAuthority("can_update_users")
                .antMatchers(HttpMethod.DELETE, "/users/**").hasAuthority("can_delete_users")
                .antMatchers(HttpMethod.GET, "/vacuums/search").hasAuthority("can_search_vacuum")
                .antMatchers(HttpMethod.PUT, "/vacuums/start/**").hasAuthority("can_start_vacuum")
                .antMatchers(HttpMethod.PUT, "/vacuums/stop/**").hasAuthority("can_stop_vacuum")
                .antMatchers(HttpMethod.PUT, "/vacuums/discharge/**").hasAuthority("can_discharge_vacuum")
                .antMatchers(HttpMethod.POST, "/vacuums").hasAuthority("can_add_vacuum")
                .antMatchers(HttpMethod.DELETE, "/vacuums").hasAuthority("can_remove_vacuum")
//                .antMatchers("/users").permitAll()
                .anyRequest().authenticated()
                .and().sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        httpSecurity.addFilterBefore(this.jwtFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
