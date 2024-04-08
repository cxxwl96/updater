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

import com.cxxwl96.updater.api.model.FileModel;
import com.cxxwl96.updater.api.model.Result;
import com.cxxwl96.updater.api.model.UpdateModel;
import com.cxxwl96.updater.server.service.UpdateService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.URLDecoder;

/**
 * UpdateController
 *
 * @author cxxwl96
 * @since 2024/3/30 23:00
 */
@RestController
public class UpdateController {
    @Autowired
    private UpdateService updateService;

    @PostMapping("/update/check")
    public Result<UpdateModel> checkUpdate(@RequestBody @Valid UpdateModel model) {
        return updateService.checkUpdate(model);
    }

    @GetMapping("/update/{appName}/{version}/**")
    public void update(@PathVariable String appName, @PathVariable String version, HttpServletRequest request,
        HttpServletResponse response) {
        String baseUri = String.format("/update/%s/%s", appName, version);
        String path = request.getRequestURI().substring(baseUri.length());
        String pathRelativeToContent = path.startsWith("/") ? path.substring(1) : path;
        pathRelativeToContent = URLDecoder.decodeForPath(pathRelativeToContent, StandardCharsets.UTF_8);

        UpdateModel model = new UpdateModel().setAppName(appName)
            .setVersion(version)
            .setFiles(CollUtil.newArrayList(new FileModel().setPath(pathRelativeToContent)));

        updateService.updateSingleFile(model, response);
    }
}
