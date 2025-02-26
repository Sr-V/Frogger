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
    private GameAudioManager gam = GameAudioManager.getInstance();

    // Usaremos un Movie para el GIF:
    private Movie noTimeGif;
    private boolean noTimeGifStarted = false;
    private long noTimeGifStartTime = 0;

    // Imagen estática de muerte (calavera)
    private Bitmap frogDeathBitmap;

    // Animación de muerte
    private AnimationDrawable froggerDeathAnim;
    private long froggerDeathAnimTotalDuration = 0L;
    private boolean froggerDeathAnimStarted = false;
    private boolean froggerDeathAnimFinished = false;
    private long froggerDeathAnimStartTime = 0L;

    // Rectángulos para los botones
    private RectF retryButtonRect;
    private RectF menuButtonRect;

    // Fuente retro
    private Typeface retroTypeface;

    // Estrellas de victoria
    private Bitmap starBitmap;
    private int victoryStars = 0;

    public Juego(android.content.Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);

        // Fuente retro
        retroTypeface = ResourcesCompat.getFont(context, R.font.press_start_2p);

        // Cargar la estrella
        starBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.star);

        // Cargar el GIF "no_time" usando Movie
        try {
            // Si lo tienes en res/raw/no_time.gif:
            InputStream is = getResources().openRawResource(R.raw.no_time);
            noTimeGif = Movie.decodeStream(is);
        } catch (Exception e) {
            e.printStackTrace();
            noTimeGif = null;
        }

        // Cargar la imagen estática de muerte (calavera)
        frogDeathBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.frogger_death3);

        // Cargar la animación frogger_death.xml (AnimationDrawable)
        froggerDeathAnim = (AnimationDrawable)
                ResourcesCompat.getDrawable(getResources(), R.drawable.frogger_death, null);
        if (froggerDeathAnim != null) {
            froggerDeathAnim.setOneShot(true); // Se reproduce una sola vez
            for (int i = 0; i < froggerDeathAnim.getNumberOfFrames(); i++) {
                froggerDeathAnimTotalDuration += froggerDeathAnim.getDuration(i);
            }
        }
    }

    public void setVictoryStars(int stars) {
        this.victoryStars = stars;
    }

    public void setLevel(int level) {
        int mapResource;
        switch (level) {
            case 1:
                mapResource = R.drawable.map_level1;
                gam.levelOneTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());
                break;
            case 2:
                mapResource = R.drawable.map_level2;
                gam.levelTwoTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());
                break;
            case 3:
                mapResource = R.drawable.map_level3;
                gam.levelThreeTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());
                break;
            default:
                mapResource = R.drawable.map_level1;
                gam.levelOneTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());
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

    public void update() {
        if (gameEngine != null) {
            gameEngine.update();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        int bottomOffset = 300;
        Rect dstRect = new Rect(0, 0, canvasWidth, canvasHeight - bottomOffset);

        // Fondo del juego
        if (background != null) {
            canvas.drawBitmap(background, null, dstRect, null);
        }

        // Lógica del GameEngine
        if (gameEngine != null) {
            gameEngine.draw(canvas);

            // Barra de tiempo
            float timeRatio = gameEngine.getTimeRatio();
            int barHeight = 20;
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.DKGRAY);
            Paint timeBarPaint = new Paint();
            timeBarPaint.setColor(Color.RED);
            canvas.drawRect(0, 0, canvasWidth, barHeight, bgPaint);
            canvas.drawRect(0, 0, (int) (canvasWidth * timeRatio), barHeight, timeBarPaint);
        }

        // Ventana final
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
                    // Iniciamos el GIF si no lo hemos hecho
                    if (!noTimeGifStarted) {
                        noTimeGifStartTime = System.currentTimeMillis();
                        noTimeGifStarted = true;
                    }
                    // Calcular el tiempo relativo
                    long now = System.currentTimeMillis();
                    int relTime = (int) ((now - noTimeGifStartTime) % noTimeGif.duration());

                    noTimeGif.setTime(relTime);

                    // Para escalar el GIF al tamaño de imageRect
                    canvas.save();
                    // Trasladar el canvas a la esquina superior izquierda de imageRect
                    canvas.translate(imageX, imageY);

                    // Calcular escalado
                    float scaleX = imageSize / noTimeGif.width();
                    float scaleY = imageSize / noTimeGif.height();
                    canvas.scale(scaleX, scaleY);

                    // Dibujar el GIF en (0,0) con esa escala
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

            // Botones
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
            if (gameEngine != null && (gameEngine.isGameWon() || gameEngine.isGameOver())) {
                float touchX = event.getX();
                float touchY = event.getY();

                if (retryButtonRect != null && retryButtonRect.contains(touchX, touchY)) {
                    Activity activity = (Activity) getContext();
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