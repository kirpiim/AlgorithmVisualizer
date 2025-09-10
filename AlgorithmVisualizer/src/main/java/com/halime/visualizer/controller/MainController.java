package com.halime.visualizer.controller;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

public class MainController {

    @FXML private Canvas canvas;
    @FXML private Slider speedSlider;
    @FXML private Label statusLabel;

    @FXML
    private void handleRunBFS() {
        statusLabel.setText("Running BFS...");
    }

    @FXML
    private void handleRunDFS() {
        statusLabel.setText("Running DFS...");
    }

    @FXML
    private void handleRunDijkstra() {
        statusLabel.setText("Running Dijkstra...");
    }
}
