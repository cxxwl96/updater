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

import com.cxxwl96.updater.client.views.annotations.ViewController;
import com.cxxwl96.updater.client.views.common.IController;
import com.cxxwl96.updater.client.views.component.Progress;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

/**
 * UpdatingController
 *
 * @author cxxwl96
 * @since 2024/04/10 21:42
 */
@Slf4j
@ViewController(value = "/views/ProgressItem.fxml")
public class ProgressItemController implements IController {
    @FXML
    private Progress progress;

    @FXML
    private Label pathLabel;

    @FXML
    private Text progressText;

    public void init() {
        progress.setOnComplete(() -> progress.setType(Progress.Type.SUCCESS));
    }

    public void setProgress(double progress) {
        this.progress.setProgress(progress);
    }

    public void setPath(String path) {
        pathLabel.setText(path);
        pathLabel.setTooltip(new Tooltip(path));
    }

    public void setProgressText(String text) {
        progressText.setText(text);
    }

}
