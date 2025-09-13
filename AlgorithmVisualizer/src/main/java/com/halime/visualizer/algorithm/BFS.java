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
    private final Color pathColor = Color.PURPLE;

    // Walls
    private final boolean[][] walls = new boolean[rows][cols];

    public void run(GraphicsContext gc, double speed) {
        // setup static walls first
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
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                parentRow[i][j] = -1;
                parentCol[i][j] = -1;
            }
        }

        final int startRow = 0, startCol = 0;
        final int goalRow = rows - 1, goalCol = cols - 1;

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;
        frontier[startRow][startCol] = true;

        int[][] directions = {{0,1},{1,0},{0,-1},{-1,0}}; // right, down, left, up

        Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));

        new Thread(() -> {
            try {
                while (!queue.isEmpty()) {
                    int[] node = queue.poll();
                    int r = node[0], c = node[1];

                    frontier[r][c] = false;
                    visited[r][c] = true;

                    Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));

                    if (r == goalRow && c == goalCol) {
                        LinkedList<int[]> path = reconstructPath(parentRow, parentCol, goalRow, goalCol);
                        animatePath(gc, path, speed, startRow, startCol, goalRow, goalCol, visited, frontier, pathCells);
                        break;
                    }

                    Thread.sleep((long) (200 / Math.max(speed, 1)));

                    for (int[] dir : directions) {
                        int nr = r + dir[0];
                        int nc = c + dir[1];
                        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !visited[nr][nc] && !walls[nr][nc]) {
                            if (!frontier[nr][nc]) {
                                frontier[nr][nc] = true;
                                parentRow[nr][nc] = r;
                                parentCol[nr][nc] = c;
                                queue.add(new int[]{nr, nc});
                                Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
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
                        Thread.sleep((long) (200 / Math.max(speed, 1)));
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
                } else if (visited[r][c]) {
                    gc.setFill(exploredColor);
                } else if (frontier[r][c]) {
                    gc.setFill(frontierColor);
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
        // Horizontal barrier with gaps
        for (int c = 0; c < cols; c++) {
            if (c == 5 || c == 10 || c == 15) continue; // leave some openings
            walls[10][c] = true;
        }

        // Vertical barrier with one gap
        for (int r = 4; r < rows; r++) {
            if (r == 12) continue; // opening
            walls[r][4] = true;
        }

        // Zig-zag wall
        for (int r = 6; r < rows - 3; r++) {
            if (r % 2 == 0) {
                walls[r][14] = true;
            } else {
                walls[r][15] = true;
            }
        }
    }
}
