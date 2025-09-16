package com.halime.visualizer.algorithm;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.LinkedList;

public class DFS {
    private final int rows = 20;
    private final int cols = 20;
    private final int cellSize = 30;

    // Node colors
    private final Color unexploredColor = Color.LIGHTGRAY;
    private final Color frontierColor = Color.BLUE;   // current head
    private final Color exploredColor = Color.GREEN;  // visited
    private final Color goalColor = Color.RED;
    private final Color wallColor = Color.BLACK;
    private final Color startColor = Color.YELLOW;
    private final Color pathColor = Color.PURPLE;

    private final boolean[][] walls = new boolean[rows][cols];

    // ---- Thread management ----
    private volatile boolean running = false;
    private Thread worker;
    private Thread animator;

    /** Gracefully stop DFS and its animation */
    public void stop() {
        running = false;
        if (worker != null) worker.interrupt();
        if (animator != null) animator.interrupt();
    }

    public void run(GraphicsContext gc, double speed, Runnable onFinish) {
        setupWalls();

        final int canvasWidth = cols * cellSize;
        final int canvasHeight = rows * cellSize;

        Canvas canvas = gc.getCanvas();
        Platform.runLater(() -> {
            canvas.setWidth(canvasWidth);
            canvas.setHeight(canvasHeight);
        });

        final boolean[][] visited = new boolean[rows][cols];
        final boolean[][] frontier = new boolean[rows][cols];
        final boolean[][] pathCells = new boolean[rows][cols];

        final int[][] parentRow = new int[rows][cols];
        final int[][] parentCol = new int[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++) {
                parentRow[i][j] = -1;
                parentCol[i][j] = -1;
            }

        final int startRow = 0, startCol = 0;
        final int goalRow = rows - 1, goalCol = cols - 1;

        Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));

        running = true;
        worker = new Thread(() -> {
            try {
                boolean found = dfsExplore(startRow, startCol, gc, speed,
                        visited, frontier, pathCells, parentRow, parentCol, goalRow, goalCol);

                if (running && found) {
                    LinkedList<int[]> path = reconstructPath(parentRow, parentCol, goalRow, goalCol);
                    animatePath(gc, path, speed, startRow, startCol, goalRow, goalCol, visited, frontier, pathCells);
                } else {
                    Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
                }
            } catch (InterruptedException ignored) {
                // stopped gracefully
            } finally {
                if (running && onFinish != null) {
                    Platform.runLater(onFinish);
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private boolean dfsExplore(int r, int c,
                               GraphicsContext gc, double speed,
                               boolean[][] visited, boolean[][] frontier, boolean[][] pathCells,
                               int[][] parentRow, int[][] parentCol,
                               int goalRow, int goalCol) throws InterruptedException {

        if (!running || Thread.currentThread().isInterrupted()) return false;

        if (visited[r][c]) return false;

        visited[r][c] = true;
        frontier[r][c] = true;
        Platform.runLater(() -> drawGrid(gc, visited, frontier, 0, 0, goalRow, goalCol, pathCells));

        Thread.sleep((long) (160 / Math.max(speed, 1)));
        if (!running || Thread.currentThread().isInterrupted()) return false;

        if (r == goalRow && c == goalCol) {
            return true;
        }

        int[][] directions = {{1,0},{0,1},{0,-1},{-1,0}};

        for (int[] dir : directions) {
            int nr = r + dir[0];
            int nc = c + dir[1];
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
            if (visited[nr][nc] || walls[nr][nc]) continue;

            parentRow[nr][nc] = r;
            parentCol[nr][nc] = c;

            frontier[r][c] = false;
            Platform.runLater(() -> drawGrid(gc, visited, frontier, 0, 0, goalRow, goalCol, pathCells));
            Thread.sleep((long) (120 / Math.max(speed, 1)));
            if (!running || Thread.currentThread().isInterrupted()) return false;

            boolean found = dfsExplore(nr, nc, gc, speed, visited, frontier, pathCells, parentRow, parentCol, goalRow, goalCol);
            if (found) return true;

            frontier[r][c] = true;
            Platform.runLater(() -> drawGrid(gc, visited, frontier, 0, 0, goalRow, goalCol, pathCells));
            Thread.sleep((long) (120 / Math.max(speed, 1)));
        }

        frontier[r][c] = false;
        Platform.runLater(() -> drawGrid(gc, visited, frontier, 0, 0, goalRow, goalCol, pathCells));
        Thread.sleep((long) (120 / Math.max(speed, 1)));
        return false;
    }

    private LinkedList<int[]> reconstructPath(int[][] parentRow, int[][] parentCol, int goalRow, int goalCol) {
        LinkedList<int[]> path = new LinkedList<>();
        int cr = goalRow, cc = goalCol;
        while (cr != -1 && cc != -1) {
            path.addFirst(new int[]{cr, cc});
            int pr = parentRow[cr][cc];
            int pc = parentCol[cr][cc];
            cr = pr;
            cc = pc;
        }
        return path;
    }

    private void animatePath(GraphicsContext gc, LinkedList<int[]> path, double speed,
                             int startRow, int startCol, int goalRow, int goalCol,
                             boolean[][] visited, boolean[][] frontier, boolean[][] pathCells) {
        animator = new Thread(() -> {
            try {
                for (int[] cell : path) {
                    if (!running || Thread.currentThread().isInterrupted()) return;

                    int r = cell[0], c = cell[1];
                    if (!((r == startRow && c == startCol) || (r == goalRow && c == goalCol))) {
                        pathCells[r][c] = true;
                        Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
                        Thread.sleep((long) (160 / Math.max(speed, 1)));
                    }
                }
            } catch (InterruptedException ignored) {
                // stopped
            }
        });
        animator.setDaemon(true);
        animator.start();
    }

    private void drawGrid(GraphicsContext gc, boolean[][] visited, boolean[][] frontier,
                          int startRow, int startCol, int goalRow, int goalCol,
                          boolean[][] pathCells) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = c * cellSize;
                double y = r * cellSize;

                if (pathCells[r][c]) {
                    gc.setFill(pathColor);
                } else if (walls[r][c]) {
                    gc.setFill(wallColor);
                } else if (frontier[r][c]) {
                    gc.setFill(frontierColor);
                } else if (visited[r][c]) {
                    gc.setFill(exploredColor);
                } else {
                    gc.setFill(unexploredColor);
                }
                gc.fillRect(x, y, cellSize, cellSize);

                gc.setStroke(Color.DARKGRAY);
                gc.strokeRect(x, y, cellSize, cellSize);
            }
        }

        gc.setFill(startColor);
        gc.fillRect(startCol * cellSize, startRow * cellSize, cellSize, cellSize);

        gc.setFill(goalColor);
        gc.fillRect(goalCol * cellSize, goalRow * cellSize, cellSize, cellSize);
    }

    private void setupWalls() {
        for (int c = 0; c < cols; c++) {
            if (c == 5 || c == 10 || c == 15) continue;
            walls[10][c] = true;
        }
        for (int r = 4; r < rows; r++) {
            if (r == 12) continue;
            walls[r][4] = true;
        }
        for (int r = 6; r < rows - 3; r++) {
            if (r % 2 == 0) walls[r][14] = true;
            else walls[r][15] = true;
        }
    }
}
