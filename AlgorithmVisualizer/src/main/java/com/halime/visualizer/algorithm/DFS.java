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

    public void run(GraphicsContext gc, double speed) {
        setupWalls();

        final int canvasWidth = cols * cellSize;
        final int canvasHeight = rows * cellSize;

        Canvas canvas = gc.getCanvas();
        Platform.runLater(() -> {
            canvas.setWidth(canvasWidth);
            canvas.setHeight(canvasHeight);
        });

        final boolean[][] visited = new boolean[rows][cols];
        final boolean[][] frontier = new boolean[rows][cols];    // used to show current head only
        final boolean[][] pathCells = new boolean[rows][cols];  // keep final path visible

        final int[][] parentRow = new int[rows][cols];
        final int[][] parentCol = new int[rows][cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++) {
                parentRow[i][j] = -1;
                parentCol[i][j] = -1;
            }

        final int startRow = 0, startCol = 0;
        final int goalRow = rows - 1, goalCol = cols - 1;

        // initial draw
        Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));

        new Thread(() -> {
            try {
                boolean found = dfsExplore(startRow, startCol, gc, speed,
                        visited, frontier, pathCells, parentRow, parentCol, goalRow, goalCol);
                if (found) {
                    LinkedList<int[]> path = reconstructPath(parentRow, parentCol, goalRow, goalCol);
                    animatePath(gc, path, speed, startRow, startCol, goalRow, goalCol, visited, frontier, pathCells);
                } else {
                    // final draw if not found
                    Platform.runLater(() -> drawGrid(gc, visited, frontier, startRow, startCol, goalRow, goalCol, pathCells));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Recursive DFS that animates forward movement and backtracking step-by-step.
     * Returns true if goal was found (so caller can reconstruct path).
     */
    private boolean dfsExplore(int r, int c,
                               GraphicsContext gc, double speed,
                               boolean[][] visited, boolean[][] frontier, boolean[][] pathCells,
                               int[][] parentRow, int[][] parentCol,
                               int goalRow, int goalCol) throws InterruptedException {

        // If already visited (may be pushed multiple times), skip
        if (visited[r][c]) return false;

        // Mark current as visited and show it as the single frontier/head
        visited[r][c] = true;
        frontier[r][c] = true;
        Platform.runLater(() -> drawGrid(gc, visited, frontier, 0, 0, goalRow, goalCol, pathCells));
        Thread.sleep((long) (160 / Math.max(speed, 1)));

        // Check goal
        if (r == goalRow && c == goalCol) {
            // Leave frontier true for the caller to handle final drawing/animation
            return true;
        }

        int[][] directions = {{1,0},{0,1},{0,-1},{-1,0}}; // down, right, left, up

        // Explore neighbors in LIFO order (depth-first)
        for (int[] dir : directions) {
            int nr = r + dir[0];
            int nc = c + dir[1];
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
            if (visited[nr][nc] || walls[nr][nc]) continue;

            // Record parent for path reconstruction
            parentRow[nr][nc] = r;
            parentCol[nr][nc] = c;

            // Move forward visually: clear frontier on current so it becomes green,
            // then recurse into neighbor which will set frontier on the neighbor.
            frontier[r][c] = false;
            Platform.runLater(() -> drawGrid(gc, visited, frontier, 0, 0, goalRow, goalCol, pathCells));
            Thread.sleep((long) (120 / Math.max(speed, 1)));

            boolean found = dfsExplore(nr, nc, gc, speed, visited, frontier, pathCells, parentRow, parentCol, goalRow, goalCol);
            if (found) {
                // If child found the goal we bubble up immediately (do not re-mark frontier on this node)
                return true;
            }

            // Child finished and didn't find goal -> animate backtracking: set this node as frontier/head again
            frontier[r][c] = true;
            Platform.runLater(() -> drawGrid(gc, visited, frontier, 0, 0, goalRow, goalCol, pathCells));
            Thread.sleep((long) (120 / Math.max(speed, 1)));
        }

        // All neighbors tried, none found -> remove frontier and let caller continue/backtrack
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
        // IMPORTANT: frontier must be drawn *before* visited so frontier (blue) overlays visited (green)
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = c * cellSize;
                double y = r * cellSize;

                if (pathCells[r][c]) {
                    gc.setFill(pathColor);
                } else if (walls[r][c]) {
                    gc.setFill(wallColor);
                } else if (frontier[r][c]) {         // <-- frontier before visited
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

        // draw start and goal on top
        gc.setFill(startColor);
        gc.fillRect(startCol * cellSize, startRow * cellSize, cellSize, cellSize);

        gc.setFill(goalColor);
        gc.fillRect(goalCol * cellSize, goalRow * cellSize, cellSize, cellSize);
    }

    private void setupWalls() {
        // reuse the BFS layout for now (can customize later)
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
