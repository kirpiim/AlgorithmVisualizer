package com.halime.visualizer.algorithm;

import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;

import java.util.LinkedList;
import java.util.Queue;

public class BFS {
    private final int rows = 20;
    private final int cols = 20;
    private final int cellSize = 30;

    // Node colors
    private final Color unexploredColor = Color.LIGHTGRAY;
    private final Color frontierColor = Color.BLUE;
    private final Color exploredColor = Color.GREEN;
    private final Color goalColor = Color.RED;
    private final Color wallColor = Color.BLACK;
    private final Color startColor = Color.YELLOW;

    // Example walls; you can later allow user to place these
    private final boolean[][] walls = new boolean[rows][cols];

    public void run(GraphicsContext gc, double speed) {
        final int canvasWidth = cols * cellSize;
        final int canvasHeight = rows * cellSize;

        Canvas canvas = gc.getCanvas();

        // Ensure canvas has the exact pixel size we expect (must run on FX thread)
        Platform.runLater(() -> {
            canvas.setWidth(canvasWidth);
            canvas.setHeight(canvasHeight);
        });

        // state arrays
        final boolean[][] visited = new boolean[rows][cols];
        final boolean[][] frontier = new boolean[rows][cols];

        final int startRow = 0, startCol = 0;               // top-left start
        final int goalRow = rows - 1, goalCol = cols - 1;  // bottom-right goal

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;
        frontier[startRow][startCol] = true;

        int[][] directions = {{0,1},{1,0},{0,-1},{-1,0}}; // right, down, left, up

        // initial draw
        Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol));

        new Thread(() -> {
            try {
                while (!queue.isEmpty()) {
                    int[] node = queue.poll();
                    int r = node[0], c = node[1];

                    // mark frontier -> explored
                    frontier[r][c] = false;
                    visited[r][c] = true;

                    // draw update
                    Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol));

                    // if we've reached the goal, draw final grid and stop
                    if (r == goalRow && c == goalCol) {
                        Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol));
                        break;
                    }

                    Thread.sleep((long) (200 / Math.max(speed, 1))); // animation speed

                    // expand neighbors
                    for (int[] dir : directions) {
                        int nr = r + dir[0];
                        int nc = c + dir[1];
                        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !visited[nr][nc] && !walls[nr][nc]) {
                            if (!frontier[nr][nc]) {
                                frontier[nr][nc] = true;
                                queue.add(new int[]{nr, nc});
                                // draw after adding to frontier
                                Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol));
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // final draw to ensure goal visible
                Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol));
            }
        }).start();
    }

    private void drawGrid(GraphicsContext gc, boolean[][] visited, boolean[][] frontier,
                          int startRow, int startCol, int goalRow, int goalCol) {
        // full-grid painting so paint order is deterministic
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = c * cellSize;
                double y = r * cellSize;

                if (walls[r][c]) {
                    gc.setFill(wallColor);
                    gc.fillRect(x, y, cellSize, cellSize);
                } else if (visited[r][c]) {
                    gc.setFill(exploredColor);
                    gc.fillRect(x, y, cellSize, cellSize);
                } else if (frontier[r][c]) {
                    gc.setFill(frontierColor);
                    gc.fillRect(x, y, cellSize, cellSize);
                } else {
                    gc.setFill(unexploredColor);
                    gc.fillRect(x, y, cellSize, cellSize);
                }
            }
        }

        // draw start and goal on top (goal last so it is always visible)
        gc.setFill(startColor);
        gc.fillRect(startCol * cellSize, startRow * cellSize, cellSize, cellSize);

        gc.setFill(goalColor);
        gc.fillRect(goalCol * cellSize, goalRow * cellSize, cellSize, cellSize);
    }
}
