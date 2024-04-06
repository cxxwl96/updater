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
import com.cxxwl96.updater.server.service.RepositoryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RepositoryController
 *
 * @author cxxwl96
 * @since 2024/04/06 16:48
 */
@RestController
@RequestMapping("/repository")
public class RepositoryController {
    @Autowired
    private RepositoryService repositoryService;

    @GetMapping("/apps")
    public Result<List<FileModel>> apps() {
        return repositoryService.apps();
    }

    @GetMapping("/apps/{appName}")
    public Result<List<FileModel>> versions(@PathVariable String appName) {
        return repositoryService.versions(appName);
    }

    @GetMapping("/apps/{appName}/{version}")
    public Result<List<FileModel>> versionDetail(@PathVariable String appName, @PathVariable String version) {
        return repositoryService.versionDetail(appName, version);
    }
}
