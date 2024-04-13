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
import com.cxxwl96.updater.client.UpdaterClient;
import com.cxxwl96.updater.client.model.CheckUpdateResult;
import com.cxxwl96.updater.client.views.annotations.ViewController;
import com.cxxwl96.updater.client.views.common.IController;
import com.cxxwl96.updater.client.views.component.Progress;
import com.cxxwl96.updater.client.views.utils.FXMLUtil;

import java.io.File;
import java.net.URLEncoder;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
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
    private Label titleLabel;

    @FXML
    private ImageView successImg;

    @FXML
    private Label detailLabel;

    @FXML
    private Progress totalProgress;

    @FXML
    private ListView<Parent> listBox;

    private Parent parent;

    private final CheckUpdateResult result;

    public UpdateController(CheckUpdateResult result) {
        this.result = result;
    }

    @Override
    public void initialize(Parent parent) {
        this.parent = parent;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        parent.getScene().getWindow().setOnCloseRequest(event -> {
            if (!executor.isTerminated()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "应用未更新完成，退出更新可能导致软件启动失败，是否退出更新？",
                    ButtonType.CANCEL, ButtonType.OK);
                alert.initOwner(parent.getScene().getWindow());
                Optional<ButtonType> optional = alert.showAndWait();
                optional.ifPresent(buttonType -> {
                    if (buttonType == ButtonType.OK) {
                        executor.shutdownNow();
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
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "应用更新成功，请重启应用");
            alert.show();
        });

        // 更新
        executor.submit(this::update);
        executor.shutdown();
    }

    private void update() {
        try {
            String baseUrl = String.format("%s/update/%s/%s", UpdaterClient.host, result.getAppName(), result.getNewVersion());
            for (int i = 0; i < result.getModifyFileModels().size(); i++) {
                FileModel fileModel = result.getModifyFileModels().get(i);
                File file = FileUtil.newFile(UpdaterClient.appPath + fileModel.getPath());
                // 如果是变更文件则先删除原文件
                if (fileModel.getOption() == FileOption.DELETE) {
                    try {
                        if (file.exists()) {
                            FileUtil.del(file);
                        }
                    } catch (Exception ignored) {
                    }
                    continue;
                }
                ProgressItemController progressItemController = new ProgressItemController();
                Parent progressItemNode = FXMLUtil.load(progressItemController);
                ((VBox) progressItemNode).prefWidthProperty().bind(listBox.widthProperty().subtract(40));
                progressItemController.init();

                int finalI = i;
                Platform.runLater(() -> {
                    listBox.getItems().add(progressItemNode);
                    listBox.scrollTo(finalI); // 滚动条置底
                });
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {
                }

                String url = baseUrl + "?pathRelativeToContent=" + URLEncoder.encode(fileModel.getPath(), StandardCharsets.UTF_8.name());

                log.info("更新文件: {} {}", fileModel.getPath(), PrettyUtil.prettySize(fileModel.getSize()));
                HttpUtil.downloadFile(url, file, 3000, new StreamProgress() {
                    @Override
                    public void start() {
                        Platform.runLater(() -> {
                            progressItemController.setPath(fileModel.getPath());
                        });
                    }

                    @Override
                    public void progress(long total, long progressSize) {
                        Platform.runLater(() -> {
                            progressItemController.setProgress(progressSize * 1.0 / total);
                            progressItemController.setProgressText(
                                PrettyUtil.prettySize(progressSize, 1) + "/" + PrettyUtil.prettySize(total, 1));
                        });
                    }

                    @Override
                    public void finish() {
                        log.info("更新文件成功: {}", fileModel.getPath());
                        Platform.runLater(() -> {
                            if (finalI == result.getModifyFileModels().size() - 1) {
                                titleLabel.setText("更新成功");
                                successImg.setManaged(true);
                                successImg.setVisible(true);
                                totalProgress.setProgress(1);
                            } else {
                                totalProgress.setProgress(finalI * 1.0 / result.getModifyFileModels().size());
                            }
                        });
                    }
                });
            }
        } catch (Exception exception) {
            if (exception instanceof IORuntimeException && exception.getCause() instanceof ClosedByInterruptException) {
                log.error("Closed by InterruptException");
                closeThisStage();
                return;
            }
            log.error(exception.getMessage(), exception);
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, exception.getMessage());
                alert.setOnCloseRequest(event -> closeThisStage());
                alert.show();
            });
        }
    }

    private void closeThisStage() {
        Optional.ofNullable(this.parent).map(Parent::getScene).map(Scene::getWindow).ifPresent(window -> ((Stage) window).close());
    }
}
