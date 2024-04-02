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

package com.cxxwl96.updater.server.utils;

import com.cxxwl96.updater.api.exception.BadRequestException;
import com.cxxwl96.updater.api.model.Constant;
import com.cxxwl96.updater.server.config.AppConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;

/**
 * AppRepository
 *
 * @author cxxwl96
 * @since 2024/04/02 20:37
 */
@Component
public class AppRepository {
    @Autowired
    private AppConfig appConfig;

    /**
     * 获取应用根路径
     *
     * @param appName 应用名
     * @param checkExist 检查是否存在，不存在则抛异常
     * @return 应用根路径
     */
    public File getRootFile(String appName, boolean checkExist) {
        File file = FileUtil.newFile(String.format("%s/%s", appConfig.getRepository(), appName));
        checkFileExist(checkExist, file, "应用" + appName + "不存在");
        return file;
    }

    /**
     * 获取应用版本路径
     *
     * @param appName 应用名
     * @param version 应用版本
     * @param checkExist 检查是否存在，不存在则抛异常
     * @return 应用版本路径
     */
    public File getVersionFile(String appName, String version, boolean checkExist) {
        File file = FileUtil.newFile(String.format("%s/%s", getRootFile(appName, checkExist), version));
        checkFileExist(checkExist, file, "版本" + version + "不存在");
        return file;
    }

    /**
     * 获取应用Content路径
     *
     * @param appName 应用名
     * @param version 应用版本
     * @param checkExist 检查是否存在，不存在则抛异常
     * @return 应用Content路径
     */
    public File getContentFile(String appName, String version, boolean checkExist) {
        File file = FileUtil.newFile(String.format("%s/Content", getVersionFile(appName, version, checkExist)));
        checkFileExist(checkExist, file, "Content目录不存在");
        return file;
    }

    /**
     * 获取应用zip包路径
     *
     * @param appName 应用名
     * @param version 应用版本
     * @param checkExist 检查是否存在，不存在则抛异常
     * @return 应用zip包路径
     */
    public File getZipFile(String appName, String version, boolean checkExist) {
        File file = FileUtil.newFile(String.format("%s/%s.zip", getVersionFile(appName, version, checkExist), appName));
        checkFileExist(checkExist, file, "没有找到版本为" + version + "的zip文件");
        return file;
    }

    /**
     * 获取应用Checksum文件路径
     *
     * @param appName 应用名
     * @param version 应用版本
     * @param checkExist 检查是否存在，不存在则抛异常
     * @return 应用Checksum文件路径
     */
    public File getChecksumFile(String appName, String version, boolean checkExist) {
        File file = FileUtil.newFile(String.format("%s/%s", getContentFile(appName, version, checkExist), Constant.CHECKLIST));
        checkFileExist(checkExist, file, "没有找到" + Constant.CHECKLIST + "文件");
        return file;
    }

    /**
     * 获取应用最新版本文件路径
     *
     * @param appName 应用名
     * @param checkExist 检查是否存在，不存在则抛异常
     * @return 应用最新版本文件路径
     */
    public File getLatestFile(String appName, boolean checkExist) {
        File file = FileUtil.newFile(String.format("%s/%s", getRootFile(appName, checkExist), Constant.LATEST));
        checkFileExist(checkExist, file, "没有找到" + Constant.LATEST + "文件");
        return file;
    }

    /**
     * 获取应用最新版本zip包路径
     *
     * @param appName 应用名
     * @param checkExist 检查是否存在，不存在则抛异常
     * @return 应用最新版本zip包路径
     */
    public File getLatestAppZipFile(String appName, boolean checkExist) {
        File latestFile = getLatestFile(appName, checkExist);
        String latestVersion = FileUtil.readUtf8String(latestFile).trim();
        return getZipFile(appName, latestVersion, checkExist);
    }

    /**
     * 获取应用Content下单个文件路径
     *
     * @param appName 应用名
     * @param version 应用版本
     * @param pathRelativeTpContent 文件相对Content的路径
     * @param checkExist 检查是否存在，不存在则抛异常
     * @return 应用Content下单个文件路径
     */
    public File getSingleInContentFile(String appName, String version, String pathRelativeTpContent, boolean checkExist) {
        File file = FileUtil.newFile(String.format("%s/%s", getContentFile(appName, version, checkExist), pathRelativeTpContent));
        checkFileExist(checkExist, file, "没有找到文件: " + pathRelativeTpContent);
        return file;
    }

    /**
     * 检查文件/文件夹是否存在，不存在则抛异常
     *
     * @param checkExist 检查是否存在，不存在则抛异常
     * @param file 文件
     * @param msg 错误消息
     */
    private void checkFileExist(boolean checkExist, File file, String msg) {
        if (checkExist) {
            Assert.isTrue(file.exists(), () -> new BadRequestException(msg));
        }
    }
}
