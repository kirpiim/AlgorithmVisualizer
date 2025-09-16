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
        } else if (currentAlgorithm instanceof DFS) {
            ((DFS) currentAlgorithm).stop();
        } else if (currentAlgorithm instanceof Dijkstra) {
            ((Dijkstra) currentAlgorithm).stop();
        } else if (currentAlgorithm instanceof AStar) {
            ((AStar) currentAlgorithm).stop();
        }
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
        stopCurrentAlgorithm(); // stop BFS/other algo first
        statusLabel.setText("Running DFS...");

        var gc = canvas.getGraphicsContext2D();
        double speed = Math.max(1, speedSlider.getValue());

        DFS dfs = new DFS();
        currentAlgorithm = dfs;
        dfs.run(gc, speed, () ->
                Platform.runLater(() -> statusLabel.setText("DFS Finished")));
    }

    @FXML
    private void handleRunDijkstra() {
        stopCurrentAlgorithm();
        statusLabel.setText("Running Dijkstra...");

        var gc = canvas.getGraphicsContext2D();
        double speed = Math.max(1, speedSlider.getValue());

        Dijkstra dijkstra = new Dijkstra();
        currentAlgorithm = dijkstra;
        dijkstra.run(gc, speed, () ->
                Platform.runLater(() -> statusLabel.setText("Dijkstra Finished")));
    }

    @FXML
    private void handleRunAStar() {
        stopCurrentAlgorithm();
        statusLabel.setText("Running A*...");

        var gc = canvas.getGraphicsContext2D();
        double speed = Math.max(1, speedSlider.getValue());

        AStar aStar = new AStar();
        currentAlgorithm = aStar;
        aStar.run(gc, speed); // run A* search

        // run finish check asynchronously
        new Thread(() -> {
            try {
                // Wait for algorithm to finish
                Thread.sleep(100); // small delay so UI updates start
                while (aStarIsRunning()) {
                    Thread.sleep(100);
                }
                Platform.runLater(() -> statusLabel.setText("A* Finished"));
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private boolean aStarIsRunning() {
        if (currentAlgorithm instanceof AStar) {
            // use reflection-safe check of its running flag
            return true; // replace with getter if we expose in AStar
        }
        return false;
    }
}
