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

/**
 * {@code GameEngine} gestiona la lógica principal del juego Frogger.
 * Se encarga de actualizar la posición de la rana, de los obstáculos, gestionar colisiones,
 * controlar el tiempo, manejar la animación de muerte, las vidas y determinar el fin del juego.
 */
public class GameEngine {

    // Tag para mensajes de log
    private static final String TAG = "GameEngine";

    // Instancia del jugador (rana)
    private PlayerFrog player;
    // Lista de obstáculos (por ejemplo, coches)
    private List<Obstacle> obstacles;
    // Gestor de colisiones entre objetos del juego
    private CollisionManager collisionManager;
    // Camino seguro o "path" que representa la zona protegida
    private Path path;
    // Gestor de audio para reproducir efectos y sonidos
    private GameAudioManager gam;
    // Contexto de la aplicación
    private Context context;

    // Líneas verticales que indican las posiciones de la rana en pantalla
    private final float[] frogLines = generateLines(0.92f, 0.02f, 13);
    // Líneas para la zona de obstáculos (carretera)
    private final float[] roadLines = generateLines(0.82f, 0.52f, 5);
    // Líneas para la zona del camino seguro
    private final float[] pathLines = generateLines(0.43f, 0.08f, 5);

    // Índices que controlan la posición de la rana en las líneas y columnas
    private int frogLineIndex = 0;
    private int[] columnsX = new int[5];
    private int frogColumnIndex = 2;

    // Dimensiones de la pantalla y del mapa
    private int screenWidth;
    private int mapHeight;

    // Vidas del jugador y estados de victoria/derrota
    private int lives = 3;
    private boolean gameWon = false;
    private boolean gameOver = false;
    // Nivel actual y nivel del usuario registrado
    private int level;
    private int userCurrentLevel;
    // Listener para eventos de juego (victoria, derrota, etc.)
    private GameEventsListener listener;

    // Variables para el control del tiempo
    private long levelTimeLimit;   // Límite de tiempo del nivel en milisegundos
    private long levelStartTime;   // Tiempo de inicio del nivel
    private boolean lostByTime = false; // Indica si se perdió por agotar el tiempo

    // Variables para gestionar la pausa del juego
    private boolean isPaused = false;
    private long pauseStartTime = 0;
    private long totalPausedTime = 0;

    // Bitmap para representar las vidas (frog life icon)
    private Bitmap originalLifeBitmap;
    private Bitmap lifeBitmap;
    // Contador para el efecto de parpadeo de la última vida
    private int blinkCounter = 0;
    private static final int BLINK_DURATION = 30;

    // Tiempo final transcurrido (cuando el juego se detiene) o nulo si aún sigue corriendo
    private Long finalElapsedTime = null;

