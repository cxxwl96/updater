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
import com.cxxwl96.updater.api.model.FileModel;
import com.cxxwl96.updater.api.model.Result;
import com.cxxwl96.updater.server.service.RepositoryService;
import com.cxxwl96.updater.server.utils.AppRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        List<FileModel> fileModels = new ArrayList<>();
        if (ArrayUtil.isEmpty(files)) {
            return Result.success();
        }

        // 先按文件夹、文件排序，再按文件排序
        List<File> sortedFiles = Arrays.stream(files).sorted((file1, file2) -> {
            int value1 = 0, value2 = 0;
            if (file1.isDirectory()) {
                value1 = 1;
            }
            if (file2.isDirectory()) {
                value2 = 1;
            }
            if (value1 != value2) {
                return value2 - value1;
            } else {
                return file1.getName().compareTo(file2.getName());
            }
        }).collect(Collectors.toList());

        for (File childFile : sortedFiles) {
            FileModel fileModel = buildFileModel(childFile, repositoryFile);
            fileModels.add(fileModel);
        }
        return Result.success(fileModels);
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
