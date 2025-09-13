package com.halime.visualizer.controller;

import com.halime.visualizer.algorithm.BFS;
import com.halime.visualizer.algorithm.DFS;
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
        var gc = canvas.getGraphicsContext2D();
        double speed = Math.max(1, speedSlider.getValue()); // ensure >0
        BFS bfs = new BFS();
        bfs.run(gc, speed);
    }

    @FXML
    private void handleRunDFS() {
        statusLabel.setText("Running DFS...");
        var gc = canvas.getGraphicsContext2D();
        double speed = Math.max(1, speedSlider.getValue());
        DFS dfs = new DFS();
        dfs.run(gc, speed);
    }

    @FXML
    private void handleRunDijkstra() {
        statusLabel.setText("Running Dijkstra...");
    }
}
