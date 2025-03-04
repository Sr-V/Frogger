package edu.pmdm.frogger.game;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.Movie;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.res.ResourcesCompat;

import java.io.InputStream;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.activities.MainActivity;
import edu.pmdm.frogger.utils.GameAudioManager;

/**
 * {@code Juego} es el SurfaceView principal del juego Frogger.
 * Se encarga de gestionar la actualización y el renderizado de todos los elementos del juego,
 * como el fondo, la rana, los obstáculos, las animaciones, botones y ventanas de confirmación.
 */
public class Juego extends SurfaceView implements SurfaceHolder.Callback {

    private BucleJuego bucleJuego;          // Hilo principal del juego
    private GameEngine gameEngine;           // Motor del juego, que actualiza la lógica y renderizado
    private Bitmap background;               // Fondo del nivel
    private boolean positionsConfigured = false; // Indica si las posiciones iniciales han sido configuradas
    private GameAudioManager gam = GameAudioManager.getInstance(getContext()); // Gestor de audio

    // Variables para la animación del GIF "no_time" (derrota por tiempo)
    private Movie noTimeGif;
    private boolean noTimeGifStarted = false;
    private long noTimeGifStartTime = 0;

    // Imagen estática que representa la muerte (calavera)
    private Bitmap frogDeathBitmap;

    // Imagen que se muestra al intentar volver al menú (sad frog)
    private Bitmap sadFrogBitmap;

    // Animación de muerte de la rana, definida en un AnimationDrawable
    private AnimationDrawable froggerDeathAnim;
    private long froggerDeathAnimTotalDuration = 0L;
    private boolean froggerDeathAnimStarted = false;
    private boolean froggerDeathAnimFinished = false;
    private long froggerDeathAnimStartTime = 0L;

    // Rectángulos que definen las zonas de los botones en la ventana final (REINTENTAR / MENÚ)
    private RectF retryButtonRect;
    private RectF menuButtonRect;

    // Fuente retro para textos
    private Typeface retroTypeface;

    // Estrellas de victoria obtenidas al ganar el nivel
    private Bitmap starBitmap;
    private int victoryStars = 0;

    // Control del nivel actual
    private int currentLevel;

    // --- Scroll manual de texturas en niveles ---
    // Nivel 1: Agua
    private Bitmap waterBitmap;
    private float waterOffsetX = 0;
    private static final float WATER_SCROLL_SPEED = 2f;
    // Nivel 2: Arena
    private Bitmap sandBitmap;
    private float sandOffsetX = 0;
    private static final float SAND_SCROLL_SPEED = 1.5f;
    // Nivel 3: Espacio
    private Bitmap spaceBitmap;
    private float spaceOffsetX = 0;
    private static final float SPACE_SCROLL_SPEED = 2.2f;

    // Variables para la ventana de confirmación al intentar salir
    private boolean showExitConfirmWindow = false;
    private RectF exitYesRect; // Botón "SÍ"
    private RectF exitNoRect;  // Botón "NO"

    /**
     * Constructor de {@code Juego}. Se inicializan los callbacks del SurfaceHolder,
     * se cargan recursos y se configura la fuente retro.
     *
     * @param context Contexto de la aplicación.
     * @param attrs   Atributos XML.
     */
    public Juego(android.content.Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);

        // Cargar la fuente retro
        retroTypeface = ResourcesCompat.getFont(context, R.font.press_start_2p);

