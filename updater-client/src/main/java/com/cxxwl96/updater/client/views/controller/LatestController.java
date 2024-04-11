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

import com.cxxwl96.updater.client.MainClass;
import com.cxxwl96.updater.client.model.CheckUpdateResult;
import com.cxxwl96.updater.client.views.annotations.ViewController;
import com.cxxwl96.updater.client.views.common.IController;

import cn.hutool.core.swing.clipboard.ClipboardUtil;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * LatestController
 *
 * @author cxxwl96
 * @since 2024/04/10 21:42
 */
@Slf4j
@ViewController(value = "/views/Latest.fxml", title = "已经是最新版本", iconPath = "/assets/imgs/logo.png")
public class LatestController implements IController {
    @FXML
    private Label titleLabel;

    @FXML
    private Label versionLabel;

    @FXML
    private Hyperlink copy;

    @FXML
    private Button okBtn;

    private final CheckUpdateResult result;

    public LatestController(CheckUpdateResult result) {
        this.result = result;
    }

    @Override
    public void initialize(Parent parent) {
        log.info("已是最新版本: {}", result.getNewVersion());
        titleLabel.setText(result.getAppName() + titleLabel.getText());
        versionLabel.setText(result.getNewVersion());
        copy.setOnAction(event -> {
            String url = MainClass.host + "/download/" + result.getAppName();
            ClipboardUtil.setStr(url);
        });
        okBtn.setOnAction(event -> ((Stage) parent.getScene().getWindow()).close());
    }
}
