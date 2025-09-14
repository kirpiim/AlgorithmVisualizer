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

    public void run(GraphicsContext gc, double speed) {
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

                if (found) {
                    LinkedList<int[]> path = reconstructPath(parentRow, parentCol, goalRow, goalCol);
                    animatePath(gc, path, speed, startRow, startCol, goalRow, goalCol, visited, frontier, pathCells);
                } else {
                    Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
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

        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int r = current[0], c = current[1];

            if (visited[r][c]) continue;
            visited[r][c] = true;

            frontier[r][c] = true;
            Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
            Thread.sleep((long) (120 / Math.max(speed, 1)));
            frontier[r][c] = false;

            if (r == goalRow && c == goalCol) {
                return true;
            }

            int[][] directions = {{1,0},{-1,0},{0,1},{0,-1}};
            for (int[] d : directions) {
                int nr = r + d[0];
                int nc = c + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
                if (walls[nr][nc]) continue;

                int newDist = dist[r][c] + weights[nr][nc];
                if (newDist < dist[nr][nc]) {
                    dist[nr][nc] = newDist;
                    parentRow[nr][nc] = r;
                    parentCol[nr][nc] = c;
                    pq.add(new int[]{nr, nc});
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
                             boolean[][] visited, boolean[][] frontier, boolean[][] pathCells) {
        new Thread(() -> {
            try {
                for (int[] cell : path) {
                    int r = cell[0], c = cell[1];
                    if (!((r == startRow && c == startCol) || (r == goalRow && c == goalCol))) {
                        pathCells[r][c] = true;
                        Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
                        Thread.sleep((long) (160 / Math.max(speed, 1)));
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
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
                    // weight-based color
                    if (weights[r][c] == 1) {
                        gc.setFill(unexploredColor); // normal ground
                    } else if (weights[r][c] == 5) {
                        gc.setFill(Color.ORANGE);  // rocky terrain
                    } else if (weights[r][c] == 10) {
                        gc.setFill(Color.LIGHTBLUE); // water
                    } else {
                        gc.setFill(unexploredColor);
                    }
                }

                // fill cell
                gc.fillRect(x, y, cellSize, cellSize);

                // optional: draw grid lines
                gc.setStroke(Color.DARKGRAY);
                gc.strokeRect(x, y, cellSize, cellSize);

                // optional: draw weight numbers for clarity
                if (!walls[r][c] && weights[r][c] > 1) {
                    gc.setFill(Color.BLACK);
                    gc.fillText(String.valueOf(weights[r][c]), x + cellSize / 3, y + cellSize / 1.5);
                }
            }
        }

        // draw start and goal on top
        gc.setFill(startColor);
        gc.fillRect(startCol * cellSize, startRow * cellSize, cellSize, cellSize);

        gc.setFill(goalColor);
        gc.fillRect(goalCol * cellSize, goalRow * cellSize, cellSize, cellSize);
    }


    private void setupWeightsAndWalls() {
        // default weight = 1 everywhere
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                weights[r][c] = 1;
                walls[r][c] = false;
            }

        // -----------------------
        // Maze-like walls
        // -----------------------
        // horizontal wall with 2 gaps
        for (int c = 0; c < cols; c++) {
            if (c == 3 || c == 14) continue;  // openings
            walls[5][c] = true;
        }

        // vertical wall left side
        for (int r = 6; r < rows - 3; r++) {
            if (r == 10) continue; // opening
            walls[r][7] = true;
        }

        // zig-zag walls near the right
        for (int r = 2; r < rows - 2; r++) {
            if (r % 2 == 0) walls[r][12] = true;
            else walls[r][13] = true;
        }

        // -----------------------
        // Weighted terrain
        // -----------------------
        // rocky ground (cost 5, dark gray)
        for (int r = 8; r < 12; r++) {
            for (int c = 2; c < 6; c++) {
                weights[r][c] = 5;
            }
        }

        // water area (cost 10, light blue)
        for (int r = 14; r < 18; r++) {
            for (int c = 10; c < 15; c++) {
                weights[r][c] = 10;
            }
        }
    }

}
