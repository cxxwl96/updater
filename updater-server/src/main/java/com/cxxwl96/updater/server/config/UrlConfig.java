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

package com.cxxwl96.updater.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;

/**
 * UrlUtil
 *
 * @author cxxwl96
 * @since 2021/6/20 20:38
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.security")
public class UrlConfig {
    public static final String LOGIN_URL = "/login";

    // 白名单列表
    private static final Set<String> PERMIT_URLS = CollUtil.newHashSet(LOGIN_URL);

    // 配置文件配置的白名单
    private String[] urlWhiteList;

    /**
     * 获取所有URL白名单
     * - 包括配置文件配置的白名单
     *
     * @return URL白名单
     */
    public String[] getAllPermitUrls() {
        // 添加配置文件配置的白名单
        if (urlWhiteList != null) {
            for (String urlsStr : urlWhiteList) {
                String[] urls = urlsStr.split(",");
                for (String url : urls) {
                    if (StrUtil.isNotBlank(url)) {
                        PERMIT_URLS.add(url.trim());
                    }
                }
            }
        }
        return PERMIT_URLS.toArray(new String[0]);
    }
}
