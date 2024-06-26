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

package com.cxxwl96.updater.server.controller;

import com.cxxwl96.updater.api.model.Result;
import com.cxxwl96.updater.api.model.UploadRequest;
import com.cxxwl96.updater.server.service.UpdateService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

/**
 * UploadController
 *
 * @author cxxwl96
 * @since 2024/04/02 23:45
 */
@RestController
public class UploadController {
    @Autowired
    private UpdateService updateService;

    @PostMapping("/upload")
    public Result<?> upload(@Valid UploadRequest request, MultipartFile file) {
        return updateService.upload(request, file);
    }
}
