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
import com.cxxwl96.updater.client.model.PropertyKeys;
import com.cxxwl96.updater.client.views.controller.CheckUpdateController;
import com.cxxwl96.updater.client.views.controller.ConfirmUpdateController;
import com.cxxwl96.updater.client.views.controller.LatestController;
import com.cxxwl96.updater.client.views.utils.FXMLUtil;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.system.SystemUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * MainClass
 *
 * @author cxxwl96
 * @since 2024/3/30 23:28
 */
@Slf4j
public class UpdaterClient extends Application {
    public static String host = "";

    public static String appPath = "./";

    public static String appName = "";

    public static String appVersion = "";

    public static void run(String... args) {
        // 启动javafx应用
        Application.launch(UpdaterClient.class, args);
    }

    @Override
    public void start(Stage primaryStage) {
        initEnv();

        Stage checkUpdateStage = FXMLUtil.loadStage(new CheckUpdateController());
        checkUpdateStage.show();

        // 检查更新
        new Thread(() -> {
            CheckUpdateResult result;
            try {
                result = checkUpdate();
            } catch (Exception exception) {
                log.error(exception.getMessage(), exception);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, exception.getMessage());
                    alert.setOnCloseRequest(event -> checkUpdateStage.close());
                    alert.show();
                });
                return;
            }
            Platform.runLater(checkUpdateStage::close);
            if (!result.isNeedUpdate()) {
                Platform.runLater(() -> FXMLUtil.loadStage(new LatestController(result)).show());
            } else {
                Platform.runLater(() -> FXMLUtil.loadStage(new ConfirmUpdateController(result)).show());
            }
        }).start();
    }

    private void initEnv() {
        Properties props = SystemUtil.getProps();
        Optional.ofNullable(props.get(PropertyKeys.HOST)).map(Object::toString).ifPresent(val -> UpdaterClient.host = val);
        Optional.ofNullable(props.get(PropertyKeys.APP_PATH)).map(Object::toString).ifPresent(val -> UpdaterClient.appPath = val);
        log.info("{}={}", PropertyKeys.HOST, host);
        log.info("{}={}", PropertyKeys.APP_PATH, appPath);
        Assert.notBlank(host, "Host is blank");
        Assert.notBlank(appPath, "AppPath is blank");
    }

    private void initAppInfo() {
        Properties props = SystemUtil.getProps();
        Optional.ofNullable(props.get(PropertyKeys.APP_NAME)).map(Object::toString).ifPresent(val -> UpdaterClient.appName = val);
        Optional.ofNullable(props.get(PropertyKeys.APP_VERSION)).map(Object::toString).ifPresent(val -> UpdaterClient.appVersion = val);
        log.info("{}={}", PropertyKeys.APP_NAME, appName);
        log.info("{}={}", PropertyKeys.APP_VERSION, appVersion);
        Assert.notBlank(appName, "AppName is blank");
        Assert.notBlank(appVersion, "AppVersion is blank");
    }

    private CheckUpdateResult checkUpdate() {
        File checksumFile = FileUtil.newFile(UpdaterClient.appPath + Constant.CHECKLIST);
        if (!checksumFile.exists()) {
            log.warn("没有找到CHECKLIST文件, 将自动生成CHECKLIST文件: {}", checksumFile.getAbsolutePath());
            initAppInfo();
            String checksumHeader = ChecksumUtil.checksumHeader(appName, appVersion);
            FileUtil.writeUtf8String(checksumHeader, checksumFile);
        }
        Assert.isTrue(checksumFile.exists(), () -> new BadRequestException("检查更新失败，没有找到CHECKLIST文件"));

        String url = UpdaterClient.host + "/update/check";
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
            long totalSize = modifyFileModels.stream().mapToLong(FileModel::getSize).sum();

            String oldVersion = requestBody.getVersion();
            String newVersion = updateModel.getVersion();
            CheckUpdateResult result = new CheckUpdateResult();
            // 版本相同且没有需要变更的文件则无需更新
            result.setNeedUpdate(!StrUtil.equals(oldVersion, newVersion) || CollUtil.isNotEmpty(modifyFileModels));
            result.setAppName(requestBody.getAppName());
            result.setOldVersion(oldVersion);
            result.setNewVersion(newVersion);
            result.setModifyFileModels(modifyFileModels);
            result.setTotalSize(totalSize);
            return result;
        }
    }
}