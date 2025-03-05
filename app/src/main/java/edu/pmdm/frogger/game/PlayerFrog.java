package edu.pmdm.frogger.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import edu.pmdm.frogger.R;

/**
 * {@code PlayerFrog} representa la rana controlada por el jugador en el juego Frogger.
 * Maneja las animaciones, el movimiento y el estado de la rana (viva o muerta).
 * Además, implementa métodos para actualizar y dibujar la rana, y para reproducir animaciones
 * específicas (por ejemplo, la animación de muerte).
 */
public class PlayerFrog extends FroggerObject {

    // Animaciones disponibles para la rana
    private AnimationDrawable animIdleRight;   // Animación de inactividad mirando a la derecha
    private AnimationDrawable animDown;          // Animación al moverse hacia abajo
    private AnimationDrawable animUp;            // Animación al moverse hacia arriba
    private AnimationDrawable animHorizontal;    // Animación al moverse horizontalmente
    private AnimationDrawable animDeath;         // Animación de muerte

    // Animación actualmente activa
    private AnimationDrawable currentAnim;

    // Factor de escala para redimensionar la rana
    private float scaleFactor = 1.0f;
    // Posición inicial de la rana (para reinicios)
    private int initX, initY;

    // Estado de la rana: true si está muerta
    private boolean isDead = false;
    // Tiempo de inicio de la animación de muerte
    private long deathStartTime;
    // Duración total de la animación de muerte en milisegundos
    private long deathDurationMs = 2500;

    // Variables para controlar la reproducción de la animación (para no saturar la llamada a run())
    private boolean isPlayingAnim = false;
    private long lastAnimRunTime = 0;
    // Intervalo mínimo entre ejecuciones de la animación (en milisegundos)
    private int animRunInterval = 150;

    // Indica si la rana está mirando a la izquierda (para voltear la animación)
    private boolean facingLeft = false;

    /**
     * Interfaz para notificar cuando la animación de muerte ha finalizado.
     */
    public interface DeathAnimationListener {
        void onDeathAnimationFinished();
    }

    // Listener para la animación de muerte
    private DeathAnimationListener deathAnimationListener;

    /**
     * Registra un listener para la animación de muerte.
     *
     * @param listener Instancia de {@code DeathAnimationListener} que recibirá la notificación.
     */
    public void setDeathAnimationListener(DeathAnimationListener listener) {
        this.deathAnimationListener = listener;
    }

    /**
     * Constructor de {@code PlayerFrog}.
     * Carga las animaciones y establece la animación de inactividad por defecto.
     *
     * @param context Contexto de la aplicación.
     */
    public PlayerFrog(Context context) {
        super(context);

        // Cargar la animación de inactividad (idle) mirando a la derecha
        Drawable idleDrawable = context.getResources().getDrawable(R.drawable.frogger_idle);
        if (idleDrawable instanceof AnimationDrawable) {
            animIdleRight = (AnimationDrawable) idleDrawable;
        }

        // Cargar la animación para el movimiento horizontal
        Drawable horizDrawable = context.getResources().getDrawable(R.drawable.frogger_mov);
        if (horizDrawable instanceof AnimationDrawable) {
            animHorizontal = (AnimationDrawable) horizDrawable;
        }

        // Cargar la animación para moverse hacia arriba
        Drawable upDrawable = context.getResources().getDrawable(R.drawable.frogger_up);
        if (upDrawable instanceof AnimationDrawable) {
            animUp = (AnimationDrawable) upDrawable;
        }

        // Cargar la animación para moverse hacia abajo
        Drawable downDrawable = context.getResources().getDrawable(R.drawable.frogger_down);
        if (downDrawable instanceof AnimationDrawable) {
            animDown = (AnimationDrawable) downDrawable;
        }

        // Cargar la animación de muerte
        Drawable deathDrawable = context.getResources().getDrawable(R.drawable.frogger_death);
        if (deathDrawable instanceof AnimationDrawable) {
            animDeath = (AnimationDrawable) deathDrawable;
        }

        // Establecer la animación por defecto y dirección de la rana
        setCurrentAnim(animIdleRight);
        facingLeft = false;
    }

