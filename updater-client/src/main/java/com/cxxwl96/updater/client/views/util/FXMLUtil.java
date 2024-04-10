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

package com.cxxwl96.updater.client.views.util;

import com.cxxwl96.updater.client.views.annotations.ViewController;
import com.cxxwl96.updater.client.views.common.IController;

import java.io.InputStream;

import cn.hutool.core.lang.Assert;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.SneakyThrows;

/**
 * FXMLUtil
 *
 * @author cxxwl96
 * @since 2024/04/10 21:20
 */
public class FXMLUtil {
    @SneakyThrows
    public static <T extends IController> Stage loadStage(Class<T> controllerClass) {
        final ViewController annotation = controllerClass.getDeclaredAnnotation(ViewController.class);
        T controller = controllerClass.newInstance();
        Parent root = load(controllerClass, controller);
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setTitle(annotation.title());
        stage.getIcons().add(new Image(annotation.iconPath()));
        controller.initialize(root);
        return stage;
    }

    @SneakyThrows
    private static <T extends IController> Parent load(Class<T> controllerClass, T controller) {
        final ViewController annotation = controllerClass.getDeclaredAnnotation(ViewController.class);
        Assert.notNull(annotation, () -> new NullPointerException("Not ViewController"));
        final String path = annotation.value();
        final InputStream inputStream = controllerClass.getResourceAsStream(path);
        final FXMLLoader loader = new FXMLLoader();
        loader.setController(controller);
        return loader.load(inputStream);
    }
}
