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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
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
    public static void main(String[] args) {
        SpringApplication.run(MainClass.class, args);

        checkUpdate();
    }

    private static void checkUpdate() {
        File checksumFile = FileUtil.newFile(Constant.CHECKLIST);
        if (!checksumFile.exists()) {
            log.warn("没有找到CHECKLIST文件, 将自动生成CHECKLIST文件");
            String checksumHeader = ChecksumUtil.checksumHeader(null, null);
            FileUtil.writeUtf8String(checksumHeader, checksumFile);
        }
        Assert.isTrue(checksumFile.exists(), () -> new BadRequestException("检查更新失败，没有找到CHECKLIST文件"));

        String url = "http://localhost:8000/update/check";
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
            if (StrUtil.equals(oldVersion, newVersion) && CollUtil.isEmpty(modifyFileModels)) {
                // 版本相同且没有需要变更的文件则无需更新
                log.info("已是最新版本");
            } else {
                log.info("检查到新版本{}, 是否需要从版本{}升级到版本{}", newVersion, oldVersion, newVersion);
                for (FileModel modifyFileModel : modifyFileModels) {
                    log.info("{}: {}", modifyFileModel.getOption(), modifyFileModel.getPath());
                }
            }
        }
    }
}