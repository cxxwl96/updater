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

import com.cxxwl96.updater.api.model.Result;
import com.cxxwl96.updater.api.model.UpdateModel;
import com.cxxwl96.updater.api.model.UploadRequest;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * UpdateService
 *
 * @author cxxwl96
 * @since 2024/3/30 23:49
 */
public interface UpdateService {
    /**
     * 上传应用
     *
     * @param request request
     * @param multipartFile multipartFile
     * @return result
     */
    Result<?> upload(UploadRequest request, MultipartFile multipartFile);

    /**
     * 检查更新
     *
     * @param model model
     * @return result
     */
    Result<UpdateModel> checkUpdate(UpdateModel model);

    /**
     * 下载最新应用
     *
     * @param appName app name
     * @param response response
     */
    void downloadLatest(String appName, HttpServletResponse response);
}
