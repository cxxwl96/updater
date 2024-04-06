/*
 * Copyright (c) 2021-2024, cxxwl96.com (cxxwl96@sina.com).
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

package com.cxxwl96.updater.server.service;

import com.cxxwl96.updater.api.model.FileModel;
import com.cxxwl96.updater.api.model.Result;

import java.util.List;

/**
 * RepositoryService
 *
 * @author cxxwl96
 * @since 2024/04/06 16:42
 */
public interface RepositoryService {
    /**
     * 获取所有应用
     *
     * @return 所有应用
     */
    Result<List<FileModel>> apps();

    /**
     * 获取应用的所有版本
     *
     * @param appName 应用名
     * @return 应用的所有版本
     */
    Result<List<FileModel>> versions(String appName);

    /**
     * 获取应用版本详情
     *
     * @param appName 应用名
     * @param version 应用版本
     * @return 应用版本详情
     */
    Result<List<FileModel>> versionDetail(String appName, String version);
}
