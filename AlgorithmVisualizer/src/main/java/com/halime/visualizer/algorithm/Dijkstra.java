package com.halime.visualizer.algorithm;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.*;

public class Dijkstra {
    private final int rows = 20;
    private final int cols = 20;
    private final int cellSize = 30;

    // Colors
    private final Color unexploredColor = Color.LIGHTGRAY;
    private final Color frontierColor = Color.BLUE;
    private final Color exploredColor = Color.GREEN;
    private final Color goalColor = Color.RED;
    private final Color wallColor = Color.BLACK;
    private final Color startColor = Color.YELLOW;
    private final Color pathColor = Color.PURPLE;

    private final int[][] weights = new int[rows][cols];  // cell cost
    private final boolean[][] walls = new boolean[rows][cols];

    private volatile boolean running = false;

    public void stop() {
        running = false;
    }

    public void run(GraphicsContext gc, double speed, Runnable onFinished) {
        running = true;
        setupWeightsAndWalls();

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

        new Thread(() -> {
            try {
                boolean found = dijkstraSearch(startRow, startCol, goalRow, goalCol,
                        gc, speed, visited, frontier, pathCells, parentRow, parentCol);

                if (!running) return; // stopped

                if (found) {
                    LinkedList<int[]> path = reconstructPath(parentRow, parentCol, goalRow, goalCol);
                    animatePath(gc, path, speed, startRow, startCol, goalRow, goalCol, visited, frontier, pathCells, onFinished);
                } else {
                    Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
                    if (onFinished != null) Platform.runLater(onFinished);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                running = false;
            }
        }).start();
    }

    private boolean dijkstraSearch(int startRow, int startCol, int goalRow, int goalCol,
                                   GraphicsContext gc, double speed,
                                   boolean[][] visited, boolean[][] frontier, boolean[][] pathCells,
                                   int[][] parentRow, int[][] parentCol) throws InterruptedException {

        int[][] dist = new int[rows][cols];
        for (int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE);
        dist[startRow][startCol] = 0;

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> dist[a[0]][a[1]]));
        pq.add(new int[]{startRow, startCol});

        while (!pq.isEmpty() && running) {
            int[] current = pq.poll();
            int r = current[0], c = current[1];

            if (visited[r][c]) continue; // skip outdated

            frontier[r][c] = false; // remove from frontier
            visited[r][c] = true;   // finalize

            Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
            Thread.sleep((long) (120 / Math.max(speed, 1)));

            if (r == goalRow && c == goalCol) {
                return true;
            }

            int[][] directions = {{1,0},{-1,0},{0,1},{0,-1}};
            for (int[] d : directions) {
                if (!running) return false;
                int nr = r + d[0], nc = c + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
                if (walls[nr][nc]) continue;

                int newDist = dist[r][c] + weights[nr][nc];
                if (newDist < dist[nr][nc]) {
                    dist[nr][nc] = newDist;
                    parentRow[nr][nc] = r;
                    parentCol[nr][nc] = c;
                    pq.add(new int[]{nr, nc});
                    if (!visited[nr][nc]) {
                        frontier[nr][nc] = true; // only mark unvisited nodes
                    }
                }
            }
        }


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
                             boolean[][] visited, boolean[][] frontier, boolean[][] pathCells,
                             Runnable onFinished) {
        new Thread(() -> {
            try {
                for (int[] cell : path) {
                    if (!running) return;
                    int r = cell[0], c = cell[1];
                    if (!((r == startRow && c == startCol) || (r == goalRow && c == goalCol))) {
                        pathCells[r][c] = true;
                        Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
                        Thread.sleep((long) (160 / Math.max(speed, 1)));
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (onFinished != null && running) Platform.runLater(onFinished);
                running = false;
            }
        }).start();
    }

    // drawGrid() and setupWeightsAndWalls() stay the same...
    private void drawGrid(GraphicsContext gc, boolean[][] visited, boolean[][] frontier,
                          int startRow, int startCol, int goalRow, int goalCol,
                          boolean[][] pathCells) {
        // unchanged
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
        // unchanged
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                weights[r][c] = 1;
                walls[r][c] = false;
            }
        for (int c = 0; c < cols; c++) {
            if (c == 3 || c == 14) continue;
            walls[5][c] = true;
        }
        for (int r = 6; r < rows - 3; r++) {
            if (r == 10) continue;
            walls[r][7] = true;
        }
        for (int r = 2; r < rows - 2; r++) {
            if (r % 2 == 0) walls[r][12] = true;
            else walls[r][13] = true;
        }
        for (int r = 8; r < 12; r++) {
            for (int c = 2; c < 6; c++) {
                weights[r][c] = 5;
            }
        }
        for (int r = 14; r < 18; r++) {
            for (int c = 10; c < 15; c++) {
                weights[r][c] = 10;
            }
        }
    }
}
