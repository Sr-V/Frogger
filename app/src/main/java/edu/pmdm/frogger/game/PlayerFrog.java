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
    private AnimationDrawable animUp;          // Mov. arriba
    private AnimationDrawable animHorizontal;  // Mov. horizontal (mirando derecha)
    private AnimationDrawable animDeath;       // Muerte

    // La animación actual que se está dibujando
    private AnimationDrawable currentAnim;

    // Escala
    private float scaleFactor = 1.0f;

    // Posición inicial (reset)
    private int initX, initY;

    // Control de muerte
    private boolean isDead = false;
    private long deathStartTime;
    // Ajusta según la duración total de frogger_death.xml
    private long deathDurationMs = 2500;

    // Indica si la anim se está reproduciendo
    private boolean isPlayingAnim = false;

    // Control del avance de frames en SurfaceView
    private long lastAnimRunTime = 0;
    private int animRunInterval = 150; // ms para llamar a run()

    // Indica si la rana mira a la izquierda
    private boolean facingLeft = false;

    public PlayerFrog(Context context) {
        super(context);

        // Cargar la animación "idle" (un solo frame = frogger_mov5)
        Drawable idleDrawable = context.getResources().getDrawable(R.drawable.frogger_idle);
        if (idleDrawable instanceof AnimationDrawable) {
            animIdleRight = (AnimationDrawable) idleDrawable;
        }

        // Cargar animación de moverse horizontal (mirando a la derecha)
        Drawable horizDrawable = context.getResources().getDrawable(R.drawable.frogger_mov);
        if (horizDrawable instanceof AnimationDrawable) {
            animHorizontal = (AnimationDrawable) horizDrawable;
        }

        // Cargar animación de moverse arriba
        Drawable upDrawable = context.getResources().getDrawable(R.drawable.frogger_up);
        if (upDrawable instanceof AnimationDrawable) {
            animUp = (AnimationDrawable) upDrawable;
        }

        // Cargar animación de muerte
        Drawable deathDrawable = context.getResources().getDrawable(R.drawable.frogger_death);
        if (deathDrawable instanceof AnimationDrawable) {
            animDeath = (AnimationDrawable) deathDrawable;
        }

        // Empezamos con la animación "idle" (frogger_mov5) para que se vea esa imagen
        setCurrentAnim(animIdleRight);
        facingLeft = false; // Por defecto, mirando a la derecha
    }

    /**
     * Ajusta la escala de la rana.
     * Subiendo el desiredRatio la haces más grande.
     */
    public void configureScale(int mapHeight, float desiredRatio) {
        // Usamos animHorizontal como referencia
        if (animHorizontal != null) {
            int h = animHorizontal.getIntrinsicHeight();
            // Calculamos la altura deseada
            float desiredHeight = mapHeight * desiredRatio;
            // Aumentamos un poco más (p.ej. 1.2) si quieres agrandarla extra
            scaleFactor = (desiredHeight / h) * 1.2f;
        }
    }

    /**
     * Guardamos la posición inicial (punto de respawn).
     */
    public void storeInitialPosition(int x, int y) {
        initX = x;
        initY = y;
        setPosition(x, y);

        // Al spawnear => idle (mirando a la derecha)
        setCurrentAnim(animIdleRight);
        facingLeft = false;
    }

    @Override
    public void update() {
        long now = SystemClock.uptimeMillis();

        // Avance manual de la anim
        if (currentAnim != null && isPlayingAnim && (now - lastAnimRunTime >= animRunInterval)) {
            currentAnim.run();
            lastAnimRunTime = now;
            if (!currentAnim.isRunning()) {
                // Terminó => se queda en el último frame
                isPlayingAnim = false;
            }
        }

        // Si está en anim de muerte, ver si superó deathDurationMs
        if (isDead) {
            long elapsed = now - deathStartTime;
            if (elapsed > deathDurationMs) {
                // Termina la muerte => reset
                resetPosition();
                setCurrentAnim(animIdleRight);
                isDead = false;
                isPlayingAnim = false;
                facingLeft = false;
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (currentAnim != null && canvas != null) {
            // Tomamos ancho/alto del frame
            int w = currentAnim.getIntrinsicWidth();
            int h = currentAnim.getIntrinsicHeight();

            w = (int) (w * scaleFactor);
            h = (int) (h * scaleFactor);

            // Guardamos el estado
            canvas.save();

            // Si mira a la izquierda => flip horizontal
            if (facingLeft) {
                float centerX = x + w / 2f;
                float centerY = y + h / 2f;
                canvas.scale(-1, 1, centerX, centerY);
            }

            // Dibujamos
            currentAnim.setBounds(x, y, x + w, y + h);
            currentAnim.draw(canvas);

            // Restauramos
            canvas.restore();
        }
    }

    /**
     * Reproducir anim de muerte.
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
     * Resetear a la posición inicial.
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
        startAnimation(animHorizontal); // oneshot => se reproducirá y quedará en último frame
    }

    public void moveRight() {
        if (isDead) return;
        facingLeft = false;
        startAnimation(animHorizontal);
    }

    public void moveUpSmall() {
        if (isDead) return;
        y -= 50;
        // Normalmente, al subir, la rana mira arriba => no hay flip horizontal
        facingLeft = false; // o la dejas como estaba
        startAnimation(animUp);
    }

    /**
     * Iniciar la anim => la reiniciamos y marcamos isPlayingAnim = true
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
     * Asigna la anim actual sin iniciarla (queda en el primer frame).
     */
    private void setCurrentAnim(AnimationDrawable anim) {
        if (anim == null) return;
        currentAnim = anim;
        currentAnim.stop(); // se queda en frame 0
        isPlayingAnim = false;
    }
}