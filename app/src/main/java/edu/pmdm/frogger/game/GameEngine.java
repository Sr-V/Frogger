package edu.pmdm.frogger.game;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import edu.pmdm.frogger.R;

public class GameEngine {

    private static final String TAG = "GameEngine";

    private PlayerFrog player;
    private List<Obstacle> obstacles;
    private CollisionManager collisionManager;
    private Path path; // Camino seguro

    // Líneas para la rana
    private final float[] frogLines = generateLines(0.92f, 0.02f, 13);
    // Líneas para obstáculos (carretera)
    private final float[] roadLines = generateLines(0.82f, 0.52f, 5);
    // Líneas para la zona del camino
    private final float[] pathLines = generateLines(0.43f, 0.08f, 5);

    private int frogLineIndex = 0;
    private int[] columnsX = new int[5];
    private int frogColumnIndex = 2;

    private int screenWidth;
    private int mapHeight;

    private int lives = 3;
    private boolean gameWon = false;
    private boolean gameOver = false;
    private int level;
    private int userCurrentLevel;
    private GameEventsListener listener;

    // Variables para el control del tiempo
    private long levelTimeLimit;   // en milisegundos
    private long levelStartTime;   // tiempo de inicio del nivel
    private boolean lostByTime = false; // Indica si se perdió por agotar el tiempo

    public GameEngine(Context context, int level, int userCurrentLevel, GameEventsListener listener) {
        this.collisionManager = new CollisionManager();
        this.player = new PlayerFrog(context);
        // Registrar el listener para cuando termine la animación de muerte:
        player.setDeathAnimationListener(new PlayerFrog.DeathAnimationListener() {
            @Override
            public void onDeathAnimationFinished() {
                resetAfterDeath();
            }
        });
        this.obstacles = new ArrayList<>();
        this.level = level;
        this.userCurrentLevel = userCurrentLevel;
        this.listener = listener;
    }

    private float[] generateLines(float start, float end, int count) {
        float[] lines = new float[count];
        float step = (start - end) / (count - 1);
        for (int i = 0; i < count; i++) {
            lines[i] = start - i * step;
        }
        return lines;
    }

    public void configurePositions(int screenWidth, int mapHeight) {
        this.screenWidth = screenWidth;
        this.mapHeight = mapHeight;

        player.configureScale(mapHeight, 0.06f);

        int columnWidth = screenWidth / 5;
        for (int i = 0; i < 5; i++) {
            columnsX[i] = i * columnWidth + (columnWidth / 2);
        }

        frogLineIndex = 0;
        frogColumnIndex = 2;
        float frogScaledWidth = player.getScaledWidth();
        float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);
        float frogY = frogLines[frogLineIndex] * mapHeight;
        player.storeInitialPosition((int) frogX, (int) frogY);

        resetObstacles();

        // Crear el camino seguro utilizando la configuración para el nivel.
        Path.PathConfig config = Path.getPathConfigForLevel(level);
        path = new Path(player.context, screenWidth, mapHeight, config);

        gameWon = false;
        gameOver = false;
        lives = 3;
        lostByTime = false;