    /**
     * Configura la escala de las animaciones de la rana en función de la altura del mapa y una proporción deseada.
     *
     * @param mapHeight    Altura total del mapa.
     * @param desiredRatio Proporción deseada para la altura de la rana en relación con el mapa.
     */
    public void configureScale(int mapHeight, float desiredRatio) {
        if (animHorizontal != null) {
            int h = animHorizontal.getIntrinsicHeight();
            float desiredHeight = mapHeight * desiredRatio;
            // Se aplica un factor adicional de 1.2 para ajustar la escala visual
            scaleFactor = (desiredHeight / h) * 1.2f;
        }
    }

    /**
     * Guarda la posición inicial de la rana, utilizada para reiniciar su posición tras una muerte.
     *
     * @param x Coordenada X inicial.
     * @param y Coordenada Y inicial.
     */
    public void storeInitialPosition(int x, int y) {
        initX = x;
        initY = y;
        setPosition(x, y);
        setCurrentAnim(animIdleRight);
        facingLeft = false;
    }

    /**
     * Actualiza la lógica de la rana, controlando la reproducción de animaciones y
     * reiniciando la posición tras la muerte, cuando la animación de muerte ha finalizado.
     */
    @Override
    public void update() {
        long now = SystemClock.uptimeMillis();

        // Si se está reproduciendo una animación, se avanza según el intervalo definido
        if (currentAnim != null && isPlayingAnim && (now - lastAnimRunTime >= animRunInterval)) {
            currentAnim.run();
            lastAnimRunTime = now;
            // Si la animación ha dejado de correr, se detiene la reproducción
            if (!currentAnim.isRunning()) {
                isPlayingAnim = false;
            }
        }

        // Si la rana está muerta, comprobar si ha transcurrido la duración de la animación de muerte
        if (isDead) {
            long elapsed = now - deathStartTime;
            if (elapsed > deathDurationMs) {
                // Reiniciar la posición y restablecer el estado de la rana
                resetPosition();
                setCurrentAnim(animIdleRight);
                isDead = false;
                isPlayingAnim = false;
                facingLeft = false;
                // Notificar que la animación de muerte ha finalizado
                if (deathAnimationListener != null) {
                    deathAnimationListener.onDeathAnimationFinished();
                }
            }
        }
    }

    /**
     * Dibuja la rana en el canvas, aplicando la animación actual escalada según el factor de escala.
     * Si la rana está mirando a la izquierda, se voltea la imagen horizontalmente.
     *
     * @param canvas Canvas sobre el que se dibuja la rana.
     */
    @Override
    public void draw(Canvas canvas) {
        if (currentAnim != null && canvas != null) {
            int w = currentAnim.getIntrinsicWidth();
            int h = currentAnim.getIntrinsicHeight();
            // Ajustar dimensiones según el factor de escala
            w = (int) (w * scaleFactor);
            h = (int) (h * scaleFactor);

            canvas.save();
            // Si la rana está mirando a la izquierda, se aplica una transformación de escala negativa en X
            if (facingLeft) {
                float centerX = x + w / 2f;
                float centerY = y + h / 2f;
                canvas.scale(-1, 1, centerX, centerY);
            }
            // Establecer los límites para la animación y dibujarla
            currentAnim.setBounds(x, y, x + w, y + h);
            currentAnim.draw(canvas);
            canvas.restore();
        }
    }

    /**
     * Reproduce la animación de muerte de la rana.
     * Establece el estado de la rana a muerta, inicia la animación de muerte y registra el tiempo de inicio.
     */
    public void playDeathAnimation() {
        if (animDeath == null) return;
        currentAnim = animDeath;
        currentAnim.stop();
        currentAnim.start();
        isPlayingAnim = true;
        isDead = true;
        deathStartTime = SystemClock.uptimeMillis();
        lastAnimRunTime = 0;
    }

