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
import com.cxxwl96.updater.client.views.enums.PageKeys;

import java.util.Set;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * UpdateConfirmController
 *
 * @author cxxwl96
 * @since 2024/04/10 21:42
 */
@ViewController(value = "/views/Index.fxml", title = "正在检查更新", iconPath = "/assets/imgs/logo.png")
public class IndexController implements IController {

    @FXML
    private Label titleLabel;

    @Override
    public void initialize(Parent parent) {
        Stage stage = (Stage) parent.getScene().getWindow();

        for (PageKeys pageKeys : PageKeys.values()) {
            SimpleBooleanProperty property = pageKeys.getProperty();
            // 标题绑定
            property.addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    stage.setTitle(pageKeys.getTitle());
                }
            });
            // visible绑定
            Set<Node> nodes = parent.lookupAll("." + pageKeys.getName());
            for (Node node : nodes) {
                node.visibleProperty().bindBidirectional(property);
                node.managedProperty().bindBidirectional(property);
            }
        }

        PageKeys.switchPage(PageKeys.UPDATING);
    }
}
