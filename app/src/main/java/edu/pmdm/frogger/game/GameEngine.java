package edu.pmdm.frogger.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.utils.GameAudioManager;

public class GameEngine {

    private static final String TAG = "GameEngine";

    private PlayerFrog player;
    private List<Obstacle> obstacles;
    private CollisionManager collisionManager;
    private Path path; // Camino seguro
    private GameAudioManager gam;
    private Context context;

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

    // Variable para congelar el contador de tiempo
    private Long finalElapsedTime = null;

    // NUEVO: Flag de pausa + tiempo de pausa
    private boolean isPaused = false;
    private long pauseStartTime = 0;
    private long totalPausedTime = 0;

    private Bitmap originalLifeBitmap;
    private Bitmap lifeBitmap;
    private int blinkCounter = 0;
    private static final int BLINK_DURATION = 30;

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
        this.gam = GameAudioManager.getInstance();
        this.context = context;

        originalLifeBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.frog_life);
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

        int lifeSize = (int) (mapHeight * 0.06f);

        lifeBitmap = Bitmap.createScaledBitmap(originalLifeBitmap, lifeSize, lifeSize, true);

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
        finalElapsedTime = null;

        // Establecer el tiempo según el nivel
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
            int baseSpeed = rand.nextInt(6) + 3;
            int newSpeed = (int) (baseSpeed * speedMultiplier);
            car.setSpeed(newSpeed);
            obstacles.add(car);
        }
    }

    public void resetAfterDeath() {
        resetObstacles();
        frogLineIndex = 0;
        frogColumnIndex = 2;
        float frogScaledWidth = player.getScaledWidth();
        float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);
        float frogY = frogLines[frogLineIndex] * mapHeight;
        player.storeInitialPosition((int) frogX, (int) frogY);

        Path.PathConfig config = Path.getPathConfigForLevel(level);
        path = new Path(player.context, screenWidth, mapHeight, config);

        levelStartTime = System.currentTimeMillis();
        finalElapsedTime = null;
        lostByTime = false;
        lives = 3;
        if (listener != null) {
            listener.onButtonsBlocked(false);
        }
    }

    /**
     * Lógica principal de update: si no está en pausa, calculamos el tiempo y actualizamos obstáculos/rana.
     */
    public void update() {
        if (gameWon || gameOver) return;
        if (isPaused) {
            // Si está en pausa, no actualizamos nada
            return;
        }

        long now = System.currentTimeMillis();
        // Ajustamos el tiempo transcurrido restando el totalPausedTime
        long elapsed = (now - levelStartTime) - totalPausedTime;

        if (elapsed >= levelTimeLimit) {
            lostByTime = true;
            gameOver = true;
            finalElapsedTime = levelTimeLimit;
            if (listener != null) {
                listener.onGameLost();
            }
            return;
        }

        // Actualizamos la rana y obstáculos
        player.update();
        for (Obstacle obstacle : obstacles) {
            obstacle.update();
        }

        // Comprobación de colisiones
        if (!player.isDead()) {
            for (Obstacle obstacle : obstacles) {
                if (collisionManager.checkCollision(player, obstacle)) {
                    gam.playerDeath(context);
                    lives--;
                    if (lives > 0) {
                        if (listener != null) listener.onButtonsBlocked(true);
                        player.playDeathAnimation();
                    } else {
                        gameOver = true;
                        if (finalElapsedTime == null) {
                            finalElapsedTime = (now - levelStartTime) - totalPausedTime;
                        }
                        if (listener != null) listener.onGameLost();
                    }
                    break;
                }
            }
        }

        // Verificar si se recoge la llave
        if (!player.isDead()) {
            path.checkKeyCollected(player);
        }

        // Comprobación en la zona del camino (agua/arena/espacio)
        if (!player.isDead()) {
            float pathTop = pathLines[pathLines.length - 1] * mapHeight;
            float pathBottom = pathLines[0] * mapHeight;
            float frogFootY = player.getBoundingBox().bottom;
            if (frogFootY >= pathTop && frogFootY <= pathBottom && !path.isFrogSafe(player)) {
                lives--;
                if (level == 1) {
                    gam.playerDrowned(context);
                } else if (level == 2) {
                    gam.playerSand(context);
                } else if (level == 3) {
                    gam.playerFell(context);
                }
                if (lives > 0) {
                    if (listener != null) listener.onButtonsBlocked(true);
                    player.playDeathAnimation();
                } else {
                    gameOver = true;
                    if (finalElapsedTime == null) {
                        finalElapsedTime = (now - levelStartTime) - totalPausedTime;
                    }
                    if (listener != null) listener.onGameLost();
                }
            }
        }
    }

    public void draw(Canvas canvas) {
        // Dibujar el camino
        if (path != null) {
            path.draw(canvas);
        }
        // Rana
        player.draw(canvas);
        // Obstáculos
        for (Obstacle obstacle : obstacles) {
            obstacle.draw(canvas);
        }
        // Vidas
        drawLives(canvas);
    }

    private void drawLives(Canvas canvas) {
        int lifeSpacing = 10;
        int lifeSize = lifeBitmap.getWidth();
        int startX = 20;
        int startY = 20;

        for (int i = 0; i < lives; i++) {
            int lifeX = startX + i * (lifeSize + lifeSpacing);
            int lifeY = startY;

            // Parpadeo de la última vida
            if (i == lives - 1 && (blinkCounter / (BLINK_DURATION / 2)) % 2 == 0) {
                continue;
            }
            canvas.drawBitmap(lifeBitmap, lifeX, lifeY, null);
        }

        if (!isPaused) {
            blinkCounter++;
            if (blinkCounter >= BLINK_DURATION) {
                blinkCounter = 0;
            }
        }
    }

    // --- Métodos de movimiento de la rana ---
    public void movePlayerUp() {
        if (gameWon || gameOver || isPaused) return;
        if (frogLineIndex < frogLines.length - 1) {
            frogLineIndex++;
            float frogScaledWidth = player.getScaledWidth();
            float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);
            float frogY = frogLines[frogLineIndex] * mapHeight;
            player.setPosition((int) frogX, (int) frogY);
            player.moveUpSmall();
            if (frogLineIndex == frogLines.length - 1) {
                gameWon = true;
                long now = System.currentTimeMillis();
                finalElapsedTime = (now - levelStartTime) - totalPausedTime;
                Log.d(TAG, "¡Victoria! La rana ha llegado arriba.");
                boolean shouldIncrementLevel = (level == userCurrentLevel);
                if (listener != null) listener.onGameWon(shouldIncrementLevel);
            }
        }
    }

    public void movePlayerLeft() {
        if (gameWon || gameOver || isPaused) return;
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
        if (gameWon || gameOver || isPaused) return;
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
        if (gameWon || gameOver || isPaused) return;
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
        long elapsed = getFinalElapsedTime();
        long remaining = Math.max(levelTimeLimit - elapsed, 0);
        return remaining / (float) levelTimeLimit;
    }

    /**
     * Retorna el tiempo final transcurrido (congelado) o el tiempo actual si aún no se ha congelado.
     * Ajustado por totalPausedTime para que no corra el tiempo durante la pausa.
     */
    public long getFinalElapsedTime() {
        if (finalElapsedTime != null) {
            return finalElapsedTime;
        } else {
            long now = System.currentTimeMillis();
            return Math.max(0, (now - levelStartTime) - totalPausedTime);
        }
    }

    public long getLevelTimeLimit() {
        return levelTimeLimit;
    }

    public boolean isGameWon() {
        return gameWon;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isLostByTime() {
        return lostByTime;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        if (this.isPaused == paused) return;
        this.isPaused = paused;
        if (paused) {
            // Empezamos a contar la pausa
            pauseStartTime = System.currentTimeMillis();
        } else {
            // Reanudamos: sumamos el tiempo que ha durado la pausa
            long now = System.currentTimeMillis();
            totalPausedTime += (now - pauseStartTime);
        }
    }
}