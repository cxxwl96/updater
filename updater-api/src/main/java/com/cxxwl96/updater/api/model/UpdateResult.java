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

package com.cxxwl96.updater.api.model;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * UpdateModel
 *
 * @author cxxwl96
 * @since 2024/3/30 23:13
 */
@Data
@Accessors(chain = true)
public class UpdateResult {
    // 应用名称
    private String appName;

    // 应用版本
    private String version;

    // 是否需要更新
    private boolean needUpdate;

    // 需要下载的应用文件列表。needUpdate=true时不为空
    private List<FileModel> files;
}
