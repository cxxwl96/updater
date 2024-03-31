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

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * FileModel
 *
 * @author cxxwl96
 * @since 2024/3/30 23:17
 */
@Data
@Accessors(chain = true)
public class FileModel {
    // 文件操作
    private FileOption option;

    // 文件相对应用根目录的路径
    private String path;

    // 文件名
    private String name;

    // 文件大小
    private long size;

    // 友好的文件大小
    private String prettifySize;
}