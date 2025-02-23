package edu.pmdm.frogger.game;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameEngine {

    private static final String TAG = "GameEngine";

    private PlayerFrog player;
    private List<ObstacleCar> cars;
    private CollisionManager collisionManager;

    // Carriles de coches
    private final float[] roadLines = { 0.82f, 0.74f, 0.66f, 0.58f, 0.50f };

    // Filas verticales de la rana
    private final float[] frogLines = {
            0.92f, 0.87f, 0.79f, 0.71f, 0.63f,
            0.55f, 0.47f, 0.39f, 0.31f, 0.23f,
            0.15f, 0.07f, 0.02f
    };
    private int frogLineIndex = 0;

    // 5 columnas
    private int[] columnsX = new int[5];
    private int frogColumnIndex = 2;

    private int screenWidth;
    private int mapHeight;

    // Control de vidas
    private int lives = 3;

    // Control de victoria
    private boolean gameWon = false;
    private boolean gameOver = false; // si se queda sin vidas

    // Datos para comprobar subida de nivel
    private int level;            // nivel actual que se está jugando
    private int userCurrentLevel; // nivel actual del usuario en Firebase

    // Listener para avisar a la Activity (mostrar alert, bloquear botones, etc.)
    private GameEventsListener listener;

    public GameEngine(Context context,
                      int level,
                      int userCurrentLevel,
                      GameEventsListener listener) {
        this.collisionManager = new CollisionManager();
        this.player = new PlayerFrog(context);

        // Asignamos el listener para el fin de la animación de muerte:
        // Al finalizar, se reinician los obstáculos, se reposiciona la rana y se reactivan los botones.
        this.player.setDeathAnimationListener(() -> resetAfterDeath());

        this.cars = new ArrayList<>();
        this.level = level;
        this.userCurrentLevel = userCurrentLevel;
        this.listener = listener;
    }

    public void configurePositions(int screenWidth, int mapHeight) {
        this.screenWidth = screenWidth;
        this.mapHeight   = mapHeight;

        // Escalamos la rana
        player.configureScale(mapHeight, 0.06f);

        // 5 columnas
        int columnWidth = screenWidth / 5;
        for (int i = 0; i < 5; i++) {
            columnsX[i] = i * columnWidth + (columnWidth / 2);
        }

        // Posición inicial
        frogLineIndex = 0;
        frogColumnIndex = 2;
        float frogScaledWidth = player.getScaledWidth();
        float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);
        float frogY = frogLines[frogLineIndex] * mapHeight;
        player.storeInitialPosition((int) frogX, (int) frogY);

        // Generar coches
        resetObstacles();

        // Reiniciar flags
        gameWon = false;
        gameOver = false;
        lives = 3;
    }

    /**
     * Reinicia los obstáculos del mapa.
     */
    private void resetObstacles() {
        cars.clear();
        for (float line : roadLines) {
            float carY = line * mapHeight;
            float carX = new Random().nextFloat() * (screenWidth - 100);

            ObstacleCar car = new ObstacleCar(player.context, (int) carX, (int) carY);
            car.configureScale(mapHeight, 0.10f);
            cars.add(car);
        }
    }

    /**
     * Se encarga de reiniciar la partida tras la animación de muerte:
     * reposiciona la rana, reinicia los obstáculos y desbloquea los botones.
     */
    public void resetAfterDeath() {
        resetObstacles();
        frogLineIndex = 0;
        frogColumnIndex = 2;
        float frogScaledWidth = player.getScaledWidth();
        float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);
        float frogY = frogLines[frogLineIndex] * mapHeight;
        player.storeInitialPosition((int) frogX, (int) frogY);
        if (listener != null) {
            listener.onButtonsBlocked(false);
        }
    }

    public void update() {
        if (gameWon || gameOver) return; // si ya ganamos o perdimos, no actualizar

        player.update();
        for (ObstacleCar car : cars) {
            car.update();
        }

        // Si la rana está en proceso de muerte, no comprobamos colisiones
        if (player.isDead()) {
            return;
        }

        // Colisiones
        for (ObstacleCar car : cars) {
            if (collisionManager.checkCollision(player, car)) {
                // Restar una vida
                lives--;
                if (lives > 0) {
                    // Bloquear botones hasta que la rana reaparezca
                    if (listener != null) {
                        listener.onButtonsBlocked(true);
                    }
                    // Reproducir animación de muerte en el punto de colisión
                    player.playDeathAnimation();
                } else {
                    // Sin vidas => derrota
                    gameOver = true;
                    if (listener != null) {
                        listener.onGameLost();
                    }
                }
                break; // salir del loop de coches
            }
        }
    }

    public void draw(android.graphics.Canvas canvas) {
        player.draw(canvas);
        for (ObstacleCar car : cars) {
            car.draw(canvas);
        }
    }

    // ===== MOVIMIENTO VERTICAL =====
    public void movePlayerUp() {
        if (gameWon || gameOver) return;

        if (frogLineIndex < frogLines.length - 1) {
            frogLineIndex++;
            float frogScaledWidth = player.getScaledWidth();
            float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);
            float frogY = frogLines[frogLineIndex] * mapHeight;

            player.setPosition((int) frogX, (int) frogY);
            player.moveUpSmall();

            // Comprobar si hemos llegado a la última fila
            if (frogLineIndex == frogLines.length - 1) {
                gameWon = true;
                Log.d(TAG, "¡Victoria! La rana ha llegado arriba.");

                // 1) Comprobar si hay que incrementar nivel en Firebase
                boolean shouldIncrementLevel = (level == userCurrentLevel);

                // 2) Llamar al listener para informar del fin de partida
                if (listener != null) {
                    listener.onGameWon(shouldIncrementLevel);
                }
            }
        }
    }

    // ===== MOVIMIENTOS HORIZONTALES =====
    public void movePlayerLeft() {
        if (gameWon || gameOver) return;
        if (frogColumnIndex > 0) {
            frogColumnIndex--;
            float frogScaledWidth = player.getScaledWidth();
            float frogY = player.y;
            float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);

            player.setPosition((int) frogX, (int) frogY);
            player.moveLeft();
        }
    }

    public void movePlayerRight() {
        if (gameWon || gameOver) return;
        if (frogColumnIndex < columnsX.length - 1) {
            frogColumnIndex++;
            float frogScaledWidth = player.getScaledWidth();
            float frogY = player.y;
            float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);

            player.setPosition((int) frogX, (int) frogY);
            player.moveRight();
        }
    }
}