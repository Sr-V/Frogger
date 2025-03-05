package edu.pmdm.frogger.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.activities.LevelSelectionActivity;
import edu.pmdm.frogger.activities.GameActivity;
import edu.pmdm.frogger.utils.GameAudioManager;

/**
 * {@code AlertsOverlayView} es un overlay personalizado que dibuja ventanas de alerta
 * con un estilo retro (fondo negro, bordes verdes y tipografía pixelada) en lugar de utilizar
 * un AlertDialog tradicional. Se utiliza para mostrar mensajes como "Nivel no disponible" o
 * "Proximamente" en la aplicación.
 */
public class AlertsOverlayView extends View {

    // Flags para controlar qué ventana se muestra
    private boolean showNoNewLevelsWindow = false;   // Ventana para "Niveles no disponibles" (desde MainActivity)
    private boolean showProximamenteWindow = false;    // Ventana para "Nivel no disponible" (desde LevelSelectionActivity)

    // Bitmaps para ilustrar las ventanas con imágenes de ranas
    private Bitmap thinkingFrogBitmap;
    private Bitmap doubtFrogBitmap;

    // Pinturas para dibujar bordes, fondo y textos
    private Paint borderPaint, bgPaint, textPaint;
    // Fuente retro para los textos
    private Typeface retroTypeface;

    // Rectángulos que definen las áreas táctiles para los botones en la ventana "Niveles no disponibles"
    private RectF btnJugarNivel3Rect;
    private RectF btnSeleccionarNivelRect;

    // Rectángulo que define el área táctil para el botón "Aceptar" en la ventana "Proximamente"
    private RectF btnOkRect;

    // Referencia al contexto, utilizado para lanzar Intents
    private final Context mContext;