        // Establecer el tiempo según el nivel:
        // Nivel 1: 60 segundos, Nivel 2: 45 segundos, Nivel 3: 30 segundos.
        if (level == 1) {
            levelTimeLimit = 60000;
        } else if (level == 2) {
            levelTimeLimit = 45000;
        } else if (level == 3) {
            levelTimeLimit = 30000;
        } else {
            levelTimeLimit = 60000; // por defecto
        }
        levelStartTime = System.currentTimeMillis();
    }

    private void resetObstacles() {
        obstacles.clear();
        Random rand = new Random();

        int drawableObstacle;
        switch (level) {
            case 1:
                drawableObstacle = R.drawable.cars;
                break;
            case 2:
                drawableObstacle = R.drawable.desert_cars;
                break;
            case 3:
                drawableObstacle = R.drawable.space_cars;
                break;
            default:
                drawableObstacle = R.drawable.cars;
        }

        // Factor multiplicador de velocidad según el nivel
        float speedMultiplier = 1.35f;
        if (level == 2) {
            speedMultiplier = 1.7f;
        } else if (level == 3) {
            speedMultiplier = 2.0f;
        }

        for (float line : roadLines) {
            float carY = line * mapHeight;
            float carX = rand.nextFloat() * (screenWidth - 100);
            Obstacle car = new Obstacle(player.context, (int) carX, (int) carY, drawableObstacle);
            car.configureScale(mapHeight, 0.10f);
            car.setScreenWidth(screenWidth);
            // Calcular la velocidad base y multiplicarla
            int baseSpeed = rand.nextInt(6) + 3;
            int newSpeed = (int) (baseSpeed * speedMultiplier);
            car.setSpeed(newSpeed);
            obstacles.add(car);
        }
    }

    public void resetAfterDeath() {
        // Reinicia los obstáculos
        resetObstacles();
        // Reinicia la posición de la rana
        frogLineIndex = 0;
        frogColumnIndex = 2;
        float frogScaledWidth = player.getScaledWidth();
        float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);
        float frogY = frogLines[frogLineIndex] * mapHeight;
        player.storeInitialPosition((int) frogX, (int) frogY);
        // Reinicia el camino seguro (incluyendo la llave y piezas adicionales)
        Path.PathConfig config = Path.getPathConfigForLevel(level);
        path = new Path(player.context, screenWidth, mapHeight, config);
        // Reinicia el tiempo del nivel
        levelStartTime = System.currentTimeMillis();
        lostByTime = false;
        if (listener != null) {
            listener.onButtonsBlocked(false);
        }
    }

    public void update() {
        if (gameWon || gameOver) return;

        // Actualizar el tiempo y comprobar si se ha agotado
        long elapsed = System.currentTimeMillis() - levelStartTime;
        if (elapsed >= levelTimeLimit) {
            lostByTime = true;
            gameOver = true;
            if (listener != null) {
                listener.onGameLost();
            }
            return;
        }

        player.update();
        for (Obstacle obstacle : obstacles) {
            obstacle.update();
        }

        // Comprobación de colisión con obstáculos.
        if (!player.isDead()) {
            for (Obstacle obstacle : obstacles) {
                if (collisionManager.checkCollision(player, obstacle)) {
                    lives--;
                    if (lives > 0) {
                        if (listener != null) listener.onButtonsBlocked(true);
                        player.playDeathAnimation();
                    } else {
                        gameOver = true;
                        if (listener != null) listener.onGameLost();
                    }
                    break;
                }
            }
        }

        // Verificar si se recoge la llave (para niveles con llave).
        if (!player.isDead()) {
            path.checkKeyCollected(player);
        }

        // Comprobación en la zona del camino.
        if (!player.isDead()) {
            float pathTop = pathLines[pathLines.length - 1] * mapHeight;
            float pathBottom = pathLines[0] * mapHeight;
            float frogFootY = player.getBoundingBox().bottom;
            if (frogFootY >= pathTop && frogFootY <= pathBottom && !path.isFrogSafe(player)) {
                lives--;
                if (lives > 0) {
                    if (listener != null) listener.onButtonsBlocked(true);
                    player.playDeathAnimation();
                } else {
                    gameOver = true;
                    if (listener != null) listener.onGameLost();
                }
            }
        }
    }

    public void draw(Canvas canvas) {
        // Dibujar primero el camino seguro.
        if (path != null) {
            path.draw(canvas);
        }
        // Dibujar la rana encima del camino.
        player.draw(canvas);
        // Dibujar los obstáculos encima de la rana.
        for (Obstacle obstacle : obstacles) {
            obstacle.draw(canvas);
        }
    }

    public void movePlayerUp() {
        if (gameWon || gameOver) return;
        if (frogLineIndex < frogLines.length - 1) {
            frogLineIndex++;
            float frogScaledWidth = player.getScaledWidth();
            float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);
            float frogY = frogLines[frogLineIndex] * mapHeight;
            player.setPosition((int) frogX, (int) frogY);
            player.moveUpSmall();
            if (frogLineIndex == frogLines.length - 1) {
                gameWon = true;
                Log.d(TAG, "¡Victoria! La rana ha llegado arriba.");
                boolean shouldIncrementLevel = (level == userCurrentLevel);
                if (listener != null) listener.onGameWon(shouldIncrementLevel);
            }
        }
    }

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

    public void movePlayerDown() {
        if (gameWon || gameOver) return;
        if (frogLineIndex > 0) {
            frogLineIndex--;
            float frogScaledWidth = player.getScaledWidth();
            float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);
            float frogY = frogLines[frogLineIndex] * mapHeight;
            player.setPosition((int) frogX, (int) frogY);
            player.moveDown();
        }
    }

    /**
     * Devuelve la proporción de tiempo restante (entre 0 y 1) para usar en la barra.
     */
    public float getTimeRatio() {
        long elapsed = System.currentTimeMillis() - levelStartTime;
        long remaining = Math.max(levelTimeLimit - elapsed, 0);
        return remaining / (float) levelTimeLimit;
    }

    // Getter para saber si se perdió por tiempo
    public boolean isLostByTime() {
        return lostByTime;
    }
}