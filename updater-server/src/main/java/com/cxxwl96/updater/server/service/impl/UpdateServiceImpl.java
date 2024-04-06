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

import com.cxxwl96.updater.api.enums.FileOption;
import com.cxxwl96.updater.api.exception.BadRequestException;
import com.cxxwl96.updater.api.model.FileModel;
import com.cxxwl96.updater.api.model.Result;
import com.cxxwl96.updater.api.model.UpdateModel;
import com.cxxwl96.updater.api.model.UploadRequest;
import com.cxxwl96.updater.api.utils.ChecksumUtil;
import com.cxxwl96.updater.server.config.AppConfig;
import com.cxxwl96.updater.server.service.UpdateService;
import com.cxxwl96.updater.server.utils.AppRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
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
    @Autowired
    private AppConfig appConfig;

    @Autowired
    private AppRepository appRepository;

    /**
     * 上传应用
     *
     * @param request request
     * @param multipartFile multipartFile
     * @return result
     */
    @Override
    public Result<?> upload(UploadRequest request, MultipartFile multipartFile) {
        // 仅支持zip文件
        Assert.isTrue(StrUtil.endWith(multipartFile.getOriginalFilename(), ".zip"), () -> new BadRequestException("仅支持zip文件上传"));

        String appName = request.getAppName();
        String version = request.getVersion();

        // 检查版本是否存在
        File versionFile = appRepository.getVersionFile(appName, version, false);
        Assert.isFalse(versionFile.exists(), () -> new BadRequestException("已存在该版本的应用"));
        // 创建应用内容文件夹
        File contentFile = appRepository.getContentFile(appName, version, false);
        FileUtil.mkdir(contentFile);

        try {
            // 上传的应用zip文件
            File originalZipFile = FileUtil.newFile(versionFile.getPath() + "/" + multipartFile.getOriginalFilename());

            // 保存文件
            log.info("Upload file to '{}'", originalZipFile.getPath());
            multipartFile.transferTo(originalZipFile.getAbsoluteFile());

            // 解压
            log.info("Unzip file '{}' to '{}'", originalZipFile.getPath(), contentFile.getPath());
            ZipUtil.unzip(originalZipFile, contentFile);

            // 删除上传的zip
            log.info("Delete origin zip file '{}'", originalZipFile.getPath());
            FileUtil.del(originalZipFile);

            // 删除忽略的文件
            List<String> ignoredFiles = appConfig.getIgnoreFiles().get(appName);
            if (CollUtil.isNotEmpty(ignoredFiles)) {
                delIgnoredFiles(ignoredFiles, contentFile);
            }

            // 计算校验文件并保存
            log.info("Checksum file '{}'", contentFile.getPath());
            String checksum = ChecksumUtil.checksum(appName, version, contentFile);
            FileUtil.writeUtf8String(checksum, appRepository.getChecksumFile(appName, version, false));

            // 重新压缩zip
            File zipFile = appRepository.getZipFile(appName, version, false);
            log.info("Re-zip file '{}'", zipFile.getPath());
            ZipUtil.zip(contentFile.getAbsolutePath(), zipFile.getAbsolutePath(), true);

            // 更新latest
            if (request.isLatest()) {
                log.info("Update latest file to '{}'", version);
                FileUtil.writeUtf8String(version, appRepository.getLatestFile(appName, false));
            }
            return Result.success("上传成功");
        } catch (IOException exception) {
            log.error(exception.getMessage(), exception);
            return Result.failed("上传失败: " + exception.getMessage());
        }
    }

    /**
     * 检查更新
     *
     * @param model model
     * @return result
     */
    @Override
    public Result<UpdateModel> checkUpdate(UpdateModel model) {
        String appName = model.getAppName();
        File latestFile = appRepository.getLatestFile(appName, true);

        // 获取最新版本的Checksum
        String latestVersion = FileUtil.readUtf8String(latestFile).trim();
        File latestChecksumFile = appRepository.getChecksumFile(appName, latestVersion, true);

        String checksum = FileUtil.readUtf8String(latestChecksumFile);
        UpdateModel latestUpdateModel = ChecksumUtil.parseChecksum(checksum);

        // 补充额外的CheckList
        addCheckListFileModel(latestUpdateModel);

        List<FileModel> fileModels = compareAppFiles(latestUpdateModel, model);
        latestUpdateModel.setFiles(fileModels);
        return Result.success(latestUpdateModel);
    }

    /**
     * 下载最新应用
     *
     * @param appName app name
     * @param response response
     */
    @Override
    public void downloadLatest(String appName, HttpServletResponse response) {
        appRepository.getRootFile(appName, true);

        File appZipFile = appRepository.getLatestAppZipFile(appName, true);

        dealDownload(response, appZipFile);
    }

    /**
     * 更新应用单文件
     *
     * @param model model
     * @param response response
     */
    @Override
    public void updateSingleFile(UpdateModel model, HttpServletResponse response) {
        appRepository.getRootFile(model.getAppName(), true);
        Assert.notEmpty(model.getFiles(), () -> new BadRequestException("请选择需要更新的应用文件"));
        Assert.isTrue(model.getFiles().size() == 1, () -> new BadRequestException("仅支持单文件更新"));
        String appName = model.getAppName();
        String version = model.getVersion();
        String path = model.getFiles().get(0).getPath();

        File singleFile = appRepository.getSingleInContentFile(appName, version, path, true);

        dealDownload(response, singleFile);
    }

    private void delIgnoredFiles(List<String> ignoredFiles, File file) {
        if (ignoredFiles.contains(file.getName())) {
            log.info("Delete ignore file '{}'", file.getPath());
            FileUtil.del(file);
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    delIgnoredFiles(ignoredFiles, childFile);
                }
            }
        }
    }

    private void addCheckListFileModel(UpdateModel latestUpdateModel) {
        String appName = latestUpdateModel.getAppName();
        String version = latestUpdateModel.getVersion();
        // 添加Checksum文件
        File checksumFile = appRepository.getChecksumFile(appName, version, true);
        File contentFile = appRepository.getContentFile(appName, version, true);
        FileModel checksumFileModel = ChecksumUtil.crc32FileModel(checksumFile);

        String path = checksumFile.getPath().substring(contentFile.getPath().length());
        if (path.startsWith("/")) {
            path = path.substring(1);
        } else if (path.startsWith("./")) {
            path = path.substring(2);
        }
        FileModel fileModel = checksumFileModel.setOption(FileOption.ADD).setPath(path);
        latestUpdateModel.getFiles().add(fileModel);
    }

    private List<FileModel> compareAppFiles(UpdateModel latestUpdateModel, UpdateModel model) {
        // 用户file
        List<FileModel> files = model.getFiles();
        if (CollUtil.isEmpty(files)) {
            return latestUpdateModel.getFiles().stream().map(file -> file.setOption(FileOption.ADD)).collect(Collectors.toList());
        }
        // 系统file
        List<FileModel> latestFiles = CollUtil.newArrayList(latestUpdateModel.getFiles());

        // 转为map
        Map<String, FileModel> fileMap = new HashMap<>();
        for (FileModel file : files) {
            fileMap.put(file.getPath(), file);
        }
        Map<String, FileModel> latestFileMap = new HashMap<>();
        for (FileModel file : latestFiles) {
            latestFileMap.put(file.getPath(), file);
        }

        // 新增、修改
        for (FileModel latestFile : latestFiles) {
            FileModel file = fileMap.get(latestFile.getPath());
            if (file == null) {
                latestFile.setOption(FileOption.ADD);
            } else if (!latestFile.getCrc32().equals(file.getCrc32())) {
                latestFile.setOption(FileOption.OVERWRITE);
            }
        }

        // 删除
        for (FileModel file : files) {
            if (!latestFileMap.containsKey(file.getPath())) {
                file.setOption(FileOption.DELETE);
                latestFiles.add(file);
            }
        }

        return latestFiles;
    }

    private void dealDownload(HttpServletResponse response, File file) {
        try (InputStream is = FileUtil.getInputStream(file); OutputStream os = response.getOutputStream()) {
            String contentDisposition = String.format("attachment;fileName=%s;filename*=utf-8''%s", file.getName(),
                URLEncoder.encode(file.getName(), "UTF-8"));
            // 响应头设置
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("content-type", "application/x-zip-compressed;charset=UTF-8");
            response.setHeader("Content-Disposition", contentDisposition);
            response.setHeader("Access-Control-Allow-Origin", "*"); // 实现跨域下载

            IoUtil.copy(is, os);
        } catch (IOException exception) {
            log.error("下载文件异常失败", exception);
            throw new BadRequestException("下载文件异常失败");
        }
    }
}
