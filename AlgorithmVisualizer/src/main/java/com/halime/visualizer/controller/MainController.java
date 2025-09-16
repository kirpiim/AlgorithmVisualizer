package com.halime.visualizer.controller;

import com.halime.visualizer.algorithm.BFS;
import com.halime.visualizer.algorithm.DFS;
import com.halime.visualizer.algorithm.Dijkstra;
import com.halime.visualizer.algorithm.AStar;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

public class MainController {

    @FXML private Canvas canvas;
    @FXML private Slider speedSlider;
    @FXML private Label statusLabel;

    private Object currentAlgorithm; // keep reference to running algorithm

    /** Stop any currently running algorithm gracefully */
    private void stopCurrentAlgorithm() {
        if (currentAlgorithm instanceof BFS) {
            ((BFS) currentAlgorithm).stop();
        }
        // later: add DFS.stop(), Dijkstra.stop(), AStar.stop()
        currentAlgorithm = null;
    }

    @FXML
    private void handleRunBFS() {
        stopCurrentAlgorithm(); // stop previous before starting new
        statusLabel.setText("Running BFS...");

        var gc = canvas.getGraphicsContext2D();
        double speed = Math.max(1, speedSlider.getValue());

        BFS bfs = new BFS();
        currentAlgorithm = bfs;
        bfs.run(gc, speed, () ->
                Platform.runLater(() -> statusLabel.setText("BFS Finished")));
    }

    @FXML
    private void handleRunDFS() {
        stopCurrentAlgorithm();
        statusLabel.setText("Running DFS...");

        var gc = canvas.getGraphicsContext2D();
        double speed = Math.max(1, speedSlider.getValue());

        DFS dfs = new DFS();
        currentAlgorithm = dfs;
        dfs.run(gc, speed);
    }

    @FXML
    private void handleRunDijkstra() {
        stopCurrentAlgorithm();
        statusLabel.setText("Running Dijkstra...");

        var gc = canvas.getGraphicsContext2D();
        double speed = Math.max(1, speedSlider.getValue());

        Dijkstra dijkstra = new Dijkstra();
        currentAlgorithm = dijkstra;
        dijkstra.run(gc, speed);
    }

    @FXML
    private void handleRunAStar() {
        stopCurrentAlgorithm();
        statusLabel.setText("Running A*...");

        var gc = canvas.getGraphicsContext2D();
        double speed = Math.max(1, speedSlider.getValue());

        AStar aStar = new AStar();
        currentAlgorithm = aStar;
        aStar.run(gc, speed);
    }
}
