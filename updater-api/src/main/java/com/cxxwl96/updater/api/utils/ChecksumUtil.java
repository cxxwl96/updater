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

package com.cxxwl96.updater.api.utils;

import com.cxxwl96.updater.api.exception.BadRequestException;
import com.cxxwl96.updater.api.model.FileModel;
import com.cxxwl96.updater.api.model.UpdateModel;

import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;

/**
 * ChecksumUtil
 *
 * @author cxxwl96
 * @since 2024/4/1 20:02
 */
@Slf4j
public class ChecksumUtil {
    private static final String SEPARATOR = ":";

    private static final String LINE_BREAK = "\n";

    /**
     * 计算文件/文件夹crc32
     *
     * @param appName app name
     * @param version version
     * @param file file
     * @return crc32
     */
    public static String checksum(String appName, String version, File file) {
        Assert.notNull(file);
        List<FileModel> fileModels;
        if (file.isFile()) {
            fileModels = CollUtil.newArrayList(crc32FileModel(file));
        } else {
            fileModels = FileUtil.loopFiles(file).stream().map(ChecksumUtil::crc32FileModel).collect(Collectors.toList());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Power by http://www.cxxwl96.com")
            .append(LINE_BREAK)
            .append(appName)
            .append(LINE_BREAK)
            .append(version)
            .append(LINE_BREAK);
        for (FileModel fileModel : fileModels) {
            String path = fileModel.getPath().replace(file.getPath(), "");
            if (path.startsWith("/")) {
                path = path.substring(1);
            } else if (path.startsWith("./")) {
                path = path.substring(2);
            }
            sb.append(path).append(SEPARATOR).append(fileModel.getCrc32()).append(LINE_BREAK);
        }
        return sb.toString();
    }

    /**
     * 解析checksum
     *
     * @param checksum checksum
     * @return UpdateModel
     */
    public static UpdateModel parseChecksum(String checksum) {
        Assert.notBlank(checksum, () -> new BadRequestException("未找到校验文件"));
        Scanner scanner = new Scanner(checksum);
        UpdateModel updateModel = new UpdateModel();
        try {
            scanner.nextLine(); // skip power by
            String appName = scanner.nextLine();
            String version = scanner.nextLine();
            updateModel.setAppName(appName);
            updateModel.setVersion(version);
        } catch (NoSuchElementException exception) {
            throw new BadRequestException("校验文件格式错误");
        }
        List<FileModel> fileModels = CollUtil.newArrayList();
        while (scanner.hasNextLine()) {
            try {
                String line = scanner.nextLine();
                Assert.notBlank(line);
                int i = line.lastIndexOf(SEPARATOR);
                String path = line.substring(0, i);
                long crc32 = Long.parseLong(line.substring(i + 1));
                FileModel fileModel = newFileModel(FileUtil.newFile(path)).setCrc32(crc32);
                fileModels.add(fileModel);
            } catch (IndexOutOfBoundsException | IllegalArgumentException exception) {
                log.error(exception.getMessage(), exception);
            }
        }
        return updateModel.setFiles(fileModels);
    }

    private static FileModel crc32FileModel(File file) {
        long crc32 = FileUtil.checksumCRC32(file);
        return newFileModel(file).setCrc32(crc32);
    }

    private static FileModel newFileModel(File file) {
        return new FileModel().setOption(null).setPath(file.getPath()).setName(file.getName());
    }
}
