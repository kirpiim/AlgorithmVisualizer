package com.halime.visualizer.algorithm;

import com.halime.visualizer.controller.MainController;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.*;

public class AStar {
    private final int rows = 20;
    private final int cols = 20;
    private final int cellSize = 30;

    private volatile boolean running = false;
    private Thread worker;
    private Thread animator;
    private final MainController controller;

    // Colors
    private final Color unexploredColor = Color.LIGHTGRAY;
    private final Color frontierColor = Color.BLUE;
    private final Color exploredColor = Color.GREEN;
    private final Color goalColor = Color.RED;
    private final Color wallColor = Color.BLACK;
    private final Color startColor = Color.YELLOW;
    private final Color pathColor = Color.PURPLE;

    private final int[][] weights = new int[rows][cols];
    private final boolean[][] walls = new boolean[rows][cols];

    public AStar(MainController controller) {
        this.controller = controller;
    }

    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
        if (animator != null) {
            animator.interrupt();
            animator = null;
        }
    }

    public void run(GraphicsContext gc, Runnable onFinished) {
        stop();
        running = true;
        setupWeightsAndWalls();

        // ✅ Fix: resize canvas so grid fits exactly
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

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                parentRow[r][c] = -1;
                parentCol[r][c] = -1;
            }

        final int startRow = 0, startCol = 0;
        final int goalRow = rows - 1, goalCol = cols - 1;

        Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));

        worker = new Thread(() -> {
            try {
                boolean found = aStarSearch(startRow, startCol, goalRow, goalCol,
                        gc, visited, frontier, pathCells, parentRow, parentCol);

                if (!running) return;

                if (found) {
                    LinkedList<int[]> path = reconstructPath(parentRow, parentCol, goalRow, goalCol);
                    animatePath(gc, path, startRow, startCol, goalRow, goalCol,
                            visited, frontier, pathCells, onFinished);
                } else {
                    Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
                    if (onFinished != null) Platform.runLater(onFinished);
                    running = false;
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private boolean aStarSearch(int startRow, int startCol, int goalRow, int goalCol,
                                GraphicsContext gc,
                                boolean[][] visited, boolean[][] frontier, boolean[][] pathCells,
                                int[][] parentRow, int[][] parentCol) throws InterruptedException {

        int[][] g = new int[rows][cols];
        for (int[] row : g) Arrays.fill(row, Integer.MAX_VALUE);
        g[startRow][startCol] = 0;

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a ->
                g[a[0]][a[1]] + heuristic(a[0], a[1], goalRow, goalCol)));
        pq.add(new int[]{startRow, startCol});
        frontier[startRow][startCol] = true;
        Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));

        while (running && !pq.isEmpty()) {
            int[] current = pq.poll();
            int r = current[0], c = current[1];

            if (visited[r][c]) continue;

            frontier[r][c] = true;
            Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
            sleepDynamic(120);

            frontier[r][c] = false;
            visited[r][c] = true;
            Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));

            if (r == goalRow && c == goalCol) {
                return true;
            }

            int[][] directions = {{1,0},{-1,0},{0,1},{0,-1}};
            for (int[] d : directions) {
                if (!running) return false;
                int nr = r + d[0], nc = c + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
                if (walls[nr][nc]) continue;

                int newG = g[r][c] + weights[nr][nc];
                if (newG < g[nr][nc]) {
                    g[nr][nc] = newG;
                    parentRow[nr][nc] = r;
                    parentCol[nr][nc] = c;
                    pq.add(new int[]{nr, nc});
                    if (!visited[nr][nc]) {
                        frontier[nr][nc] = true;
                        Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
                    }
                }
            }
        }
        return false;
    }

    private int heuristic(int r, int c, int goalRow, int goalCol) {
        return Math.abs(r - goalRow) + Math.abs(c - goalCol);
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

    private void animatePath(GraphicsContext gc, LinkedList<int[]> path,
                             int startRow, int startCol, int goalRow, int goalCol,
                             boolean[][] visited, boolean[][] frontier, boolean[][] pathCells,
                             Runnable onFinished) {
        animator = new Thread(() -> {
            try {
                for (int[] cell : path) {
                    if (!running) return;
                    int r = cell[0], c = cell[1];

                    if (!((r == startRow && c == startCol) || (r == goalRow && c == goalCol))) {
                        pathCells[r][c] = true;
                        Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
                        sleepDynamic(160);
                    }
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                if (onFinished != null) Platform.runLater(onFinished);
                running = false;
                animator = null;
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
                    if (weights[r][c] == 1) {
                        gc.setFill(unexploredColor);
                    } else if (weights[r][c] == 5) {
                        gc.setFill(Color.ORANGE);
                    } else if (weights[r][c] == 10) {
                        gc.setFill(Color.LIGHTBLUE);
                    } else {
                        gc.setFill(unexploredColor);
                    }
                }

                gc.fillRect(x, y, cellSize, cellSize);
                gc.setStroke(Color.DARKGRAY);
                gc.strokeRect(x, y, cellSize, cellSize);

                if (!walls[r][c] && weights[r][c] > 1) {
                    gc.setFill(Color.BLACK);
                    gc.fillText(String.valueOf(weights[r][c]), x + cellSize / 3, y + cellSize / 1.5);
                }
            }
        }

        gc.setFill(startColor);
        gc.fillRect(startCol * cellSize, startRow * cellSize, cellSize, cellSize);

        gc.setFill(goalColor);
        gc.fillRect(goalCol * cellSize, goalRow * cellSize, cellSize, cellSize);
    }

    private void setupWeightsAndWalls() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                weights[r][c] = 1;
                walls[r][c] = false;
            }

        for (int c = 0; c < cols; c++) {
            if (c == 3 || c == 14) continue;
            walls[5][c] = true;
        }
        for (int r = 6; r < rows - 3; r++) if (r != 10) walls[r][7] = true;
        for (int r = 2; r < rows - 2; r++) {
            if (r % 2 == 0) walls[r][12] = true;
            else walls[r][13] = true;
        }
        for (int r = 8; r < 12; r++) for (int c = 2; c < 6; c++) weights[r][c] = 5;
        for (int r = 14; r < 18; r++) for (int c = 10; c < 15; c++) weights[r][c] = 10;
    }

    private void sleepDynamic(long baseDelay) throws InterruptedException {
        double speed = (controller != null) ? Math.max(controller.getSpeed(), 0.1) : 1.0;
        Thread.sleep((long) (baseDelay / speed));
    }
}
