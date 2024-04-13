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

import com.cxxwl96.updater.api.utils.PrettyUtil;
import com.cxxwl96.updater.client.UpdaterClient;
import com.cxxwl96.updater.client.model.CheckUpdateResult;
import com.cxxwl96.updater.client.views.annotations.ViewController;
import com.cxxwl96.updater.client.views.common.IController;
import com.cxxwl96.updater.client.views.utils.FXMLUtil;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * ConfirmUpdateController
 *
 * @author cxxwl96
 * @since 2024/04/10 21:42
 */
@Slf4j
@ViewController(value = "/views/ConfirmUpdate.fxml", title = "检查到新版本", iconPath = "/assets/imgs/logo.png")
public class ConfirmUpdateController implements IController {
    @FXML
    private Label oldVersionLabel;

    @FXML
    private Label newVersionLabel;

    @FXML
    private Label detailLabel;

    @FXML
    private Hyperlink copy;

    @FXML
    private Button cancelBtn;

    @FXML
    private Button updateBtn;

    private final CheckUpdateResult result;

    public ConfirmUpdateController(CheckUpdateResult result) {
        this.result = result;
    }

    @Override
    public void initialize(Parent parent) {
        String detail = String.format("需要更新%d个文件，总%s", result.getModifyFileModels().size(),
            PrettyUtil.prettySize(result.getTotalSize()));

        log.info("检查到新版本");
        log.info("当前版本: {}", result.getOldVersion());
        log.info("新版本: {}", result.getNewVersion());
        log.info(detail);

        oldVersionLabel.setText(result.getOldVersion());
        newVersionLabel.setText(result.getNewVersion());
        detailLabel.setText(detail);
        copy.setOnAction(event -> {
            String url = UpdaterClient.host + "/download/" + result.getAppName();
            ClipboardUtil.setStr(url);
        });
        cancelBtn.setOnAction(event -> ((Stage) parent.getScene().getWindow()).close());
        updateBtn.setOnAction(event -> {
            Stage stage = (Stage) parent.getScene().getWindow();
            Platform.runLater(stage::close);
            Platform.runLater(() -> FXMLUtil.loadStage(new UpdateController(result)).show());
        });
    }
}