        // Cargar la imagen de la estrella para la victoria
        starBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.star);

        // Cargar el GIF "no_time" usando la clase Movie para la derrota por tiempo
        try {
            InputStream is = getResources().openRawResource(R.raw.no_time);
            noTimeGif = Movie.decodeStream(is);
        } catch (Exception e) {
            e.printStackTrace();
            noTimeGif = null;
        }

        // Cargar la imagen estática de muerte (calavera)
        frogDeathBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.frogger_death3);

        // Cargar la imagen sad_frog para la confirmación de salida
        sadFrogBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sad_frog);

        // Cargar la animación de muerte desde frogger_death.xml
        froggerDeathAnim = (AnimationDrawable)
                ResourcesCompat.getDrawable(getResources(), R.drawable.frogger_death, null);
        if (froggerDeathAnim != null) {
            froggerDeathAnim.setOneShot(true);
            // Calcular la duración total de la animación sumando la duración de cada frame
            for (int i = 0; i < froggerDeathAnim.getNumberOfFrames(); i++) {
                froggerDeathAnimTotalDuration += froggerDeathAnim.getDuration(i);
            }
        }
    }

    /**
     * Establece el número de estrellas obtenidas al ganar el nivel.
     *
     * @param stars Número de estrellas.
     */
    public void setVictoryStars(int stars) {
        this.victoryStars = stars;
    }

    /**
     * Configura el nivel actual del juego, carga el fondo y texturas específicas,
     * y reproduce la música y efectos correspondientes.
     *
     * @param level Nivel actual.
     */
    public void setLevel(int level) {
        currentLevel = level; // Guardamos el nivel actual
        int mapResource;

        switch (level) {
            case 1:
                mapResource = R.drawable.map_level1;
                gam.levelOneTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());
                // Cargar textura de agua para nivel 1
                waterBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.water);
                sandBitmap = null;
                spaceBitmap = null;
                break;

            case 2:
                mapResource = R.drawable.map_level2;
                gam.levelTwoTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());
                // Cargar textura de arena para nivel 2
                sandBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sand);
                waterBitmap = null;
                spaceBitmap = null;
                break;

            case 3:
                mapResource = R.drawable.map_level3;
                gam.levelThreeTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());
                // Cargar textura de espacio para nivel 3
                spaceBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.space);
                waterBitmap = null;
                sandBitmap = null;
                break;

            default:
                // Por defecto, tratar como nivel 1
                mapResource = R.drawable.map_level1;
                gam.levelOneTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());
                waterBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.water);
                sandBitmap = null;
                spaceBitmap = null;
        }

        // Cargar el fondo del mapa según el nivel
        background = BitmapFactory.decodeResource(getResources(), mapResource);
    }

    /**
     * Asocia el {@code GameEngine} con esta vista para delegar la actualización y el renderizado.
     *
     * @param engine Instancia de {@link GameEngine}.
     */
    public void setGameEngine(GameEngine engine) {
        this.gameEngine = engine;
    }

    /**
     * Callback invocado cuando la superficie de dibujo se crea.
     * Configura las posiciones iniciales y arranca el hilo del juego.
     *
     * @param holder SurfaceHolder asociado a esta vista.
     */
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

    /**
     * Callback invocado cuando la superficie de dibujo se destruye.
     * Detiene el hilo del juego de forma segura.
     *
     * @param holder SurfaceHolder asociado a esta vista.
     */
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

    /**
     * Configura las posiciones iniciales del {@code GameEngine} basándose en el tamaño del canvas.
     *
     * @param width  Ancho del canvas.
     * @param height Alto del canvas.
     */
    private void configurePositions(int width, int height) {
        if (gameEngine == null) return;
        int bottomOffset = 300;
        int mapHeight = height - bottomOffset;
        gameEngine.configurePositions(width, mapHeight);
    }

    /**
     * Actualiza la lógica del juego y los offsets de scroll de las texturas (agua, arena, espacio),
     * siempre que el juego no esté en pausa ni finalizado.
     */
    public void update() {
        // 1) Actualizar la lógica del GameEngine
        if (gameEngine != null) {
            if (!gameEngine.isPaused()) {
                gameEngine.update();
            }
            // Si el juego ha finalizado, no se actualiza el scroll
            if (gameEngine.isGameWon() || gameEngine.isGameOver()) {
                return;
            }
        }

        // 2) Actualizar los offsets para el scroll manual de texturas, solo si no está en pausa
        if (gameEngine != null && gameEngine.isPaused()) {
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

    /**
     * Dibuja todos los elementos del juego en el canvas, incluyendo fondo, scroll de texturas,
     * elementos del GameEngine, barra de tiempo y ventanas finales (victoria/derrota y confirmación de salida).
     *
     * @param canvas Canvas sobre el que se dibuja el juego.
     */
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        int bottomOffset = 300;
        int mapHeight = canvasHeight - bottomOffset;
        Rect dstRect = new Rect(0, 0, canvasWidth, mapHeight);

        // Dibujar el fondo del juego
        if (background != null) {
            canvas.drawBitmap(background, null, dstRect, null);
        }

        // --- Scroll manual para nivel 1: Agua ---
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

        // --- Scroll manual para nivel 2: Arena ---
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

        // --- Scroll manual para nivel 3: Espacio ---
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

        // Dibujar elementos del GameEngine (rana, obstáculos, vidas, etc.)
        if (gameEngine != null) {
            gameEngine.draw(canvas);

            // Dibujar la barra de tiempo si el juego no está en pausa
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

        // --- Ventana final (victoria o derrota) ---
        if (gameEngine != null && (gameEngine.isGameWon() || gameEngine.isGameOver())) {
            // Dibujar overlay semitransparente
            Paint overlayPaint = new Paint();
            overlayPaint.setColor(Color.argb(200, 0, 0, 0));
            canvas.drawRect(0, 0, canvasWidth, canvasHeight, overlayPaint);

            // Ventana centrada
            int windowWidth = (int) (canvasWidth * 0.75f);
            int windowHeight = (int) (canvasHeight * 0.35f);
            int left = (canvasWidth - windowWidth) / 2;
            int top = (canvasHeight - windowHeight) / 2;
            RectF windowRect = new RectF(left, top, left + windowWidth, top + windowHeight);

            // Fondo negro para la ventana
            Paint windowPaint = new Paint();
            windowPaint.setColor(Color.BLACK);
            canvas.drawRect(windowRect, windowPaint);

            // Borde verde para la ventana
            Paint borderPaint = new Paint();
            borderPaint.setColor(Color.GREEN);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(8);
            canvas.drawRect(windowRect, borderPaint);

            // Configurar el texto del mensaje final
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

            // Mostrar estrellas de victoria si se ganó
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
            // Mostrar animaciones para derrota
            else if (!isVictory) {
                float imageSize = windowWidth * 0.3f;
                float imageX = left + (windowWidth - imageSize) / 2f;
                float imageY = top + (windowHeight * 0.35f);
                RectF imageRect = new RectF(imageX, imageY, imageX + imageSize, imageY + imageSize);

                // Derrota por tiempo: reproducir el GIF "no_time"
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

                    float scaleX = imageSize / (float) noTimeGif.width();
                    float scaleY = imageSize / (float) noTimeGif.height();
                    canvas.scale(scaleX, scaleY);

                    noTimeGif.draw(canvas, 0, 0);
                    canvas.restore();
                }
                // Derrota por vidas: reproducir animación de muerte o mostrar imagen estática
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

            // Dibujar botones de la ventana final: REINTENTAR y MENÚ
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

        // --- Ventana de confirmación de salida ---
        if (showExitConfirmWindow) {
            // Dibujar overlay semitransparente
            Paint overlayPaint = new Paint();
            overlayPaint.setColor(Color.argb(200, 0, 0, 0));
            canvas.drawRect(0, 0, canvasWidth, canvasHeight, overlayPaint);

            // Definir ventana centrada
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

            // Texto de confirmación
            Paint textPaint = new Paint();
            textPaint.setColor(Color.GREEN);
            textPaint.setAntiAlias(true);
            textPaint.setTypeface(retroTypeface);
            textPaint.setTextSize(32);
            textPaint.setTextAlign(Paint.Align.CENTER);

            String confirmMessage = "¿Salir al Menú?\nPerderás el \nprogreso actual.\n";
            float textX = canvasWidth / 2f;
            float lineSpacing = 40f;
            float currentY = top + (windowHeight * 0.15f);

            // Dibujar cada línea del mensaje
            String[] lines = confirmMessage.split("\n");
            for (String line : lines) {
                canvas.drawText(line, textX, currentY, textPaint);
                currentY += lineSpacing;
            }

            // Dibujar imagen sad_frog centrada debajo del texto
            if (sadFrogBitmap != null) {
                float frogSize = windowWidth * 0.25f;
                float frogX = (canvasWidth / 2f) - (frogSize / 2f);
                float frogY = currentY + 10;
                RectF frogRect = new RectF(frogX, frogY, frogX + frogSize, frogY + frogSize);
                canvas.drawBitmap(sadFrogBitmap, null, frogRect, null);
                currentY = frogY + frogSize + 20;
            }

            // Dibujar botones "SÍ" y "NO"
            float btnW = windowWidth * 0.3f;
            float btnH = 80;
            float btnSpace = windowWidth * 0.1f;
            float marginBottom = 30;
            float buttonTop = top + windowHeight - btnH - marginBottom;

            float yesLeft = (canvasWidth / 2f) - btnW - (btnSpace / 2f);
            exitYesRect = new RectF(yesLeft, buttonTop, yesLeft + btnW, buttonTop + btnH);

            float noLeft = (canvasWidth / 2f) + (btnSpace / 2f);
            exitNoRect = new RectF(noLeft, buttonTop, noLeft + btnW, buttonTop + btnH);

            drawRetroButton(canvas, exitYesRect, "SÍ");
            drawRetroButton(canvas, exitNoRect, "NO");
        }
    }

    /**
     * Dibuja un botón con estilo retro dentro del rectángulo especificado y con el texto dado.
     *
     * @param canvas Canvas en el que se dibuja.
     * @param rect   Rectángulo que define la posición y tamaño del botón.
     * @param text   Texto a mostrar en el botón.
     */
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

    /**
     * Maneja los eventos táctiles para la interacción del usuario.
     * Detecta toques en los botones de la ventana final o en la ventana de confirmación de salida.
     *
     * @param event Evento de toque.
     * @return {@code true} si el evento fue consumido, {@code false} en caso contrario.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();

            // 1) Si se muestra la ventana de confirmación de salida
            if (showExitConfirmWindow) {
                if (exitYesRect != null && exitYesRect.contains(touchX, touchY)) {
                    // Botón "SÍ": Regresar a MainActivity
                    Activity activity = (Activity) getContext();
                    Intent intent = new Intent(activity, MainActivity.class);
                    activity.startActivity(intent);
                    activity.finish();
                    return true;
                }
                if (exitNoRect != null && exitNoRect.contains(touchX, touchY)) {
                    // Botón "NO": Ocultar la ventana de confirmación y reanudar el juego
                    showExitConfirmWindow = false;
                    if (gameEngine != null) {
                        gameEngine.setPaused(false);
                    }
                    return true;
                }
                return true;
            }

            // 2) Si el juego está en ventana final (victoria o derrota)
            if (gameEngine != null && (gameEngine.isGameWon() || gameEngine.isGameOver())) {
                if (retryButtonRect != null && retryButtonRect.contains(touchX, touchY)) {
                    Activity activity = (Activity) getContext();
                    // Reiniciar la Activity para volver a jugar
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
     * Activa la ventana de confirmación para salir al menú.
     * Además, pausa el juego en el GameEngine.
     */
    public void requestExitConfirmation() {
        showExitConfirmWindow = true;
        if (gameEngine != null) {
            gameEngine.setPaused(true);
        }
    }

    // --- Métodos de control de movimiento de la rana ---

    /**
     * Mueve la rana hacia la izquierda y reproduce un efecto de movimiento.
     */
    public void movePlayerLeft() {
        if (gameEngine != null) {
            gameEngine.movePlayerLeft();
            gam.playerMovement(getContext());
        }
    }

    /**
     * Mueve la rana hacia arriba y reproduce un efecto de movimiento.
     */
    public void movePlayerUp() {
        if (gameEngine != null) {
            gameEngine.movePlayerUp();
            gam.playerMovement(getContext());
        }
    }

    /**
     * Mueve la rana hacia la derecha y reproduce un efecto de movimiento.
     */
    public void movePlayerRight() {
        if (gameEngine != null) {
            gameEngine.movePlayerRight();
            gam.playerMovement(getContext());
        }
    }

    /**
     * Mueve la rana hacia abajo y reproduce un efecto de movimiento.
     */
    public void movePlayerDown() {
        if (gameEngine != null) {
            gameEngine.movePlayerDown();
            gam.playerMovement(getContext());
        }
    }
}