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

package com.cxxwl96.updater.client.views.controller;

import com.cxxwl96.updater.api.enums.FileOption;
import com.cxxwl96.updater.api.model.FileModel;
import com.cxxwl96.updater.api.utils.PrettyUtil;
import com.cxxwl96.updater.client.MainClass;
import com.cxxwl96.updater.client.model.CheckUpdateResult;
import com.cxxwl96.updater.client.views.annotations.ViewController;
import com.cxxwl96.updater.client.views.common.IController;
import com.cxxwl96.updater.client.views.component.Progress;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.http.HttpUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

/**
 * UpdatingController
 *
 * @author cxxwl96
 * @since 2024/04/10 21:42
 */
@Slf4j
@ViewController(value = "/views/Update.fxml", title = "更新程序", iconPath = "/assets/imgs/logo.png")
public class UpdateController implements IController {

    @FXML
    private Label detailLabel;

    @FXML
    private Progress totalProgress;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private VBox listVbox;

    private final CheckUpdateResult result;

    public UpdateController(CheckUpdateResult result) {
        this.result = result;
    }

    @Override
    public void initialize(Parent parent) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        parent.getScene().getWindow().setOnCloseRequest(event -> {
            if (!executor.isTerminated()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "应用未更新完成，退出更新可能导致软件启动失败，是否退出更新？",
                    ButtonType.CANCEL, ButtonType.OK);
                alert.initOwner(parent.getScene().getWindow());
                Optional<ButtonType> optional = alert.showAndWait();
                optional.ifPresent(buttonType -> {
                    if (buttonType == ButtonType.OK) {
                        System.exit(0);
                    } else {
                        event.consume();
                    }
                });
            }
        });

        String detail = String.format("%d个文件，总%s", result.getModifyFileModels().size(), PrettyUtil.prettySize(result.getTotalSize()));
        detailLabel.setText(detail);

        totalProgress.setOnComplete(() -> {
            totalProgress.setType(Progress.Type.SUCCESS);
            scrollPane.setVvalue(1); // 滚动条置底
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "应用更新成功，请重启应用");
            alert.show();
        });

        // 更新
        executor.submit(this::update);
        executor.shutdown();
    }

    private void update() {
        try {
            String baseUrl = String.format("%s/update/%s/%s/", MainClass.host, result.getAppName(), result.getNewVersion());
            for (int i = 0; i < result.getModifyFileModels().size(); i++) {
                FileModel fileModel = result.getModifyFileModels().get(i);
                File file = FileUtil.newFile(MainClass.appRootPath + fileModel.getPath());
                // 如果是变更文件则先删除原文件
                if (fileModel.getOption() == FileOption.DELETE || fileModel.getOption() == FileOption.OVERWRITE) {
                    try {
                        if (file.exists()) {
                            FileUtil.del(file);
                        }
                    } catch (Exception ignored) {
                    }
                }
                Label pathLabel = new Label(fileModel.getPath());
                pathLabel.setStyle("-fx-font-size: 10;");
                pathLabel.setTooltip(new Tooltip(fileModel.getPath()));
                VBox pathVBox = new VBox(pathLabel);
                HBox.setHgrow(pathVBox, Priority.ALWAYS);

                Label progressLabel = new Label();
                progressLabel.setStyle("-fx-font-size: 10");
                HBox bottomBox = new HBox(pathVBox, progressLabel);
                bottomBox.setSpacing(5);

                Progress progress = new Progress();
                progress.setOnComplete(() -> progress.setType(Progress.Type.SUCCESS));
                VBox vBox = new VBox(progress, bottomBox);
                Platform.runLater(() -> {
                    listVbox.getChildren().add(vBox);
                    scrollPane.setVvalue(1); // 滚动条置底
                });

                String url = URLEncodeUtil.encode(baseUrl + fileModel.getPath(), StandardCharsets.UTF_8);
                int finalI = i;
                HttpUtil.downloadFile(url, file, 3000, new StreamProgress() {
                    @Override
                    public void start() {
                        log.info("更新文件: {} {}", fileModel.getPath(), PrettyUtil.prettySize(fileModel.getSize()));
                    }

                    @Override
                    public void progress(long total, long progressSize) {
                        Platform.runLater(() -> {
                            progress.setProgress(progressSize * 1.0 / total);
                            progressLabel.setText(PrettyUtil.prettySize(progressSize, 0) + "/" + PrettyUtil.prettySize(total, 0));
                        });
                    }

                    @Override
                    public void finish() {
                        log.info("更新文件成功: {}", fileModel.getPath());
                        Platform.runLater(() -> {
                            if (finalI == result.getModifyFileModels().size() - 1) {
                                totalProgress.setProgress(1);
                            } else {
                                totalProgress.setProgress(finalI * 1.0 / result.getModifyFileModels().size());
                            }
                            scrollPane.setVvalue(1); // 滚动条置底
                        });
                    }
                });
            }
        } catch (Exception exception) {
            log.error(exception.getMessage(), exception);
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, exception.getMessage());
                alert.setOnCloseRequest(event -> Platform.exit());
                alert.show();
            });
        }
    }
}
