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
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
/**
 * Main JavaFX controller for the Pathfinding Visualizer.
 *
 * Handles user interaction, canvas drawing, algorithm execution,
 * and animation speed control. Dispatches runs for BFS, DFS,
 * Dijkstra, and A* algorithms while updating the UI asynchronously.
 *
 * Features:
 * - Start/stop BFS, DFS, Dijkstra, and A*
 * - Reset grid to an empty state (with start and goal only)
 * - Speed slider for dynamic animation speed
 * - Status label for feedback (running, finished, reset)
 *
 * Algorithms are run on worker threads and update the canvas
 * safely via {@link Platform#runLater}.
 */
public class MainController {

    @FXML private Canvas canvas;
    @FXML private Slider speedSlider;
    @FXML private Label statusLabel;

    private Object currentAlgorithm; // keep reference to running algorithm

    /**
     * Get the current animation speed from the UI slider.
     *
     * @return live speed value (never null)
     */
    public double getSpeed() {
        return speedSlider.getValue(); // live slider value
    }

    /**
     * Reset button handler.
     * Stops any running algorithm and restores the grid to an empty state.
     */
    @FXML
    private void handleReset() {
        stopCurrentAlgorithm();
        statusLabel.setText("Grid Reset");

        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawEmptyGrid(gc);
    }

    /**
     * Draw an empty 20x20 grid with only start (yellow) and goal (red).
     *
     * @param gc the canvas graphics context
     */
    private void drawEmptyGrid(GraphicsContext gc) {
        int rows = 20;
        int cols = 20;
        int cellSize = 30;

        // Resize canvas to match algorithms
        canvas.setWidth(cols * cellSize);
        canvas.setHeight(rows * cellSize);

        // Fill background
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw empty grid cells
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = c * cellSize;
                double y = r * cellSize;
                gc.setFill(Color.LIGHTGRAY);
                gc.fillRect(x, y, cellSize, cellSize);

                gc.setStroke(Color.DARKGRAY);
                gc.strokeRect(x, y, cellSize, cellSize);
            }
        }

        // Start (yellow)
        gc.setFill(Color.YELLOW);
        gc.fillRect(0, 0, cellSize, cellSize);

        // Goal (red) — bottom-right
        gc.setFill(Color.RED);
        gc.fillRect((cols - 1) * cellSize, (rows - 1) * cellSize, cellSize, cellSize);
    }


    /**
     * Stop any currently running algorithm gracefully.
     * Ensures worker and animator threads are cancelled.
     */
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


    /**
     * Run BFS algorithm and update status label on completion.
     */
    @FXML
    private void handleRunBFS() {
        stopCurrentAlgorithm();
        statusLabel.setText("Running BFS...");

        var gc = canvas.getGraphicsContext2D();
        BFS bfs = new BFS(this);   // pass controller reference
        currentAlgorithm = bfs;
        bfs.run(gc, () -> Platform.runLater(() -> statusLabel.setText("BFS Finished")));
    }

    /**
     * Run DFS algorithm and update status label on completion.
     */
    @FXML
    private void handleRunDFS() {
        stopCurrentAlgorithm();
        statusLabel.setText("Running DFS...");

        var gc = canvas.getGraphicsContext2D();
        DFS dfs = new DFS(this);   // pass controller reference
        currentAlgorithm = dfs;
        dfs.run(gc, () -> Platform.runLater(() -> statusLabel.setText("DFS Finished")));
    }


    /**
     * Run Dijkstra algorithm and update status label on completion.
     */
    @FXML
    private void handleRunDijkstra() {
        stopCurrentAlgorithm();
        statusLabel.setText("Running Dijkstra...");

        var gc = canvas.getGraphicsContext2D();
        Dijkstra dijkstra = new Dijkstra(this); // pass controller so Dijkstra can read slider
        currentAlgorithm = dijkstra;
        dijkstra.run(gc, () -> Platform.runLater(() -> statusLabel.setText("Dijkstra Finished")));
    }


    /**
     * Run A* algorithm and update status label on completion.
     */
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
