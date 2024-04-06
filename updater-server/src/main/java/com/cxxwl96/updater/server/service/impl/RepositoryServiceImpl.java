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

package com.cxxwl96.updater.server.service.impl;

import com.cxxwl96.updater.api.enums.FileType;
import com.cxxwl96.updater.api.model.FileModel;
import com.cxxwl96.updater.api.model.Result;
import com.cxxwl96.updater.server.service.RepositoryService;
import com.cxxwl96.updater.server.utils.AppRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;

/**
 * RepositoryServiceImpl
 *
 * @author cxxwl96
 * @since 2024/04/06 16:34
 */
@Service
public class RepositoryServiceImpl implements RepositoryService {
    @Autowired
    private AppRepository appRepository;

    /**
     * 获取所有应用
     *
     * @return 所有应用
     */
    @Override
    public Result<List<FileModel>> apps() {
        File[] files = appRepository.getRepositoryFile().listFiles();
        return Result.success(buildFileModels(files));
    }

    /**
     * 获取应用的所有版本
     *
     * @param appName 应用名
     * @return 应用的所有版本
     */
    @Override
    public Result<List<FileModel>> versions(String appName) {
        File rootFile = appRepository.getRootFile(appName, true);
        return Result.success(buildFileModels(rootFile.listFiles(File::isDirectory)));
    }

    /**
     * 获取应用版本详情
     *
     * @param appName 应用名
     * @param version 应用版本
     * @return 应用版本详情
     */
    @Override
    public Result<List<FileModel>> versionDetail(String appName, String version) {
        File versionFile = appRepository.getVersionFile(appName, version, true);
        List<FileModel> fileModels = loopFiles(versionFile.listFiles());
        return Result.success(fileModels);
    }

    private List<FileModel> loopFiles(File[] files) {
        if (ArrayUtil.isEmpty(files)) {
            return CollUtil.empty(FileModel.class);
        }
        ArrayList<FileModel> list = new ArrayList<>();
        for (File file : files) {
            FileModel fileModel = buildFileModel(file);
            if (file.isDirectory()) {
                List<FileModel> fileModels = loopFiles(file.listFiles());
                fileModel.setChildren(fileModels);
            }
            list.add(fileModel);
        }
        return list;
    }

    private List<FileModel> buildFileModels(File[] files) {
        if (ArrayUtil.isEmpty(files)) {
            return CollUtil.empty(FileModel.class);
        }
        File[] appFiles = Optional.ofNullable(files).orElse(new File[] {});
        return Arrays.stream(appFiles).map(this::buildFileModel).collect(Collectors.toList());
    }

    private FileModel buildFileModel(File file) {
        FileModel fileModel = new FileModel();
        fileModel.setType(file.isFile() ? FileType.FILE : FileType.DIRECTORY);
        fileModel.setName(file.getName());
        fileModel.setSize(file.isFile() ? file.length() : 0L);
        return fileModel;
    }
}
