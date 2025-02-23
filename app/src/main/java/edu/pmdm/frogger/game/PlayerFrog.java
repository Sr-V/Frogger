package edu.pmdm.frogger.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import edu.pmdm.frogger.R;

public class PlayerFrog extends FroggerObject {

    // Animaciones
    private AnimationDrawable animIdleRight;   // Un solo frame: frogger_mov5
    private AnimationDrawable animUp;            // Movimiento arriba
    private AnimationDrawable animHorizontal;    // Movimiento horizontal (mirando derecha)
    private AnimationDrawable animDeath;         // Muerte

    // La animación actual que se está dibujando
    private AnimationDrawable currentAnim;

    // Escala
    private float scaleFactor = 1.0f;

    // Posición inicial (para reinicio)
    private int initX, initY;

    // Control de muerte
    private boolean isDead = false;
    private long deathStartTime;
    // Duración total de la animación de muerte (ajustar según frogger_death.xml)
    private long deathDurationMs = 2500;

    // Control del avance de frames en SurfaceView
    private boolean isPlayingAnim = false;
    private long lastAnimRunTime = 0;
    private int animRunInterval = 150; // ms para llamar a run()

    // Indica si la rana mira a la izquierda
    private boolean facingLeft = false;

    // Interfaz para notificar el fin de la animación de muerte
    public interface DeathAnimationListener {
        void onDeathAnimationFinished();
    }

    private DeathAnimationListener deathAnimationListener;

    public void setDeathAnimationListener(DeathAnimationListener listener) {
        this.deathAnimationListener = listener;
    }

    public PlayerFrog(Context context) {
        super(context);

        // Cargar la animación "idle" (un solo frame = frogger_mov5)
        Drawable idleDrawable = context.getResources().getDrawable(R.drawable.frogger_idle);
        if (idleDrawable instanceof AnimationDrawable) {
            animIdleRight = (AnimationDrawable) idleDrawable;
        }

        // Cargar animación de movimiento horizontal (mirando a la derecha)
        Drawable horizDrawable = context.getResources().getDrawable(R.drawable.frogger_mov);
        if (horizDrawable instanceof AnimationDrawable) {
            animHorizontal = (AnimationDrawable) horizDrawable;
        }

        // Cargar animación de movimiento arriba
        Drawable upDrawable = context.getResources().getDrawable(R.drawable.frogger_up);
        if (upDrawable instanceof AnimationDrawable) {
            animUp = (AnimationDrawable) upDrawable;
        }

        // Cargar animación de muerte
        Drawable deathDrawable = context.getResources().getDrawable(R.drawable.frogger_death);
        if (deathDrawable instanceof AnimationDrawable) {
            animDeath = (AnimationDrawable) deathDrawable;
        }

        // Empezar con la animación idle (frogger_mov5)
        setCurrentAnim(animIdleRight);
        facingLeft = false; // Por defecto, mirando a la derecha
    }

    /**
     * Ajusta la escala de la rana.
     * Incrementa desiredRatio para hacerla más grande.
     */
    public void configureScale(int mapHeight, float desiredRatio) {
        if (animHorizontal != null) {
            int h = animHorizontal.getIntrinsicHeight();
            float desiredHeight = mapHeight * desiredRatio;
            scaleFactor = (desiredHeight / h) * 1.2f;
        }
    }

    /**
     * Guarda la posición inicial (para respawn).
     */
    public void storeInitialPosition(int x, int y) {
        initX = x;
        initY = y;
        setPosition(x, y);

        // Al spawnear => animación idle (mirando a la derecha)
        setCurrentAnim(animIdleRight);
        facingLeft = false;
    }

    @Override
    public void update() {
        long now = SystemClock.uptimeMillis();

        // Avance manual de la animación
        if (currentAnim != null && isPlayingAnim && (now - lastAnimRunTime >= animRunInterval)) {
            currentAnim.run();
            lastAnimRunTime = now;
            if (!currentAnim.isRunning()) {
                isPlayingAnim = false;
            }
        }

        // Si está en animación de muerte, verificar si se ha superado la duración
        if (isDead) {
            long elapsed = now - deathStartTime;
            if (elapsed > deathDurationMs) {
                // Termina la animación de muerte: reinicia la posición y vuelve a la animación idle
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

    @Override
    public void draw(Canvas canvas) {
        if (currentAnim != null && canvas != null) {
            int w = currentAnim.getIntrinsicWidth();
            int h = currentAnim.getIntrinsicHeight();

            w = (int) (w * scaleFactor);
            h = (int) (h * scaleFactor);

            canvas.save();

            // Si mira a la izquierda, aplicar flip horizontal
            if (facingLeft) {
                float centerX = x + w / 2f;
                float centerY = y + h / 2f;
                canvas.scale(-1, 1, centerX, centerY);
            }

            currentAnim.setBounds(x, y, x + w, y + h);
            currentAnim.draw(canvas);
            canvas.restore();
        }
    }

    /**
     * Reproduce la animación de muerte.
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
     * Resetea la posición de la rana.
     */
    public void resetPosition() {
        setPosition(initX, initY);
    }

    public float getScaledWidth() {
        if (animHorizontal != null) {
            int w = animHorizontal.getIntrinsicWidth();
            return w * scaleFactor;
        }
        return 0;
    }

    public float getScaledHeight() {
        if (animHorizontal != null) {
            int h = animHorizontal.getIntrinsicHeight();
            return h * scaleFactor;
        }
        return 0;
    }

    // Movimientos

    public void moveLeft() {
        if (isDead) return;
        facingLeft = true;
        startAnimation(animHorizontal);
    }

    public void moveRight() {
        if (isDead) return;
        facingLeft = false;
        startAnimation(animHorizontal);
    }

    public void moveUpSmall() {
        if (isDead) return;
        y -= 50;
        facingLeft = false;
        startAnimation(animUp);
    }

    /**
     * Inicia una animación reiniciándola y marcándola como en reproducción.
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
     * Asigna la animación actual sin iniciarla (queda en el primer frame).
     */
    private void setCurrentAnim(AnimationDrawable anim) {
        if (anim == null) return;
        currentAnim = anim;
        currentAnim.stop();
        isPlayingAnim = false;
    }

    public boolean isDead() {
        return isDead;
    }

}