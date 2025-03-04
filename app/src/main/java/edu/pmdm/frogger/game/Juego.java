package edu.pmdm.frogger.game;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.res.ResourcesCompat;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.activities.MainActivity;
import edu.pmdm.frogger.utils.GameAudioManager;

import java.io.InputStream;

public class Juego extends SurfaceView implements SurfaceHolder.Callback {

    private BucleJuego bucleJuego;
    private GameEngine gameEngine;
    private Bitmap background;
    private boolean positionsConfigured = false;
    private GameAudioManager gam = GameAudioManager.getInstance(getContext());

    // Usaremos un Movie para el GIF de "no_time":
    private Movie noTimeGif;
    private boolean noTimeGifStarted = false;
    private long noTimeGifStartTime = 0;

    // Imagen estática de muerte (calavera)
    private Bitmap frogDeathBitmap;

    // Imagen al intentar volver a menu
    private Bitmap sadFrogBitmap;

    // Animación de muerte (frames en un AnimationDrawable)
    private AnimationDrawable froggerDeathAnim;
    private long froggerDeathAnimTotalDuration = 0L;
    private boolean froggerDeathAnimStarted = false;
    private boolean froggerDeathAnimFinished = false;
    private long froggerDeathAnimStartTime = 0L;

    // Rectángulos para los botones de la ventana final
    private RectF retryButtonRect;
    private RectF menuButtonRect;

    // Fuente retro
    private Typeface retroTypeface;

    // Estrellas de victoria
    private Bitmap starBitmap;
    private int victoryStars = 0;

    // Control del nivel actual
    private int currentLevel;

    // --- Scroll manual de agua en nivel 1 ---
    private Bitmap waterBitmap;
    private float waterOffsetX = 0;
    private static final float WATER_SCROLL_SPEED = 2f;

    // --- Scroll manual de arena en nivel 2 ---
    private Bitmap sandBitmap;
    private float sandOffsetX = 0;
    private static final float SAND_SCROLL_SPEED = 1.5f;

    // --- Scroll manual de espacio en nivel 3 ---
    private Bitmap spaceBitmap;
    private float spaceOffsetX = 0;
    private static final float SPACE_SCROLL_SPEED = 2.2f;

    // Ventana de confirmación para salir
    private boolean showExitConfirmWindow = false;
    private RectF exitYesRect; // Botón "SÍ"
    private RectF exitNoRect;  // Botón "NO"

    public Juego(android.content.Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);

        // Fuente retro
        retroTypeface = ResourcesCompat.getFont(context, R.font.press_start_2p);

