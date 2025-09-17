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


    public double getSpeed() {
        return speedSlider.getValue(); // live slider value
    }


    /** Stop any currently running algorithm gracefully */
    /** Stop any currently running algorithm gracefully */
    private void stopCurrentAlgorithm() {
        if (currentAlgorithm instanceof BFS) {
            ((BFS) currentAlgorithm).stop();
        } else if (currentAlgorithm instanceof DFS) {
            ((DFS) currentAlgorithm).stop();
        }else if (currentAlgorithm instanceof Dijkstra) {
            ((Dijkstra) currentAlgorithm).stop();
        }

        currentAlgorithm = null;
    }



    @FXML
    private void handleRunBFS() {
        stopCurrentAlgorithm();
        statusLabel.setText("Running BFS...");

        var gc = canvas.getGraphicsContext2D();
        BFS bfs = new BFS(this);   // pass controller reference
        currentAlgorithm = bfs;
        bfs.run(gc, () -> Platform.runLater(() -> statusLabel.setText("BFS Finished")));
    }


    @FXML
    private void handleRunDFS() {
        stopCurrentAlgorithm();
        statusLabel.setText("Running DFS...");

        var gc = canvas.getGraphicsContext2D();
        DFS dfs = new DFS(this);   // pass controller reference
        currentAlgorithm = dfs;
        dfs.run(gc, () -> Platform.runLater(() -> statusLabel.setText("DFS Finished")));
    }



    @FXML
    private void handleRunDijkstra() {
        stopCurrentAlgorithm();
        statusLabel.setText("Running Dijkstra...");

        var gc = canvas.getGraphicsContext2D();
        Dijkstra dijkstra = new Dijkstra(this); // pass controller so Dijkstra can read slider
        currentAlgorithm = dijkstra;
        dijkstra.run(gc, () -> Platform.runLater(() -> statusLabel.setText("Dijkstra Finished")));
    }



    @FXML
    private void handleRunAStar() {
        stopCurrentAlgorithm();
        statusLabel.setText("Running A*...");

        var gc = canvas.getGraphicsContext2D();
        AStar aStar = new AStar(this); // pass controller for slider speed
        currentAlgorithm = aStar;
        aStar.run(gc, () -> Platform.runLater(() -> statusLabel.setText("A* Finished")));
    }

}
