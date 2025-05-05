/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.streelet.ecg_java_app;

/**
 *
 * @author erick
 */
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class StartController implements Initializable {

    @FXML
    private Label clockLabel;

    @FXML
    private Label amPmLabel;

    private DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm");
    private DateTimeFormatter amPmFormat = DateTimeFormatter.ofPattern("a");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startClock();
    }

    private void startClock() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    LocalTime now = LocalTime.now();
                    clockLabel.setText(now.format(timeFormat));
                    amPmLabel.setText(
                            (
                                    now.format(amPmFormat).replace(".","")
                                    ).toUpperCase()
                    );
                })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
}