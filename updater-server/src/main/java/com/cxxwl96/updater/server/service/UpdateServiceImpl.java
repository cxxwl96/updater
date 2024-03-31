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

import com.cxxwl96.updater.api.exception.BadRequestException;
import com.cxxwl96.updater.api.model.FileModel;
import com.cxxwl96.updater.api.model.Result;
import com.cxxwl96.updater.api.model.UpdateRequest;
import com.cxxwl96.updater.api.model.UpdateResult;
import com.cxxwl96.updater.api.model.UploadRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;

/**
 * UpdateServiceImpl
 *
 * @author cxxwl96
 * @since 2024/3/31 00:23
 */
@Slf4j
@Service
public class UpdateServiceImpl implements UpdateService {
    @Value("${app.repository}")
    private String appRepository;

    /**
     * 上传应用
     *
     * @param uploadRequest request
     * @param multipartFile multipartFile
     * @return result
     */
    @Override
    public Result<?> upload(UploadRequest uploadRequest, MultipartFile multipartFile) {
        // 仅支持zip文件
        String filename = multipartFile.getOriginalFilename();
        Assert.isTrue(filename.endsWith(".zip"), () -> new BadRequestException("仅支持zip文件上传"));

        // 检查版本是否存在
        String versionPath = String.format("%s/%s/%s", appRepository, uploadRequest.getAppName(),
            uploadRequest.getVersion());
        File versionFile = FileUtil.newFile(versionPath);
        Assert.isFalse(versionFile.exists(), () -> new BadRequestException("已存在该版本的应用"));

        // 创建版本文件夹
        FileUtil.mkdir(versionFile);
        File appFile = new File(String.format("%s/%s", versionPath, filename));

        // 保存文件
        try {
            multipartFile.transferTo(appFile.getAbsoluteFile());
            log.info("Upload file to '{}'.", appFile.getPath());
        } catch (IOException exception) {
            log.error(exception.getMessage(), exception);
            return Result.failed("上传失败: " + exception.getMessage());
        }

        // 解压

        // 计算校验文件

        return Result.success("上传成功");
    }

    @Override
    public Result<UpdateResult> checkUpdate(UpdateRequest request) {
        String appPath = String.format("%s/%s", appRepository, request.getAppName());
        String latestFilePath = String.format("%s/latest", appPath);
        File latestFile = FileUtil.newFile(latestFilePath);
        Assert.isTrue(latestFile.exists(), ()->new BadRequestException("LasTest file not exists"));

        String text = FileUtil.readUtf8String(latestFile);

        String latestVersion = text.trim();
        boolean needUpdate = !latestVersion.equals(request.getVersion());

        UpdateResult result = new UpdateResult();
        result.setAppName(request.getAppName());
        result.setVersion(request.getVersion());
        result.setNeedUpdate(needUpdate);
        if (needUpdate) {
            List<FileModel> fileModels = compareAppFiles(appPath, request.getVersion(), latestVersion);
            result.setFiles(fileModels);
        }

        return Result.success(result);
    }

    private List<FileModel> compareAppFiles(String appPath, String version, String latestVersion) {
        File oldVersionFile = FileUtil.newFile(String.format("%s/%s", appPath, version));
        File latestVersionFile = FileUtil.newFile(String.format("%s/%s", appPath, latestVersion));

        // TODO
        Assert.isTrue(oldVersionFile.exists(), "o file not exists");

        return null;
    }
}