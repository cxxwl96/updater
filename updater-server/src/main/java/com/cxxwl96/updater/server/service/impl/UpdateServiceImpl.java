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
            List<String> defaultIgnoreFiles = appConfig.getDefaultIgnoreFiles();
            List<String> ignoredFiles = appConfig.getIgnoreFiles().get(appName);
            deleteIgnoredFiles(ignoredFiles, defaultIgnoreFiles, contentFile);

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

        // 获取最新版本的Checksum
        String latestVersion = appRepository.getLatestVersion(appName, true, true);
        File latestChecksumFile = appRepository.getChecksumFile(appName, latestVersion, true);

        String checksum = FileUtil.readUtf8String(latestChecksumFile);
        UpdateModel latestUpdateModel = ChecksumUtil.parseChecksum(checksum);

        List<FileModel> modifyFileModels = compareAppFiles(latestUpdateModel.getFiles(), model.getFiles());

        // 如果有变更的文件，则补充额外的CheckList
        if (modifyFileModels.stream().anyMatch(fileModel -> fileModel.getOption() != null)) {
            // 补充额外的CheckList
            FileModel fileModel = buildCheckListFileModel(latestUpdateModel.getAppName(), latestUpdateModel.getVersion());
            modifyFileModels.add(fileModel);
        }

        latestUpdateModel.setFiles(modifyFileModels);
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
        String latestVersion = appRepository.getLatestVersion(appName, true, true);
        File appZipFile = appRepository.getLatestAppZipFile(appName, true);
        dealDownload(response, appZipFile, appName + "-" + latestVersion + ".zip");
    }

    /**
     * 下载应用
     *
     * @param appName app name
     * @param version version
     * @param response response
     */
    @Override
    public void download(String appName, String version, HttpServletResponse response) {
        File zipFile = appRepository.getZipFile(appName, version, true);
        dealDownload(response, zipFile, appName + "-" + version + ".zip");
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

        dealDownload(response, singleFile, singleFile.getName());
    }

    private void deleteIgnoredFiles(List<String> ignoredFiles, List<String> defaultIgnoreFiles, File file) {
        if (CollUtil.contains(ignoredFiles, file.getName()) || CollUtil.contains(defaultIgnoreFiles, file.getName())) {
            log.info("Delete ignore file '{}'", file.getPath());
            FileUtil.del(file);
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    deleteIgnoredFiles(ignoredFiles, defaultIgnoreFiles, childFile);
                }
            }
        }
    }

    private FileModel buildCheckListFileModel(String appName, String version) {
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
        return checksumFileModel.setOption(FileOption.ADD).setPath(path);
    }

    private List<FileModel> compareAppFiles(List<FileModel> latestFileModels, List<FileModel> fileModels) {
        // 用户file
        if (CollUtil.isEmpty(fileModels)) {
            return latestFileModels.stream().map(fileModel -> fileModel.setOption(FileOption.ADD)).collect(Collectors.toList());
        }
        // 系统file
        List<FileModel> modifyFileModels = CollUtil.newArrayList(latestFileModels);

        // 转为map
        Map<String, FileModel> fileModelMap = new HashMap<>();
        for (FileModel fileModel : fileModels) {
            fileModelMap.put(fileModel.getPath(), fileModel);
        }
        Map<String, FileModel> latestFileModelMap = new HashMap<>();
        for (FileModel fileModel : modifyFileModels) {
            latestFileModelMap.put(fileModel.getPath(), fileModel);
        }

        // 新增、修改
        for (FileModel latestFileModel : modifyFileModels) {
            FileModel file = fileModelMap.get(latestFileModel.getPath());
            if (file == null) {
                latestFileModel.setOption(FileOption.ADD);
            } else if (!latestFileModel.getCrc32().equals(file.getCrc32())) {
                latestFileModel.setOption(FileOption.OVERWRITE);
            }
        }

        // 删除
        for (FileModel fileModel : fileModels) {
            if (!latestFileModelMap.containsKey(fileModel.getPath())) {
                fileModel.setOption(FileOption.DELETE);
                modifyFileModels.add(fileModel);
            }
        }

        return modifyFileModels;
    }

    private void dealDownload(HttpServletResponse response, File file, String filename) {
        try (InputStream is = FileUtil.getInputStream(file); OutputStream os = response.getOutputStream()) {
            String contentDisposition = String.format("attachment;fileName=%s;filename*=utf-8''%s", filename,
                URLEncoder.encode(filename, "UTF-8"));
            // 响应头设置
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-type", "application/x-zip-compressed;charset=UTF-8");
            response.setHeader("Content-Disposition", contentDisposition);
            response.setHeader("Content-Length", String.valueOf(file.length()));
            response.setHeader("Access-Control-Allow-Origin", "*"); // 实现跨域下载

            IoUtil.copy(is, os);
        } catch (IOException exception) {
            log.error("下载文件异常失败", exception);
            throw new BadRequestException("下载文件异常失败");
        }
    }
}
