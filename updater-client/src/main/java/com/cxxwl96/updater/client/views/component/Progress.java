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

package com.cxxwl96.updater.client.views.component;

import cn.hutool.core.lang.func.VoidFunc0;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;

/**
 * Progress
 *
 * @author cxxwl96
 * @since 2024/04/10 20:28
 */
public class Progress extends VBox {
    private final SimpleDoubleProperty progress = new SimpleDoubleProperty(this, "progress", 0);

    private final SimpleDoubleProperty progressHeight = new SimpleDoubleProperty(this, "progressHeight", 3);

    private final SimpleObjectProperty<Type> type = new SimpleObjectProperty<>(Type.PRIMARY);

    private final SimpleStringProperty labelValue = new SimpleStringProperty(this, "labelValue", "");

    @Setter
    private VoidFunc0 onComplete;

    public enum Type {
        PRIMARY("#409EFF"),
        SUCCESS("#67C23A"),
        WARNING("#E6A23C"),
        DANGER("#F56C6C"),
        INFO("#909399"),
        ;

        @Getter
        private final String frontColor;

        Type(String frontColor) {
            this.frontColor = frontColor;
        }
    }

    public Progress() {
        init();
    }

    private void init() {
        HBox progressBox = new HBox();
        progressBox.setStyle("-fx-background-color: " + type.get().getFrontColor());

        HBox innerBox = new HBox(progressBox);
        innerBox.prefHeightProperty().bind(progressHeight);
        innerBox.setStyle("-fx-background-color: #e0e0e0");
        innerBox.setPrefWidth(100);
        HBox.setHgrow(innerBox, Priority.ALWAYS);

        Label label = new Label();
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12");
        label.textProperty().bindBidirectional(labelValue);
        label.setText(((int) (progress.doubleValue() * 100)) + "%");

        HBox outerBox = new HBox(innerBox, label);
        outerBox.setAlignment(Pos.CENTER);
        outerBox.setFillHeight(false);
        outerBox.setSpacing(5);

        super.getChildren().addAll(outerBox);

        innerBox.widthProperty().addListener((observable, oldValue, newValue) -> {
            double val = newValue.doubleValue() * progress.doubleValue();
            progressBox.setPrefWidth(val);
        });

        progress.addListener((observable, oldValue, newValue) -> {
            double val = newValue.doubleValue();
            if (val < 0) {
                val = 0;
            } else if (val > 1) {
                val = 1;
            }
            progressBox.setPrefWidth(val * innerBox.getWidth());
            label.setText(((int) (val * 100)) + "%");
            if (val == 1 && onComplete != null) {
                onComplete.callWithRuntimeException();
            }
        });

        type.addListener((observable, oldValue, newValue) -> {
            progressBox.setStyle("-fx-background-color: " + newValue.getFrontColor());
        });
    }

    public double getProgress() {
        return progress.get();
    }

    public SimpleDoubleProperty progressProperty() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress.set(progress);
    }

    public double getProgressHeight() {
        return progressHeight.get();
    }

    public SimpleDoubleProperty progressHeightProperty() {
        return progressHeight;
    }

    public void setProgressHeight(double progressHeight) {
        this.progressHeight.set(progressHeight);
    }

    public Type getType() {
        return type.get();
    }

    public SimpleObjectProperty<Type> typeProperty() {
        return type;
    }

    public void setType(Type type) {
        this.type.set(type);
    }

    public String getLabelValue() {
        return labelValue.get();
    }

    public SimpleStringProperty labelValueProperty() {
        return labelValue;
    }

    public void setLabelValue(String labelValue) {
        this.labelValue.set(labelValue);
    }
}
