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
import com.cxxwl96.updater.api.exception.BadRequestException;
import com.cxxwl96.updater.api.model.Constant;
import com.cxxwl96.updater.api.model.FileModel;
import com.cxxwl96.updater.api.model.Result;
import com.cxxwl96.updater.server.config.AppConfig;
import com.cxxwl96.updater.server.service.RepositoryService;
import com.cxxwl96.updater.server.utils.AppRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;

/**
 * RepositoryServiceImpl
 *
 * @author cxxwl96
 * @since 2024/04/06 16:34
 */
@Service
public class RepositoryServiceImpl implements RepositoryService {
    @Autowired
    private AppConfig appConfig;

    @Autowired
    private AppRepository appRepository;

    /**
     * 获取相对仓库根目录路径下的文件列表
     *
     * @param pathRelativeToRepository 相对仓库根目录路径
     * @return 相对仓库根目录路径下的文件列表
     */
    @Override
    public Result<List<FileModel>> list(@Nullable String pathRelativeToRepository) {
        String path = Optional.ofNullable(pathRelativeToRepository).orElse(StrUtil.EMPTY);

        File repositoryFile = FileUtil.newFile(appRepository.getRepositoryFile().getPath());

        // 拦截路径注入
        if (path.contains("../") || path.contains("/..")) {
            throw new BadRequestException("你没有权限查看");
        }

        File[] files = FileUtil.newFile(repositoryFile.getPath() + "/" + path).listFiles();
        if (ArrayUtil.isEmpty(files)) {
            return Result.success();
        }
        // 过滤被忽略的文件，先按文件夹、文件排序，再按文件排序
        List<String> defaultIgnoreFiles = appConfig.getDefaultIgnoreFiles();
        Map<String, List<String>> ignoreFiles = appConfig.getIgnoreFiles();
        List<FileModel> fileModels = Arrays.stream(files).map(file -> buildFileModel(file, repositoryFile)).filter(fileModel -> {
            String name = fileModel.getName();
            // 过滤LATEST文件
            if (Constant.LATEST.equals(name)) {
                return false;
            }
            // 过滤默认忽略的文件
            if (CollUtil.contains(defaultIgnoreFiles, name)) {
                return false;
            }
            // 过滤应用忽略的文件
            int endIndex = fileModel.getPath().indexOf("/");
            String appName = fileModel.getPath().substring(0, endIndex < 0 ? fileModel.getPath().length() : endIndex);
            return ignoreFiles == null || !ignoreFiles.containsKey(appName) || !ignoreFiles.get(appName).contains(name);
        }).sorted((fileModel1, fileModel2) -> {
            int value1 = 0, value2 = 0;
            if (fileModel1.getType() == FileType.DIRECTORY) {
                value1 = 1;
            }
            if (fileModel2.getType() == FileType.DIRECTORY) {
                value2 = 1;
            }
            if (value1 != value2) {
                return value2 - value1;
            } else {
                return fileModel1.getName().compareTo(fileModel2.getName());
            }
        }).collect(Collectors.toList());
        return Result.success(fileModels);
    }

    /**
     * 获取应用最新版本
     *
     * @param appName app name
     * @return 应用最新版本
     */
    @Override
    public Result<String> latest(String appName) {
        String latestVersion = appRepository.getLatestVersion(appName, true, true);
        return Result.success("success", latestVersion);
    }

    private FileModel buildFileModel(File file, File relativeFile) {
        FileModel fileModel = new FileModel();
        fileModel.setType(file.isFile() ? FileType.FILE : FileType.DIRECTORY);
        fileModel.setPath(file.getPath());
        fileModel.setName(file.getName());
        fileModel.setSize(file.isFile() ? file.length() : 0L);
        // 去掉仓库前缀
        if (relativeFile != null) {
            String pathTemp = fileModel.getPath().replace(relativeFile.getPath(), "");
            if (pathTemp.startsWith("/")) {
                pathTemp = pathTemp.substring(1);
            } else if (pathTemp.startsWith("./")) {
                pathTemp = pathTemp.substring(2);
            }
            fileModel.setPath(pathTemp);
        }
        return fileModel;
    }
}
