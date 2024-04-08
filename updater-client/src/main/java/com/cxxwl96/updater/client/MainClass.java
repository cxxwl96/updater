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

package com.cxxwl96.updater.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.cxxwl96.updater.api.exception.BadRequestException;
import com.cxxwl96.updater.api.model.Constant;
import com.cxxwl96.updater.api.model.FileModel;
import com.cxxwl96.updater.api.model.Result;
import com.cxxwl96.updater.api.model.UpdateModel;
import com.cxxwl96.updater.api.utils.ChecksumUtil;
import com.cxxwl96.updater.client.model.CheckUpdateResult;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * MainClass
 *
 * @author cxxwl96
 * @since 2024/3/30 23:28
 */
@Slf4j
@SpringBootApplication
public class MainClass {
    private static String host = "http://localhost:8000";

    private static String appRootPath = "./logs/app/";

    public static void main(String[] args) {
        SpringApplication.run(MainClass.class, args);

        CheckUpdateResult result = checkUpdate();

        if (!result.isNeedUpdate()) {
            // 版本相同且没有需要变更的文件则无需更新
            log.info("已是最新版本: {}", result.getNewVersion());
        } else {
            log.info("检查到新版本");
            log.info("当前版本: {}", result.getOldVersion());
            log.info("新版本: {}", result.getNewVersion());
            update(result);
        }
    }

    private static void update(CheckUpdateResult result) {
        String baseUrl = String.format("%s/update/%s/%s/", host, result.getAppName(), result.getNewVersion());
        for (FileModel fileModel : result.getModifyFileModels()) {
            File file = FileUtil.newFile(appRootPath + fileModel.getPath());
            String url = baseUrl + fileModel.getPath();
            HttpUtil.downloadFile(url, file, 3000, new StreamProgress() {
                @Override
                public void start() {
                    log.info("更新文件: {}", fileModel.getPath());
                }

                @Override
                public void progress(long total, long progressSize) {
                    log.info("更新文件: {} {}/{}", fileModel.getPath(), progressSize, total);
                }

                @Override
                public void finish() {
                    log.info("更新文件: {} finish", fileModel.getPath());
                }
            });
        }
    }

    private static CheckUpdateResult checkUpdate() {
        File checksumFile = FileUtil.newFile(appRootPath + Constant.CHECKLIST);
        if (!checksumFile.exists()) {
            log.warn("没有找到CHECKLIST文件, 将自动生成CHECKLIST文件");
            String checksumHeader = ChecksumUtil.checksumHeader(null, null);
            FileUtil.writeUtf8String(checksumHeader, checksumFile);
        }
        Assert.isTrue(checksumFile.exists(), () -> new BadRequestException("检查更新失败，没有找到CHECKLIST文件"));

        String url = host + "/update/check";
        UpdateModel requestBody = ChecksumUtil.parseChecksum(FileUtil.readUtf8String(checksumFile));

        try (HttpResponse response = HttpUtil.createPost(url).body(JSON.toJSONString(requestBody)).execute()) {
            Assert.isTrue(response.isOk(), () -> {
                log.error("请求服务器异常, response: {}", response);
                try {
                    Result<?> result = JSON.parseObject(response.body(), Result.class);
                    return new BadRequestException(result);
                } catch (Throwable throwable) {
                    return new BadRequestException("请求服务器异常");
                }
            });
            Result<UpdateModel> responseBody = JSON.parseObject(response.body(), new TypeReference<Result<UpdateModel>>() {
            });
            Assert.isTrue(responseBody.isSuccess(), () -> new BadRequestException(responseBody.getMsg()));

            UpdateModel updateModel = responseBody.getData();
            List<FileModel> modifyFileModels = updateModel.getFiles()
                .stream()
                .filter(fileModel -> fileModel.getOption() != null)
                .collect(Collectors.toList());

            String oldVersion = requestBody.getVersion();
            String newVersion = updateModel.getVersion();
            CheckUpdateResult result = new CheckUpdateResult();
            result.setNeedUpdate(!StrUtil.equals(oldVersion, newVersion) || CollUtil.isNotEmpty(modifyFileModels));
            result.setAppName(requestBody.getAppName());
            result.setOldVersion(oldVersion);
            result.setNewVersion(newVersion);
            result.setModifyFileModels(modifyFileModels);
            return result;
        }
    }
}