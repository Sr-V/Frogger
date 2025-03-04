package edu.pmdm.frogger.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.activities.LevelSelectionActivity;
import edu.pmdm.frogger.activities.GameActivity;
import edu.pmdm.frogger.utils.GameAudioManager;

/**
 * Un overlay que dibuja ventanas retro de color negro/verde,
 * en lugar de usar AlertDialog.
 */
public class AlertsOverlayView extends View {

    // Booleans para las 2 ventanas que quieres dibujar:
    private boolean showNoNewLevelsWindow = false;   // "Niveles no disponibles" (MainActivity)
    private boolean showProximamenteWindow = false;  // "Nivel no disponible" (LevelSelectionActivity)

    private Bitmap thinkingFrogBitmap;
    private Bitmap doubtFrogBitmap;

    // Para pintar textos y contornos
    private Paint borderPaint, bgPaint, textPaint;
    private Typeface retroTypeface;

    // Rectángulos para botones en "Niveles no disponibles"
    private RectF btnJugarNivel3Rect;
    private RectF btnSeleccionarNivelRect;

    // Rectángulo para botón en "Proximamente"
    private RectF btnOkRect;

    // Referencia a la Activity, por si necesitas iniciar Intents:
    private final Context mContext;

    public AlertsOverlayView(Context context) {
        super(context);
        mContext = context;
        init();
    }
    public AlertsOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        thinkingFrogBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.thinking_frog);
        doubtFrogBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.doubt_frog);

        // Pinturas
        borderPaint = new Paint();
        borderPaint.setColor(Color.GREEN);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(8);

        bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);

        textPaint = new Paint();
        textPaint.setColor(Color.GREEN);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(28);
        retroTypeface = ResourcesCompat.getFont(getContext(), R.font.press_start_2p);
        textPaint.setTypeface(retroTypeface);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    // Métodos para mostrar/ocultar ventanas
    public void showNoNewLevelsWindow() {
        showNoNewLevelsWindow = true;
        showProximamenteWindow = false;
        invalidate();
        setVisibility(VISIBLE);
    }
    public void showProximamenteWindow() {
        showProximamenteWindow = true;
        showNoNewLevelsWindow = false;
        invalidate();
        setVisibility(VISIBLE);
    }
    public void hideAll() {
        showNoNewLevelsWindow = false;
        showProximamenteWindow = false;
        setVisibility(GONE);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Si no se muestra ninguna ventana, no dibujamos nada
        if (!showNoNewLevelsWindow && !showProximamenteWindow) {
            return;
        }

        int w = getWidth();
        int h = getHeight();

        // Fondo overlay semi-transparente
        Paint overlayPaint = new Paint();
        overlayPaint.setColor(Color.argb(200, 0, 0, 0));
        canvas.drawRect(0, 0, w, h, overlayPaint);

        // Ventana centrada
        int windowWidth = (int) (w * 0.75f);
        int windowHeight = (int) (h * 0.35f);
        int left = (w - windowWidth) / 2;
        int top = (h - windowHeight) / 2;
        RectF windowRect = new RectF(left, top, left + windowWidth, top + windowHeight);

        // Fondo negro
        canvas.drawRect(windowRect, bgPaint);
        // Borde verde
        canvas.drawRect(windowRect, borderPaint);

        if (showNoNewLevelsWindow) {
            drawNoNewLevelsWindow(canvas, windowRect);
        }
        else if (showProximamenteWindow) {
            drawProximamenteWindow(canvas, windowRect);
        }
    }

    private void drawNoNewLevelsWindow(Canvas canvas, RectF windowRect) {
        // "Niveles no disponibles"
        // "No existen niveles nuevos. El último nivel disponible es el 3..."
        // "¿Deseas ir a la selección de niveles o jugar el último nivel disponible?"

        // Título
        String title = "Nivel no disponible";
        // Mensaje
        String confirmMessage =
                "\n\n" +
                        "No existen niveles nuevos.\nEl último nivel es el 3.\n\n" +
                        "¿Qué quieres hacer?";

        float centerX = windowRect.centerX();
        float currentY = windowRect.top + (windowRect.height() * 0.1f);

        // Dibujar título
        canvas.drawText(title, centerX, currentY, textPaint);
        currentY += 50;

        for (String line : confirmMessage.split("\n")) {
            canvas.drawText(line, centerX, currentY, textPaint);
            currentY += 40; // Ajusta la separación
        }

        if (thinkingFrogBitmap != null) {
            float frogSize = windowRect.width() * 0.25f; // Ajusta el tamaño
            float frogX = (canvas.getWidth() / 2f) - (frogSize / 2f); // Centrar horizontalmente
            float frogY = currentY + 20; // Un poco más abajo del texto
            RectF frogRect = new RectF(frogX, frogY, frogX + frogSize, frogY + frogSize);
            canvas.drawBitmap(thinkingFrogBitmap, null, frogRect, null);

            // Actualizar currentY para que los botones se posicionen más abajo
            currentY = frogY + frogSize + 20;
        }

        // Botones
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

    private void drawProximamenteWindow(Canvas canvas, RectF windowRect) {
        // "Nivel no disponible"
        // "Este nivel aún no se ha desarrollado."
        // Botón "Aceptar"

        textPaint.setTextSize(30);
        String title = "Nivel no disponible";
        String msg = "¡Lo sentimos!\nEste nivel aún\nno se ha desarrollado.";

        float centerX = windowRect.centerX();
        float currentY = windowRect.top + (windowRect.height() * 0.25f);

        // Dibujar título
        canvas.drawText(title, centerX, currentY, textPaint);
        currentY += 60;

        // Dibujar msg (varias líneas)
        for (String line : msg.split("\n")) {
            canvas.drawText(line, centerX, currentY, textPaint);
            currentY += 40;
        }

        // DIBUJAR doubt_frog centrado, debajo del texto
        if (doubtFrogBitmap != null) {
            float frogSize = windowRect.width() * 0.25f;  // Ajusta el tamaño
            float frogX = centerX - (frogSize / 2f);      // Centrado horizontal
            float frogY = currentY + 20;                  // Un poco más abajo del texto
            RectF frogRect = new RectF(frogX, frogY, frogX + frogSize, frogY + frogSize);
            canvas.drawBitmap(doubtFrogBitmap, null, frogRect, null);

        }

        // Botón "Aceptar"
        float btnW = windowRect.width() * 0.4f;
        float btnH = 80;
        float marginBottom = 40;
        float btnLeft = centerX - (btnW / 2f);
        float btnTop = windowRect.bottom - btnH - marginBottom;

        btnOkRect = new RectF(btnLeft, btnTop, btnLeft + btnW, btnTop + btnH);
        drawRetroButton(canvas, btnOkRect, "Aceptar");
    }

    private void drawRetroButton(Canvas canvas, RectF rect, String text) {
        textPaint.setTextSize(24);
        // Fondo negro
        Paint buttonPaint = new Paint();
        buttonPaint.setColor(Color.BLACK);
        canvas.drawRect(rect, buttonPaint);

        // Borde verde
        canvas.drawRect(rect, borderPaint);

        // Texto verde
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float cx = rect.centerX();
        float cy = rect.centerY() - ((fm.ascent + fm.descent) / 2f);
        canvas.drawText(text, cx, cy, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (showNoNewLevelsWindow) {
                if (btnJugarNivel3Rect != null && btnJugarNivel3Rect.contains(x, y)) {
                    // Simula "Jugar nivel 3"
                    // Lanza la GameActivity con level=3
                    Intent intent = new Intent(mContext, GameActivity.class);
                    intent.putExtra("level", 3);
                    intent.putExtra("userCurrentLevel", 4); // Por si quieres
                    mContext.startActivity(intent);
                    hideAll();
                    return true;
                }
                if (btnSeleccionarNivelRect != null && btnSeleccionarNivelRect.contains(x, y)) {
                    // Simula "Seleccionar nivel"
                    Intent intent = new Intent(mContext, LevelSelectionActivity.class);
                    mContext.startActivity(intent);
                    hideAll();
                    return true;
                }
                return true; // Consumimos el toque
            }

            if (showProximamenteWindow) {
                if (btnOkRect != null && btnOkRect.contains(x, y)) {
                    // Simula "Aceptar"
                    hideAll();
                    return true;
                }
                return true;
            }
        }
        return super.onTouchEvent(event);
    }
}