        // Cargar la estrella
        starBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.star);

        // Cargar el GIF "no_time" usando Movie (para derrota por tiempo)
        try {
            InputStream is = getResources().openRawResource(R.raw.no_time);
            noTimeGif = Movie.decodeStream(is);
        } catch (Exception e) {
            e.printStackTrace();
            noTimeGif = null;
        }

        // Cargar la imagen estática de muerte (calavera)
        frogDeathBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.frogger_death3);

        sadFrogBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sad_frog);

        // Cargar la animación frogger_death.xml (AnimationDrawable)
        froggerDeathAnim = (AnimationDrawable)
                ResourcesCompat.getDrawable(getResources(), R.drawable.frogger_death, null);
        if (froggerDeathAnim != null) {
            froggerDeathAnim.setOneShot(true);
            for (int i = 0; i < froggerDeathAnim.getNumberOfFrames(); i++) {
                froggerDeathAnimTotalDuration += froggerDeathAnim.getDuration(i);
            }
        }
    }

    public void setVictoryStars(int stars) {
        this.victoryStars = stars;
    }

    public void setLevel(int level) {
        currentLevel = level; // Guardamos el nivel actual
        int mapResource;

        switch (level) {
            case 1:
                mapResource = R.drawable.map_level1;
                gam.levelOneTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());

                // Cargamos la textura de agua (nivel 1)
                waterBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.water);
                sandBitmap = null;
                spaceBitmap = null;
                break;

            case 2:
                mapResource = R.drawable.map_level2;
                gam.levelTwoTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());

                // Cargamos la textura de arena (nivel 2)
                sandBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sand);
                waterBitmap = null;
                spaceBitmap = null;
                break;

            case 3:
                mapResource = R.drawable.map_level3;
                gam.levelThreeTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());

                // Cargamos la textura de espacio (nivel 3)
                spaceBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.space);
                waterBitmap = null;
                sandBitmap = null;
                break;

            default:
                // Por defecto, tratamos como si fuera nivel 1
                mapResource = R.drawable.map_level1;
                gam.levelOneTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());

                waterBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.water);
                sandBitmap = null;
                spaceBitmap = null;
        }

        background = BitmapFactory.decodeResource(getResources(), mapResource);
    }

    public void setGameEngine(GameEngine engine) {
        this.gameEngine = engine;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!positionsConfigured && gameEngine != null) {
            int canvasWidth = getWidth();
            int canvasHeight = getHeight();
            configurePositions(canvasWidth, canvasHeight);
            positionsConfigured = true;
        }
        bucleJuego = new BucleJuego(this);
        bucleJuego.setRunning(true);
        bucleJuego.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        bucleJuego.setRunning(false);
        while (retry) {
            try {
                bucleJuego.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void configurePositions(int width, int height) {
        if (gameEngine == null) return;
        int bottomOffset = 300;
        int mapHeight = height - bottomOffset;
        gameEngine.configurePositions(width, mapHeight);
    }

    /**
     * Update del SurfaceView. Aquí:
     * - Llamamos a gameEngine.update() si no está en pausa.
     * - Movemos los offsets de agua/arena/espacio solo si no está en pausa ni acabado el juego.
     */
    public void update() {
        // 1) Actualizar lógica del juego
        if (gameEngine != null) {
            // Si el juego no está en pausa, actualizamos
            if (!gameEngine.isPaused()) {
                gameEngine.update();
            }

            // Si el juego ha terminado (victoria o derrota), no movemos más el scroll
            if (gameEngine.isGameWon() || gameEngine.isGameOver()) {
                return;
            }
        }

        // 2) Si el juego no está en pausa, actualizamos offsets
        if (gameEngine != null && gameEngine.isPaused()) {
            // Si está en pausa, no movemos nada
            return;
        }

        if (currentLevel == 1 && waterBitmap != null) {
            waterOffsetX += WATER_SCROLL_SPEED;
            if (waterOffsetX > waterBitmap.getWidth()) {
                waterOffsetX -= waterBitmap.getWidth();
            }
        }

        if (currentLevel == 2 && sandBitmap != null) {
            sandOffsetX += SAND_SCROLL_SPEED;
            if (sandOffsetX > sandBitmap.getWidth()) {
                sandOffsetX -= sandBitmap.getWidth();
            }
        }

        if (currentLevel == 3 && spaceBitmap != null) {
            spaceOffsetX += SPACE_SCROLL_SPEED;
            if (spaceOffsetX > spaceBitmap.getWidth()) {
                spaceOffsetX -= spaceBitmap.getWidth();
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        int bottomOffset = 300;
        int mapHeight = canvasHeight - bottomOffset;
        Rect dstRect = new Rect(0, 0, canvasWidth, mapHeight);

        // Fondo del juego
        if (background != null) {
            canvas.drawBitmap(background, null, dstRect, null);
        }

        // --- Nivel 1: Agua scrolleando ---
        if (currentLevel == 1 && waterBitmap != null) {
            int waterTop = (int) (0.08f * mapHeight);
            int waterBottom = (int) (0.46f * mapHeight);
            int waterHeight = waterBottom - waterTop;

            for (float x = -waterOffsetX; x < canvasWidth; x += waterBitmap.getWidth()) {
                Rect srcRect = new Rect(0, 0, waterBitmap.getWidth(), waterBitmap.getHeight());
                Rect dstRectWater = new Rect(
                        (int) x,
                        waterTop,
                        (int) (x + waterBitmap.getWidth()),
                        waterTop + waterHeight
                );
                canvas.drawBitmap(waterBitmap, srcRect, dstRectWater, null);
            }
        }

        // --- Nivel 2: Arena scrolleando ---
        if (currentLevel == 2 && sandBitmap != null) {
            int sandTop = (int) (0.08f * mapHeight);
            int sandBottom = (int) (0.46f * mapHeight);
            int sandHeight = sandBottom - sandTop;

            for (float x = -sandOffsetX; x < canvasWidth; x += sandBitmap.getWidth()) {
                Rect srcRect = new Rect(0, 0, sandBitmap.getWidth(), sandBitmap.getHeight());
                Rect dstRectSand = new Rect(
                        (int) x,
                        sandTop,
                        (int) (x + sandBitmap.getWidth()),
                        sandTop + sandHeight
                );
                canvas.drawBitmap(sandBitmap, srcRect, dstRectSand, null);
            }
        }

        // --- Nivel 3: Espacio scrolleando ---
        if (currentLevel == 3 && spaceBitmap != null) {
            int spaceTop = (int) (0.08f * mapHeight);
            int spaceBottom = (int) (0.46f * mapHeight);
            int spaceHeight = spaceBottom - spaceTop;

            for (float x = -spaceOffsetX; x < canvasWidth; x += spaceBitmap.getWidth()) {
                Rect srcRect = new Rect(0, 0, spaceBitmap.getWidth(), spaceBitmap.getHeight());
                Rect dstRectSpace = new Rect(
                        (int) x,
                        spaceTop,
                        (int) (x + spaceBitmap.getWidth()),
                        spaceTop + spaceHeight
                );
                canvas.drawBitmap(spaceBitmap, srcRect, dstRectSpace, null);
            }
        }

        // Lógica del GameEngine (rana, obstáculos, etc.)
        if (gameEngine != null) {
            gameEngine.draw(canvas);

            // Barra de tiempo
            if (!gameEngine.isPaused()) {
                float timeRatio = gameEngine.getTimeRatio();
                int barHeight = 20;
                Paint bgPaint = new Paint();
                bgPaint.setColor(Color.DKGRAY);
                Paint timeBarPaint = new Paint();
                timeBarPaint.setColor(Color.RED);
                canvas.drawRect(0, 0, canvasWidth, barHeight, bgPaint);
                canvas.drawRect(0, 0, (int) (canvasWidth * timeRatio), barHeight, timeBarPaint);
            }
        }

        // --- Ventana final (ganar/perder) ---
        if (gameEngine != null && (gameEngine.isGameWon() || gameEngine.isGameOver())) {
            // Fondo overlay
            Paint overlayPaint = new Paint();
            overlayPaint.setColor(Color.argb(200, 0, 0, 0));
            canvas.drawRect(0, 0, canvasWidth, canvasHeight, overlayPaint);

            // Ventana centrada
            int windowWidth = (int) (canvasWidth * 0.75f);
            int windowHeight = (int) (canvasHeight * 0.35f);
            int left = (canvasWidth - windowWidth) / 2;
            int top = (canvasHeight - windowHeight) / 2;
            RectF windowRect = new RectF(left, top, left + windowWidth, top + windowHeight);

            // Fondo negro
            Paint windowPaint = new Paint();
            windowPaint.setColor(Color.BLACK);
            canvas.drawRect(windowRect, windowPaint);

            // Borde verde
            Paint borderPaint = new Paint();
            borderPaint.setColor(Color.GREEN);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(8);
            canvas.drawRect(windowRect, borderPaint);

            // Texto
            Paint textPaint = new Paint();
            textPaint.setColor(Color.GREEN);
            textPaint.setAntiAlias(true);
            textPaint.setTypeface(retroTypeface);
            textPaint.setTextSize(40);
            textPaint.setTextAlign(Paint.Align.CENTER);

            boolean isVictory = gameEngine.isGameWon();
            boolean isTimeOut = gameEngine.isLostByTime();

            String mainMessage;
            if (isVictory) {
                mainMessage = "¡GANASTE!";
            } else {
                if (isTimeOut) {
                    mainMessage = "¡TIEMPO AGOTADO!";
                } else {
                    mainMessage = "¡SIN VIDAS!";
                }
            }

            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float textX = canvasWidth / 2f;
            float textY = top + (windowHeight * 0.25f) - ((fm.ascent + fm.descent) / 2f);
            canvas.drawText(mainMessage, textX, textY, textPaint);

            // (A) Victoria => estrellas
            if (isVictory && victoryStars > 0 && starBitmap != null) {
                int starSize = (int) (windowWidth * 0.1f);
                int starSpacing = starSize / 2;
                int totalStarsWidth = victoryStars * starSize + (victoryStars - 1) * starSpacing;
                float starsStartX = left + (windowWidth - totalStarsWidth) / 2f;
                float starsY = top + windowHeight * 0.4f;
                for (int i = 0; i < victoryStars; i++) {
                    RectF starRect = new RectF(
                            starsStartX + i * (starSize + starSpacing),
                            starsY,
                            starsStartX + i * (starSize + starSpacing) + starSize,
                            starsY + starSize
                    );
                    canvas.drawBitmap(starBitmap, null, starRect, null);
                }
            }
            // (B) Derrota
            else if (!isVictory) {
                float imageSize = windowWidth * 0.3f;
                float imageX = left + (windowWidth - imageSize) / 2f;
                float imageY = top + (windowHeight * 0.35f);
                RectF imageRect = new RectF(imageX, imageY, imageX + imageSize, imageY + imageSize);

                // (B1) Derrota por tiempo => reproducir no_timeGif
                if (isTimeOut && noTimeGif != null) {
                    if (!noTimeGifStarted) {
                        noTimeGifStartTime = System.currentTimeMillis();
                        noTimeGifStarted = true;
                    }
                    long now = System.currentTimeMillis();
                    int relTime = (int) ((now - noTimeGifStartTime) % noTimeGif.duration());

                    noTimeGif.setTime(relTime);

                    canvas.save();
                    canvas.translate(imageX, imageY);

                    float scaleX = imageSize / noTimeGif.width();
                    float scaleY = imageSize / noTimeGif.height();
                    canvas.scale(scaleX, scaleY);

                    noTimeGif.draw(canvas, 0, 0);
                    canvas.restore();
                }
                // (B2) Derrota por vidas => animación de froggerDeathAnim + calavera
                else if (!isTimeOut) {
                    if (!froggerDeathAnimFinished && froggerDeathAnim != null) {
                        if (!froggerDeathAnimStarted) {
                            froggerDeathAnimStarted = true;
                            froggerDeathAnimStartTime = System.currentTimeMillis();
                            froggerDeathAnim.start();
                        }
                        froggerDeathAnim.setBounds(
                                (int) imageRect.left,
                                (int) imageRect.top,
                                (int) imageRect.right,
                                (int) imageRect.bottom
                        );
                        froggerDeathAnim.draw(canvas);
                        froggerDeathAnim.run();

                        long elapsed = System.currentTimeMillis() - froggerDeathAnimStartTime;
                        if (elapsed >= froggerDeathAnimTotalDuration) {
                            froggerDeathAnimFinished = true;
                        }
                    } else {
                        canvas.drawBitmap(frogDeathBitmap, null, imageRect, null);
                    }
                }
            }

            // Botones finales (REINTENTAR / MENÚ)
            float buttonWidth = windowWidth * 0.4f;
            float buttonHeight = 80;
            float spaceBetween = windowWidth * 0.05f;

            float marginBottom = 50;
            float retryTop = top + windowHeight - buttonHeight - marginBottom;

            float retryLeft = (canvasWidth / 2f) - buttonWidth - (spaceBetween / 2f);
            retryButtonRect = new RectF(retryLeft, retryTop,
                    retryLeft + buttonWidth, retryTop + buttonHeight);

            float menuLeft = (canvasWidth / 2f) + (spaceBetween / 2f);
            float menuTop = retryTop;
            menuButtonRect = new RectF(menuLeft, menuTop,
                    menuLeft + buttonWidth, menuTop + buttonHeight);

            drawRetroButton(canvas, retryButtonRect, "REINTENTAR");
            drawRetroButton(canvas, menuButtonRect, "MENÚ");
        }

        // --- Ventana de confirmación de salida (si se ha pedido) ---
// --- Ventana de confirmación de salida (si se ha pedido) ---
        if (showExitConfirmWindow) {
            // Fondo overlay
            Paint overlayPaint = new Paint();
            overlayPaint.setColor(Color.argb(200, 0, 0, 0));
            canvas.drawRect(0, 0, canvasWidth, canvasHeight, overlayPaint);

            // Ventana centrada
            int windowWidth = (int) (canvasWidth * 0.75f);
            int windowHeight = (int) (canvasHeight * 0.35f);
            int left = (canvasWidth - windowWidth) / 2;
            int top = (canvasHeight - windowHeight) / 2;
            RectF windowRect = new RectF(left, top, left + windowWidth, top + windowHeight);

            // Fondo negro
            Paint windowPaint = new Paint();
            windowPaint.setColor(Color.BLACK);
            canvas.drawRect(windowRect, windowPaint);

            // Borde verde
            Paint borderPaint = new Paint();
            borderPaint.setColor(Color.GREEN);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(8);
            canvas.drawRect(windowRect, borderPaint);

            // Texto
            Paint textPaint = new Paint();
            textPaint.setColor(Color.GREEN);
            textPaint.setAntiAlias(true);
            textPaint.setTypeface(retroTypeface);
            textPaint.setTextSize(32);
            textPaint.setTextAlign(Paint.Align.CENTER);

            String confirmMessage = "¿Salir al Menú?\nPerderás el \nprogreso actual.\n";
            float textX = canvasWidth / 2f;    // Centramos en X
            float lineSpacing = 40f;          // Espaciado entre líneas
            float currentY = top + (windowHeight * 0.15f); // Texto un poco más arriba

            // Dividimos en líneas y las dibujamos
            String[] lines = confirmMessage.split("\n");
            for (String line : lines) {
                canvas.drawText(line, textX, currentY, textPaint);
                currentY += lineSpacing;
            }

            // DIBUJAR sad_frog centrada debajo del texto
            if (sadFrogBitmap != null) {
                float frogSize = windowWidth * 0.25f; // Ajusta el tamaño a tu gusto
                float frogX = (canvasWidth / 2f) - (frogSize / 2f); // Centrado horizontal
                float frogY = currentY + 10; // Un poco debajo del texto
                RectF frogRect = new RectF(frogX, frogY, frogX + frogSize, frogY + frogSize);
                canvas.drawBitmap(sadFrogBitmap, null, frogRect, null);

                // Actualizamos currentY para que los botones queden más abajo si hace falta
                currentY = frogY + frogSize + 20;
            }

            // Botones (Sí/No)
            float btnW = windowWidth * 0.3f;
            float btnH = 80;
            float spaceBetween = windowWidth * 0.1f;
            float marginBottom = 30;
            float buttonTop = top + windowHeight - btnH - marginBottom; // Ajuste para situarlos al final

            float yesLeft = (canvasWidth / 2f) - btnW - (spaceBetween / 2f);
            exitYesRect = new RectF(yesLeft, buttonTop, yesLeft + btnW, buttonTop + btnH);

            float noLeft = (canvasWidth / 2f) + (spaceBetween / 2f);
            exitNoRect = new RectF(noLeft, buttonTop, noLeft + btnW, buttonTop + btnH);

            drawRetroButton(canvas, exitYesRect, "SÍ");
            drawRetroButton(canvas, exitNoRect, "NO");
        }
    }

    private void drawRetroButton(Canvas canvas, RectF rect, String text) {
        Paint buttonPaint = new Paint();
        buttonPaint.setColor(Color.BLACK);
        canvas.drawRect(rect, buttonPaint);

        Paint buttonBorderPaint = new Paint();
        buttonBorderPaint.setColor(Color.GREEN);
        buttonBorderPaint.setStyle(Paint.Style.STROKE);
        buttonBorderPaint.setStrokeWidth(4);
        canvas.drawRect(rect, buttonBorderPaint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.GREEN);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(24);
        textPaint.setTypeface(retroTypeface);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float centerX = rect.centerX();
        float centerY = rect.centerY() - ((fm.ascent + fm.descent) / 2f);
        canvas.drawText(text, centerX, centerY, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();

            // 1) Si estamos mostrando la ventana de confirmación de salida
            if (showExitConfirmWindow) {
                if (exitYesRect != null && exitYesRect.contains(touchX, touchY)) {
                    // "SÍ": Volvemos a MainActivity
                    Activity activity = (Activity) getContext();
                    Intent intent = new Intent(activity, MainActivity.class);
                    activity.startActivity(intent);
                    activity.finish();
                    return true;
                }
                if (exitNoRect != null && exitNoRect.contains(touchX, touchY)) {
                    // "NO": Ocultamos la ventana y reanudamos
                    showExitConfirmWindow = false;
                    // Quitar la pausa en GameEngine
                    if (gameEngine != null) {
                        gameEngine.setPaused(false);
                    }
                    return true;
                }
                // Si clic fuera de los botones, no hacemos nada
                return true;
            }

            // 2) Si el juego está en ventana final (ganar/perder)
            if (gameEngine != null && (gameEngine.isGameWon() || gameEngine.isGameOver())) {
                if (retryButtonRect != null && retryButtonRect.contains(touchX, touchY)) {
                    Activity activity = (Activity) getContext();
                    // Reinicia la Activity para volver a jugar
                    activity.recreate();
                    return true;
                }

                if (menuButtonRect != null && menuButtonRect.contains(touchX, touchY)) {
                    Activity activity = (Activity) getContext();
                    Intent intent = new Intent(activity, MainActivity.class);
                    activity.startActivity(intent);
                    activity.finish();
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * Método público para activar la ventana de confirmación.
     * Se pausa el juego en el GameEngine.
     */
    public void requestExitConfirmation() {
        showExitConfirmWindow = true;
        if (gameEngine != null) {
            gameEngine.setPaused(true);
        }
    }

    public void movePlayerLeft() {
        if (gameEngine != null) {
            gameEngine.movePlayerLeft();
            gam.playerMovement(getContext());
        }
    }

    public void movePlayerUp() {
        if (gameEngine != null) {
            gameEngine.movePlayerUp();
            gam.playerMovement(getContext());
        }
    }

    public void movePlayerRight() {
        if (gameEngine != null) {
            gameEngine.movePlayerRight();
            gam.playerMovement(getContext());
        }
    }

    public void movePlayerDown() {
        if (gameEngine != null) {
            gameEngine.movePlayerDown();
            gam.playerMovement(getContext());
        }
    }
}