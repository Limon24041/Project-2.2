package com.battlearena;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BattleArenaFX extends Application {

    private final double width = 900;
    private final double height = 560;

    private double playerHP = 100;
    private double enemyHP = 100;

    private final double playerX = 145;
    private final double enemyX = 740;
    private final double groundY = 420;

    private boolean gameOver = false;
    private boolean playerWon = false;
    private boolean enemyDying = false;

    private long lastEnemyAttack = 0;
    private long playerHitUntil = 0;
    private long enemyHitUntil = 0;
    private long enemyDeathStart = 0;

    private final List<Projectile> projectiles = new ArrayList<>();
    private GraphicsContext gc;

    private static class Projectile {
        double x;
        double y;
        double speed;
        boolean fromPlayer;

        Projectile(double x, double y, double speed, boolean fromPlayer) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.fromPlayer = fromPlayer;
        }
    }

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, width, height);

        canvas.setFocusTraversable(true);

        canvas.setOnMouseClicked(event -> {
            if (gameOver) {
                resetGame();
            } else {
                shoot();
            }
            canvas.requestFocus();
        });

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE && !gameOver) {
                shoot();
            }

            if (event.getCode() == KeyCode.R) {
                resetGame();
            }
        });

        stage.setTitle("Battle Arena 2D");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        canvas.requestFocus();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update(now);
                render(now);
            }
        };

        timer.start();
    }

    private void shoot() {
        if (!gameOver && !enemyDying) {
            projectiles.add(
                    new Projectile(playerX + 35, groundY - 75, 7, true)
            );
        }
    }

    private void enemyShoot() {
        if (!gameOver && !enemyDying) {
            projectiles.add(
                    new Projectile(enemyX - 35, groundY - 75, -4, false)
            );
        }
    }

    private void update(long now) {
        if (gameOver) {
            return;
        }

        if (enemyDying) {
            if (now - enemyDeathStart >= 1_000_000_000L) {
                gameOver = true;
                playerWon = true;
            }
            return;
        }

        if (lastEnemyAttack == 0) {
            lastEnemyAttack = now;
        }

        if (now - lastEnemyAttack > 1_500_000_000L) {
            enemyShoot();
            lastEnemyAttack = now;
        }

        Iterator<Projectile> iterator = projectiles.iterator();

        while (iterator.hasNext()) {
            Projectile p = iterator.next();
            p.x += p.speed;

            if (p.fromPlayer
                    && p.x >= enemyX - 35
                    && p.x <= enemyX + 35) {

                enemyHP = Math.max(0, enemyHP - 20);
                enemyHitUntil = now + 250_000_000L;

                iterator.remove();

                if (enemyHP <= 0) {
                    enemyDying = true;
                    enemyDeathStart = now;
                    projectiles.clear();
                    break;
                }

                continue;
            }

            if (!p.fromPlayer
                    && p.x >= playerX - 30
                    && p.x <= playerX + 30) {

                playerHP = Math.max(0, playerHP - 10);
                playerHitUntil = now + 250_000_000L;

                iterator.remove();
                continue;
            }

            if (p.x < 0 || p.x > width) {
                iterator.remove();
            }
        }

        if (playerHP <= 0) {
            playerHP = 0;
            gameOver = true;
            playerWon = false;
        }
    }

    private void resetGame() {
        playerHP = 100;
        enemyHP = 100;

        gameOver = false;
        playerWon = false;
        enemyDying = false;

        lastEnemyAttack = 0;
        playerHitUntil = 0;
        enemyHitUntil = 0;
        enemyDeathStart = 0;

        projectiles.clear();
    }

    private void render(long now) {
        drawBackground();
        drawHealthBars();

        drawWarrior(playerX, groundY, now < playerHitUntil);

        if (!enemyDying) {
            drawEnemy(enemyX, groundY, now < enemyHitUntil);
        } else {
            drawEnemyDeath(enemyX, groundY, now);
        }

        drawProjectiles();
        drawInstructions();

        if (gameOver) {
            drawGameOver();
        }
    }

    private void drawBackground() {
        gc.setFill(Color.rgb(20, 27, 50));
        gc.fillRect(0, 0, width, height);

        gc.setFill(Color.rgb(39, 56, 77));
        gc.fillOval(660, 45, 100, 100);

        gc.setFill(Color.rgb(45, 65, 66));
        gc.fillOval(-100, 280, 400, 220);
        gc.fillOval(250, 300, 400, 200);
        gc.fillOval(580, 270, 450, 250);

        gc.setFill(Color.rgb(58, 105, 58));
        gc.fillRect(0, groundY, width, height - groundY);

        gc.setFill(Color.rgb(75, 135, 62));
        gc.fillRect(0, groundY, width, 12);

        gc.setFill(Color.rgb(90, 75, 60));
        gc.fillRect(
                0,
                groundY + 12,
                width,
                height - groundY - 12
        );

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 22));
        gc.fillText("BATTLE ARENA", 345, 38);
    }

    private void drawHealthBars() {
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 17));

        gc.fillText("WARRIOR", 70, 75);
        gc.fillText("GOBLIN", 690, 75);

        gc.setFill(Color.DARKGRAY);
        gc.fillRoundRect(70, 85, 200, 24, 8, 8);
        gc.fillRoundRect(690, 85, 150, 24, 8, 8);

        gc.setFill(Color.LIMEGREEN);
        gc.fillRoundRect(
                70, 85, 200 * playerHP / 100, 24, 8, 8
        );

        gc.setFill(Color.ORANGERED);
        gc.fillRoundRect(
                690, 85, 150 * enemyHP / 100, 24, 8, 8
        );

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 14));

        gc.fillText((int) playerHP + " / 100", 135, 103);
        gc.fillText((int) enemyHP + " / 100", 735, 103);
    }

    private void drawWarrior(double x, double y, boolean hit) {
        Color bodyColor = hit
                ? Color.ORANGERED
                : Color.DODGERBLUE;

        if (hit) {
            gc.setFill(Color.rgb(255, 60, 60, 0.25));
            gc.fillOval(x - 38, y - 145, 76, 150);
        }

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(8);

        gc.strokeLine(x, y - 45, x - 12, y);
        gc.strokeLine(x, y - 45, x + 15, y);

        gc.setStroke(bodyColor);
        gc.setLineWidth(15);
        gc.strokeLine(x, y - 95, x, y - 48);

        gc.setFill(Color.PEACHPUFF);
        gc.fillOval(x - 16, y - 135, 32, 32);

        gc.setFill(Color.SILVER);
        gc.fillRoundRect(x - 20, y - 143, 40, 15, 6, 6);

        gc.setStroke(bodyColor);
        gc.setLineWidth(10);
        gc.strokeLine(x, y - 85, x + 30, y - 70);

        gc.setStroke(Color.SILVER);
        gc.setLineWidth(5);
        gc.strokeLine(x + 25, y - 72, x + 48, y - 78);

        gc.setFill(Color.GOLD);
        gc.fillOval(x + 40, y - 83, 12, 12);
    }

    private void drawEnemy(double x, double y, boolean hit) {
        if (hit) {
            gc.setFill(Color.rgb(255, 40, 40, 0.3));
            gc.fillOval(x - 42, y - 145, 84, 150);
        }

        gc.setFill(
                hit ? Color.YELLOWGREEN : Color.FORESTGREEN
        );
        gc.fillOval(x - 22, y - 135, 44, 42);

        gc.setFill(
                hit ? Color.LIMEGREEN : Color.DARKGREEN
        );
        gc.fillRoundRect(x - 28, y - 100, 56, 48, 12, 12);

        gc.setFill(Color.WHITE);
        gc.fillOval(x - 13, y - 120, 10, 10);
        gc.fillOval(x + 5, y - 120, 10, 10);

        gc.setFill(Color.RED);
        gc.fillOval(x - 10, y - 117, 5, 5);
        gc.fillOval(x + 8, y - 117, 5, 5);

        gc.setStroke(Color.DARKGREEN);
        gc.setLineWidth(10);

        gc.strokeLine(x - 10, y - 55, x - 20, y);
        gc.strokeLine(x + 10, y - 55, x + 20, y);
        gc.strokeLine(x - 20, y - 90, x - 42, y - 70);
        gc.strokeLine(x + 20, y - 90, x + 35, y - 75);
    }

    private void drawEnemyDeath(double x, double y, long now) {
        double progress = Math.min(
                1.0,
                (now - enemyDeathStart) / 1_000_000_000.0
        );

        double tilt = progress * 75;
        double drop = progress * 35;

        gc.save();

        gc.translate(x, y - 65 + drop);
        gc.rotate(tilt);

        gc.setFill(Color.FORESTGREEN);
        gc.fillOval(-22, -70, 44, 42);

        gc.setFill(Color.DARKGREEN);
        gc.fillRoundRect(-28, -35, 56, 48, 12, 12);

        gc.setFill(Color.WHITE);
        gc.fillOval(-13, -55, 10, 10);
        gc.fillOval(5, -55, 10, 10);

        gc.setFill(Color.RED);
        gc.fillOval(-10, -52, 5, 5);
        gc.fillOval(8, -52, 5, 5);

        gc.setStroke(Color.DARKGREEN);
        gc.setLineWidth(10);

        gc.strokeLine(-10, 10, -20, 55);
        gc.strokeLine(10, 10, 20, 55);
        gc.strokeLine(-20, -25, -42, -5);
        gc.strokeLine(20, -25, 35, -10);

        gc.restore();

        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial", 20));
        gc.fillText("HIT!", x - 22, y - 155 - progress * 15);
    }

    private void drawProjectiles() {
        for (Projectile p : projectiles) {
            if (p.fromPlayer) {
                gc.setFill(Color.GOLD);
                gc.fillOval(p.x - 8, p.y - 8, 18, 18);

                gc.setFill(Color.ORANGE);
                gc.fillOval(p.x - 4, p.y - 4, 10, 10);
            } else {
                gc.setFill(Color.RED);
                gc.fillOval(p.x - 9, p.y - 9, 18, 18);

                gc.setFill(Color.ORANGERED);
                gc.fillOval(p.x - 4, p.y - 4, 10, 10);
            }
        }
    }

    private void drawInstructions() {
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 16));

        gc.fillText(
                "CLICK anywhere or press SPACE to attack!",
                245, 490
        );

        gc.fillText(
                "Each hit deals 20 damage. Enemy attacks automatically.",
                205, 518
        );

        gc.fillText("Press R to restart.", 390, 545);
    }

    private void drawGameOver() {
        gc.setFill(Color.rgb(0, 0, 0, 0.75));
        gc.fillRoundRect(220, 180, 460, 170, 25, 25);

        gc.setFill(playerWon ? Color.LIME : Color.ORANGERED);
        gc.setFont(Font.font("Arial", 40));

        gc.fillText(
                playerWon ? "YOU WIN!" : "YOU LOSE!",
                335, 245
        );

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 18));
        gc.fillText("Click to play again or press R", 315, 295);
    }

    public static void main(String[] args) {
        launch(args);
    }
}