    /**
     * Constructor de GameEngine.
     *
     * @param context          Contexto de la aplicación.
     * @param level            Nivel actual del juego.
     * @param userCurrentLevel Nivel actual registrado para el usuario.
     * @param listener         Listener para los eventos del juego.
     */
    public GameEngine(Context context, int level, int userCurrentLevel, GameEventsListener listener) {
        this.collisionManager = new CollisionManager();
        this.player = new PlayerFrog(context);
        // Registrar listener para la animación de muerte de la rana
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
        this.gam = GameAudioManager.getInstance(context);
        this.context = context;

        // Cargar el bitmap original que representa una vida
        originalLifeBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.frog_life);
    }

    /**
     * Genera un arreglo de líneas distribuidas uniformemente entre un valor inicial y final.
     *
     * @param start Valor inicial.
     * @param end   Valor final.
     * @param count Número de líneas a generar.
     * @return Arreglo de valores flotantes que representan las posiciones de las líneas.
     */
    private float[] generateLines(float start, float end, int count) {
        float[] lines = new float[count];
        float step = (start - end) / (count - 1);
        for (int i = 0; i < count; i++) {
            lines[i] = start - i * step;
        }
        return lines;
    }

    /**
     * Configura las posiciones y escalas de la rana, las columnas y el tamaño de las vidas.
     *
     * @param screenWidth Ancho de la pantalla.
     * @param mapHeight   Altura del mapa.
     */
    public void configurePositions(int screenWidth, int mapHeight) {
        this.screenWidth = screenWidth;
        this.mapHeight = mapHeight;

        // Calcular el tamaño de la imagen de vida basándose en la altura del mapa
        int lifeSize = (int) (mapHeight * 0.06f);
        // Escalar el bitmap de la vida
        lifeBitmap = Bitmap.createScaledBitmap(originalLifeBitmap, lifeSize, lifeSize, true);

        // Configurar la escala del jugador (rana) basada en el mapa
        player.configureScale(mapHeight, 0.06f);

        // Dividir la pantalla en 5 columnas
        int columnWidth = screenWidth / 5;
        for (int i = 0; i < 5; i++) {
            columnsX[i] = i * columnWidth + (columnWidth / 2);
        }

        // Posicionar la rana en la posición inicial (centro de la columna 3 y primera línea)
        frogLineIndex = 0;
        frogColumnIndex = 2;
        float frogScaledWidth = player.getScaledWidth();
        float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);
        float frogY = frogLines[frogLineIndex] * mapHeight;
        player.storeInitialPosition((int) frogX, (int) frogY);

        // Reiniciar obstáculos y crear el camino seguro
        resetObstacles();

        // Obtener la configuración del camino para el nivel actual y crear el camino seguro
        Path.PathConfig config = Path.getPathConfigForLevel(level);
        path = new Path(player.context, screenWidth, mapHeight, config);

        // Reiniciar estados del juego
        gameWon = false;
        gameOver = false;
        lives = 3;
        lostByTime = false;
        finalElapsedTime = null;

        // Establecer el límite de tiempo según el nivel
        if (level == 1) {
            levelTimeLimit = 60000;
        } else if (level == 2) {
            levelTimeLimit = 45000;
        } else if (level == 3) {
            levelTimeLimit = 30000;
        } else {
            levelTimeLimit = 60000; // valor por defecto
        }
        levelStartTime = System.currentTimeMillis();
    }

    /**
     * Reinicia la lista de obstáculos. Crea nuevos obstáculos aleatorios con velocidades ajustadas
     * según el nivel actual.
     */
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

        // Ajustar el multiplicador de velocidad según el nivel
        float speedMultiplier = 1.35f;
        if (level == 2) {
            speedMultiplier = 1.7f;
        } else if (level == 3) {
            speedMultiplier = 2.0f;
        }

        // Crear un obstáculo para cada línea de la carretera
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

    /**
     * Reinicia el estado del juego después de la muerte de la rana.
     * Reinicia obstáculos, posiciones de la rana y el camino seguro.
     */
    public void resetAfterDeath() {
        resetObstacles();
        frogLineIndex = 0;
        frogColumnIndex = 2;
        float frogScaledWidth = player.getScaledWidth();
        float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);
        float frogY = frogLines[frogLineIndex] * mapHeight;
        player.storeInitialPosition((int) frogX, (int) frogY);

        // Reiniciar el camino seguro basado en la configuración del nivel
        Path.PathConfig config = Path.getPathConfigForLevel(level);
        path = new Path(player.context, screenWidth, mapHeight, config);

        levelStartTime = System.currentTimeMillis();
        finalElapsedTime = null;
        lostByTime = false;
        if (listener != null) {
            listener.onButtonsBlocked(false);
        }
    }

    /**
     * Actualiza la lógica del juego. Si el juego no está en pausa y no ha terminado,
     * actualiza el tiempo, la rana, los obstáculos, comprueba colisiones y verifica
     * condiciones de victoria o derrota.
     */
    public void update() {
        if (gameWon || gameOver) return;
        if (isPaused) {
            // No se actualiza nada si el juego está en pausa
            return;
        }

        long now = System.currentTimeMillis();
        // Calcular el tiempo transcurrido ajustado por el tiempo total en pausa
        long elapsed = (now - levelStartTime) - totalPausedTime;

        // Verificar si se ha agotado el tiempo del nivel
        if (elapsed >= levelTimeLimit) {
            lostByTime = true;
            gameOver = true;
            finalElapsedTime = levelTimeLimit;
            if (listener != null) {
                listener.onGameLost();
            }
            return;
        }

        // Actualizar la rana y cada obstáculo
        player.update();
        for (Obstacle obstacle : obstacles) {
            obstacle.update();
        }

        // Comprobar colisiones entre la rana y los obstáculos
        if (!player.isDead()) {
            for (Obstacle obstacle : obstacles) {
                if (collisionManager.checkCollision(player, obstacle)) {
                    gam.playerDeath(context);
                    lives--;
                    Log.d(TAG, "Colisión detectada. Vidas restantes: " + lives);
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

        // Verificar si la llave ha sido recogida en el camino seguro
        if (!player.isDead()) {
            path.checkKeyCollected(player);
        }

        // Comprobar si la rana se encuentra en la zona del camino (por ejemplo, agua, arena o espacio)
        if (!player.isDead()) {
            float pathTop = pathLines[pathLines.length - 1] * mapHeight;
            float pathBottom = pathLines[0] * mapHeight;
            float frogFootY = player.getBoundingBox().bottom;
            if (frogFootY >= pathTop && frogFootY <= pathBottom && !path.isFrogSafe(player)) {
                lives--;
                Log.d(TAG, "Colisión en zona de camino. Vidas restantes: " + lives);
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

    /**
     * Dibuja los elementos del juego en el canvas: camino, rana, obstáculos y vidas.
     *
     * @param canvas Canvas donde se realiza el dibujo.
     */
    public void draw(Canvas canvas) {
        // Dibujar el camino seguro
        if (path != null) {
            path.draw(canvas);
        }
        // Dibujar la rana
        player.draw(canvas);
        // Dibujar cada obstáculo
        for (Obstacle obstacle : obstacles) {
            obstacle.draw(canvas);
        }
        // Dibujar las vidas restantes en la pantalla
        drawLives(canvas);
    }

    /**
     * Dibuja las vidas (íconos) en la parte superior izquierda de la pantalla.
     * Aplica un efecto de parpadeo a la última vida.
     *
     * @param canvas Canvas donde se dibujan las vidas.
     */
    private void drawLives(Canvas canvas) {
        int lifeSpacing = 10;
        int lifeSize = lifeBitmap.getWidth();
        int startX = 20;
        int startY = 20;

        for (int i = 0; i < lives; i++) {
            int lifeX = startX + i * (lifeSize + lifeSpacing);
            int lifeY = startY;

            // Efecto de parpadeo en la última vida
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

    /**
     * Mueve la rana hacia arriba. Si llega a la última línea, se considera victoria.
     */
    public void movePlayerUp() {
        if (gameWon || gameOver || isPaused) return;
        if (frogLineIndex < frogLines.length - 1) {
            frogLineIndex++;
            float frogScaledWidth = player.getScaledWidth();
            float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);
            float frogY = frogLines[frogLineIndex] * mapHeight;
            player.setPosition((int) frogX, (int) frogY);
            player.moveUpSmall();
            // Si la rana alcanza la última línea, se declara victoria
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

    /**
     * Mueve la rana hacia la izquierda.
     */
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

    /**
     * Mueve la rana hacia la derecha.
     */
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

    /**
     * Mueve la rana hacia abajo.
     */
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
     * Devuelve la proporción de tiempo restante del nivel (valor entre 0 y 1)
     * para usar en la barra de progreso.
     *
     * @return Proporción de tiempo restante.
     */
    public float getTimeRatio() {
        long elapsed = getFinalElapsedTime();
        long remaining = Math.max(levelTimeLimit - elapsed, 0);
        return remaining / (float) levelTimeLimit;
    }

    /**
     * Retorna el tiempo final transcurrido (congelado) si ya se ha determinado,
     * o el tiempo actual transcurrido ajustado por las pausas si aún sigue corriendo.
     *
     * @return Tiempo transcurrido en milisegundos.
     */
    public long getFinalElapsedTime() {
        if (finalElapsedTime != null) {
            return finalElapsedTime;
        } else {
            long now = System.currentTimeMillis();
            return Math.max(0, (now - levelStartTime) - totalPausedTime);
        }
    }

    /**
     * Devuelve el límite de tiempo del nivel en milisegundos.
     *
     * @return Límite de tiempo del nivel.
     */
    public long getLevelTimeLimit() {
        return levelTimeLimit;
    }

    /**
     * Indica si el juego ha sido ganado.
     *
     * @return {@code true} si se ha ganado, {@code false} de lo contrario.
     */
    public boolean isGameWon() {
        return gameWon;
    }

    /**
     * Indica si el juego ha terminado por derrota.
     *
     * @return {@code true} si el juego ha terminado, {@code false} de lo contrario.
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * Indica si el juego se ha perdido por agotar el tiempo.
     *
     * @return {@code true} si se perdió por tiempo, {@code false} de lo contrario.
     */
    public boolean isLostByTime() {
        return lostByTime;
    }

    /**
     * Indica si el juego está en pausa.
     *
     * @return {@code true} si está en pausa, {@code false} en caso contrario.
     */
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Establece el estado de pausa del juego. Al pausar, se registra el tiempo de inicio de la pausa.
     * Al reanudar, se suma el tiempo de pausa al total pausado.
     *
     * @param paused {@code true} para pausar el juego, {@code false} para reanudar.
     */
    public void setPaused(boolean paused) {
        if (this.isPaused == paused) return;
        this.isPaused = paused;
        if (paused) {
            // Iniciar el contador de la pausa
            pauseStartTime = System.currentTimeMillis();
        } else {
            // Al reanudar, sumar el tiempo de la pausa al total pausado
            long now = System.currentTimeMillis();
            totalPausedTime += (now - pauseStartTime);
        }
    }
}