    /**
     * Reinicia la posición de la rana a su posición inicial guardada.
     */
    public void resetPosition() {
        setPosition(initX, initY);
    }

    /**
     * Devuelve el ancho escalado de la rana.
     *
     * @return Ancho escalado de la animación horizontal.
     */
    public float getScaledWidth() {
        if (animHorizontal != null) {
            int w = animHorizontal.getIntrinsicWidth();
            return w * scaleFactor;
        }
        return 0;
    }

    /**
     * Devuelve la altura escalada de la rana.
     *
     * @return Altura escalada de la animación horizontal.
     */
    public float getScaledHeight() {
        if (animHorizontal != null) {
            int h = animHorizontal.getIntrinsicHeight();
            return h * scaleFactor;
        }
        return 0;
    }

    /**
     * Mueve la rana hacia la izquierda, activa la animación horizontal y marca que la rana mira a la izquierda.
     */
    public void moveLeft() {
        if (isDead) return;
        facingLeft = true;
        startAnimation(animHorizontal);
    }

    /**
     * Mueve la rana hacia abajo, activa la animación de movimiento hacia abajo y marca la dirección.
     */
    public void moveDown() {
        if (isDead) return;
        facingLeft = true;
        startAnimation(animDown);
    }

    /**
     * Mueve la rana hacia la derecha, activa la animación horizontal y marca que la rana no está mirando a la izquierda.
     */
    public void moveRight() {
        if (isDead) return;
        facingLeft = false;
        startAnimation(animHorizontal);
    }

    /**
     * Mueve la rana hacia arriba (pequeño salto) y activa la animación de movimiento hacia arriba.
     * Además, reduce la posición Y para simular el salto.
     */
    public void moveUpSmall() {
        if (isDead) return;
        y -= 50;  // Ajusta la posición Y para el salto
        facingLeft = false;
        startAnimation(animUp);
    }

    /**
     * Inicia la reproducción de la animación especificada.
     *
     * @param anim Animación que se va a reproducir.
     */
    private void startAnimation(AnimationDrawable anim) {
        if (anim == null) return;
        currentAnim = anim;
        currentAnim.stop();
        currentAnim.start();
        isPlayingAnim = true;
        lastAnimRunTime = 0;
    }

    /**
     * Establece la animación actual sin reproducirla.
     *
     * @param anim Animación a establecer.
     */
    private void setCurrentAnim(AnimationDrawable anim) {
        if (anim == null) return;
        currentAnim = anim;
        currentAnim.stop();
        isPlayingAnim = false;
    }

    /**
     * Indica si la rana está muerta.
     *
     * @return {@code true} si la rana está en estado de muerte, {@code false} en caso contrario.
     */
    public boolean isDead() {
        return isDead;
    }

    /**
     * Retorna la caja de colisión de la rana con padding basado en el ancho y alto escalados.
     *
     * @return {@code RectF} que representa la bounding box de la rana.
     */
    @Override
    public RectF getBoundingBox() {
        float paddingX = getScaledWidth() * 0.15f;
        float paddingY = getScaledHeight() * 0.15f;
        return new RectF(x + paddingX, y + paddingY, x + getScaledWidth() - paddingX, y + getScaledHeight() - paddingY);
    }

    /**
     * Retorna la caja de colisión precisa de la rana, similar a {@link #getBoundingBox()}.
     *
     * @return {@code RectF} que representa la bounding box precisa de la rana.
     */
    @Override
    public RectF getBoundingBoxPrecise() {
        float scaledWidth = getScaledWidth();
        float scaledHeight = getScaledHeight();
        float paddingX = scaledWidth * 0.15f;
        float paddingY = scaledHeight * 0.15f;
        return new RectF(x + paddingX, y + paddingY, x + scaledWidth - paddingX, y + scaledHeight - paddingY);
    }
}