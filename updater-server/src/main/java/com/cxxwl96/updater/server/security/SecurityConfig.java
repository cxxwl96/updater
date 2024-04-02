/*
 * Copyright (c) 2021-2021, jad (cxxwl96@sina.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cxxwl96.updater.server.security;

import com.cxxwl96.updater.server.config.UrlConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Security配置类
 *
 * @author cxxwl96
 * @since 2021/6/20 13:15
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UrlConfig urlConfig;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and()
            // 禁用CSRF（防止伪造的跨域攻击）
            .csrf().disable()
            // 登录配置
            .formLogin().loginProcessingUrl(UrlConfig.LOGIN_URL)
            // 配置拦截规则
            .and().authorizeRequests() // 对请求执行认证与授权
            .antMatchers(urlConfig.getAllPermitUrls()) // 匹配白名单
            .permitAll() // 不需要通过认证即允许访问
            .anyRequest() // 除以上配置过的请求路径以外的所有请求路径
            .authenticated() // 要求是已经通过认证的
            .and().httpBasic();
    }
}