    /**
     * Constructor que recibe solo el contexto.
     *
     * @param context Contexto de la aplicación.
     */
    public AlertsOverlayView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    /**
     * Constructor que recibe contexto y atributos XML.
     *
     * @param context Contexto de la aplicación.
     * @param attrs   Atributos XML.
     */
    public AlertsOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    /**
     * Inicializa los recursos, bitmaps y configuraciones de las pinturas.
     */
    private void init() {
        // Cargar los bitmaps de las ranas utilizadas en las ventanas
        thinkingFrogBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.thinking_frog);
        doubtFrogBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.doubt_frog);

        // Configurar la pintura para el borde (verde)
        borderPaint = new Paint();
        borderPaint.setColor(Color.GREEN);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(8);

        // Configurar la pintura para el fondo (negro)
        bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);

        // Configurar la pintura para el texto (verde, anti-alias, tamaño 28)
        textPaint = new Paint();
        textPaint.setColor(Color.GREEN);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(28);
        retroTypeface = ResourcesCompat.getFont(getContext(), R.font.press_start_2p);
        textPaint.setTypeface(retroTypeface);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Muestra la ventana "Niveles no disponibles" y oculta cualquier otra.
     */
    public void showNoNewLevelsWindow() {
        showNoNewLevelsWindow = true;
        showProximamenteWindow = false;
        invalidate();
        setVisibility(VISIBLE);
    }

    /**
     * Muestra la ventana "Proximamente" y oculta cualquier otra.
     */
    public void showProximamenteWindow() {
        showProximamenteWindow = true;
        showNoNewLevelsWindow = false;
        invalidate();
        setVisibility(VISIBLE);
    }

    /**
     * Oculta todas las ventanas de alerta.
     */
    public void hideAll() {
        showNoNewLevelsWindow = false;
        showProximamenteWindow = false;
        setVisibility(GONE);
        invalidate();
    }

    /**
     * Dibuja el overlay y la ventana de alerta según el estado actual.
     *
     * @param canvas Canvas sobre el que se dibuja el overlay.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Si ninguna ventana debe mostrarse, no se dibuja nada
        if (!showNoNewLevelsWindow && !showProximamenteWindow) {
            return;
        }

        int w = getWidth();
        int h = getHeight();

        // Dibujar un fondo overlay semi-transparente en toda la pantalla
        Paint overlayPaint = new Paint();
        overlayPaint.setColor(Color.argb(200, 0, 0, 0));
        canvas.drawRect(0, 0, w, h, overlayPaint);

        // Definir una ventana centrada que ocupa el 75% del ancho y el 35% del alto de la pantalla
        int windowWidth = (int) (w * 0.75f);
        int windowHeight = (int) (h * 0.35f);
        int left = (w - windowWidth) / 2;
        int top = (h - windowHeight) / 2;
        RectF windowRect = new RectF(left, top, left + windowWidth, top + windowHeight);

        // Dibujar el fondo negro y el borde verde de la ventana
        canvas.drawRect(windowRect, bgPaint);
        canvas.drawRect(windowRect, borderPaint);

        // Dibujar la ventana adecuada según el estado
        if (showNoNewLevelsWindow) {
            drawNoNewLevelsWindow(canvas, windowRect);
        } else if (showProximamenteWindow) {
            drawProximamenteWindow(canvas, windowRect);
        }
    }

    /**
     * Dibuja la ventana "Niveles no disponibles" con su mensaje y botones.
     *
     * @param canvas     Canvas sobre el que se dibuja.
     * @param windowRect Rectángulo que define la ventana.
     */
    private void drawNoNewLevelsWindow(Canvas canvas, RectF windowRect) {
        // Título y mensaje de la ventana
        String title = "Nivel no disponible";
        String confirmMessage =
                "\n\n" +
                        "No existen niveles nuevos.\nEl último nivel es el 3.\n\n" +
                        "¿Qué quieres hacer?";

        float centerX = windowRect.centerX();
        float currentY = windowRect.top + (windowRect.height() * 0.1f);

        // Dibujar el título
        canvas.drawText(title, centerX, currentY, textPaint);
        currentY += 50;

        // Dibujar cada línea del mensaje
        for (String line : confirmMessage.split("\n")) {
            canvas.drawText(line, centerX, currentY, textPaint);
            currentY += 40;
        }

        // Dibujar la imagen de la rana pensando (thinking_frog) debajo del texto
        if (thinkingFrogBitmap != null) {
            float frogSize = windowRect.width() * 0.25f;
            float frogX = (canvas.getWidth() / 2f) - (frogSize / 2f);
            float frogY = currentY + 20;
            RectF frogRect = new RectF(frogX, frogY, frogX + frogSize, frogY + frogSize);
            canvas.drawBitmap(thinkingFrogBitmap, null, frogRect, null);
            currentY = frogY + frogSize + 20;
        }

        // Definir y dibujar los botones "Jugar nivel 3" y "Escoger nivel"
        float btnW = windowRect.width() * 0.45f;
        float btnH = 80;
        float spaceBetween = windowRect.width() * 0.05f;
        float marginBottom = 30;
        float btnTop = windowRect.bottom - btnH - marginBottom;

        float leftBtnX = (canvas.getWidth() / 2f) - btnW - (spaceBetween / 2f);
        float rightBtnX = (canvas.getWidth() / 2f) + (spaceBetween / 2f);

        btnJugarNivel3Rect = new RectF(leftBtnX, btnTop, leftBtnX + btnW, btnTop + btnH);
        btnSeleccionarNivelRect = new RectF(rightBtnX, btnTop, rightBtnX + btnW, btnTop + btnH);

        drawRetroButton(canvas, btnJugarNivel3Rect, "Jugar nivel 3");
        drawRetroButton(canvas, btnSeleccionarNivelRect, "Escoger nivel");
    }

    /**
     * Dibuja la ventana "Proximamente" con su mensaje y botón "Aceptar".
     *
     * @param canvas     Canvas sobre el que se dibuja.
     * @param windowRect Rectángulo que define la ventana.
     */
    private void drawProximamenteWindow(Canvas canvas, RectF windowRect) {
        // Ajustar el tamaño del texto para esta ventana
        textPaint.setTextSize(30);
        String title = "Nivel no disponible";
        String msg = "¡Lo sentimos!\nEste nivel aún\nno se ha desarrollado.";

        float centerX = windowRect.centerX();
        float currentY = windowRect.top + (windowRect.height() * 0.25f);

        // Dibujar el título
        canvas.drawText(title, centerX, currentY, textPaint);
        currentY += 60;

        // Dibujar el mensaje de varias líneas
        for (String line : msg.split("\n")) {
            canvas.drawText(line, centerX, currentY, textPaint);
            currentY += 40;
        }

        // Dibujar la imagen doubt_frog centrada debajo del texto
        if (doubtFrogBitmap != null) {
            float frogSize = windowRect.width() * 0.25f;
            float frogX = centerX - (frogSize / 2f);
            float frogY = currentY + 20;
            RectF frogRect = new RectF(frogX, frogY, frogX + frogSize, frogY + frogSize);
            canvas.drawBitmap(doubtFrogBitmap, null, frogRect, null);
        }

        // Dibujar el botón "Aceptar"
        float btnW = windowRect.width() * 0.4f;
        float btnH = 80;
        float marginBottom = 40;
        float btnLeft = centerX - (btnW / 2f);
        float btnTop = windowRect.bottom - btnH - marginBottom;

        btnOkRect = new RectF(btnLeft, btnTop, btnLeft + btnW, btnTop + btnH);
        drawRetroButton(canvas, btnOkRect, "Aceptar");
    }

    /**
     * Dibuja un botón con estilo retro en el rectángulo especificado con el texto proporcionado.
     *
     * @param canvas Canvas donde se dibuja.
     * @param rect   Rectángulo que define la posición y tamaño del botón.
     * @param text   Texto a mostrar en el botón.
     */
    private void drawRetroButton(Canvas canvas, RectF rect, String text) {
        // Ajustar tamaño del texto para el botón
        textPaint.setTextSize(24);
        // Dibujar fondo negro
        Paint buttonPaint = new Paint();
        buttonPaint.setColor(Color.BLACK);
        canvas.drawRect(rect, buttonPaint);
        // Dibujar borde verde (usando borderPaint ya configurado)
        canvas.drawRect(rect, borderPaint);
        // Dibujar el texto centrado
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float cx = rect.centerX();
        float cy = rect.centerY() - ((fm.ascent + fm.descent) / 2f);
        canvas.drawText(text, cx, cy, textPaint);
    }

    /**
     * Maneja los eventos táctiles para la interacción con las ventanas de alerta.
     * Detecta toques en los botones y realiza la acción correspondiente.
     *
     * @param event Evento de toque.
     * @return {@code true} si el evento es consumido, {@code false} de lo contrario.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (showNoNewLevelsWindow) {
                if (btnJugarNivel3Rect != null && btnJugarNivel3Rect.contains(x, y)) {
                    // Botón "Jugar nivel 3": lanzar GameActivity con level 3
                    Intent intent = new Intent(mContext, GameActivity.class);
                    intent.putExtra("level", 3);
                    intent.putExtra("userCurrentLevel", 4);
                    mContext.startActivity(intent);
                    hideAll();
                    return true;
                }
                if (btnSeleccionarNivelRect != null && btnSeleccionarNivelRect.contains(x, y)) {
                    // Botón "Escoger nivel": lanzar LevelSelectionActivity
                    Intent intent = new Intent(mContext, LevelSelectionActivity.class);
                    mContext.startActivity(intent);
                    hideAll();
                    return true;
                }
                return true;
            }

            if (showProximamenteWindow) {
                if (btnOkRect != null && btnOkRect.contains(x, y)) {
                    // Botón "Aceptar": ocultar la ventana
                    hideAll();
                    return true;
                }
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
}