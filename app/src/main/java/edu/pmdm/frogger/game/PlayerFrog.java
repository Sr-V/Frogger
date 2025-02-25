package edu.pmdm.frogger.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

import edu.pmdm.frogger.R;

public class PlayerFrog extends FroggerObject {

    // Animaciones
    private AnimationDrawable animIdleRight;
    private AnimationDrawable animUp;
    private AnimationDrawable animHorizontal;
    private AnimationDrawable animDeath;

    private AnimationDrawable currentAnim;

    private float scaleFactor = 1.0f;
    private int initX, initY;

    private boolean isDead = false;
    private long deathStartTime;
    private long deathDurationMs = 2500;

    private boolean isPlayingAnim = false;
    private long lastAnimRunTime = 0;
    private int animRunInterval = 150;

    private boolean facingLeft = false;

    public interface DeathAnimationListener {
        void onDeathAnimationFinished();
    }

    private DeathAnimationListener deathAnimationListener;

    public void setDeathAnimationListener(DeathAnimationListener listener) {
        this.deathAnimationListener = listener;
    }

    public PlayerFrog(Context context) {
        super(context);

        Drawable idleDrawable = context.getResources().getDrawable(R.drawable.frogger_idle);
        if (idleDrawable instanceof AnimationDrawable) {
            animIdleRight = (AnimationDrawable) idleDrawable;
        }

        Drawable horizDrawable = context.getResources().getDrawable(R.drawable.frogger_mov);
        if (horizDrawable instanceof AnimationDrawable) {
            animHorizontal = (AnimationDrawable) horizDrawable;
        }

        Drawable upDrawable = context.getResources().getDrawable(R.drawable.frogger_up);
        if (upDrawable instanceof AnimationDrawable) {
            animUp = (AnimationDrawable) upDrawable;
        }

        Drawable deathDrawable = context.getResources().getDrawable(R.drawable.frogger_death);
        if (deathDrawable instanceof AnimationDrawable) {
            animDeath = (AnimationDrawable) deathDrawable;
        }

        setCurrentAnim(animIdleRight);
        facingLeft = false;
    }

    public void configureScale(int mapHeight, float desiredRatio) {
        if (animHorizontal != null) {
            int h = animHorizontal.getIntrinsicHeight();
            float desiredHeight = mapHeight * desiredRatio;
            scaleFactor = (desiredHeight / h) * 1.2f;
        }
    }

    public void storeInitialPosition(int x, int y) {
        initX = x;
        initY = y;
        setPosition(x, y);
        setCurrentAnim(animIdleRight);
        facingLeft = false;
    }

    @Override
    public void update() {
        long now = SystemClock.uptimeMillis();

        if (currentAnim != null && isPlayingAnim && (now - lastAnimRunTime >= animRunInterval)) {
            currentAnim.run();
            lastAnimRunTime = now;
            if (!currentAnim.isRunning()) {
                isPlayingAnim = false;
            }
        }

        if (isDead) {
            long elapsed = now - deathStartTime;
            if (elapsed > deathDurationMs) {
                resetPosition();
                setCurrentAnim(animIdleRight);
                isDead = false;
                isPlayingAnim = false;
                facingLeft = false;
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

    private void startAnimation(AnimationDrawable anim) {
        if (anim == null) return;
        currentAnim = anim;
        currentAnim.stop();
        currentAnim.start();
        isPlayingAnim = true;
        lastAnimRunTime = 0;
    }

    private void setCurrentAnim(AnimationDrawable anim) {
        if (anim == null) return;
        currentAnim = anim;
        currentAnim.stop();
        isPlayingAnim = false;
    }

    public boolean isDead() {
        return isDead;
    }

    @Override
    public RectF getBoundingBox() {
        float paddingX = getScaledWidth() * 0.15f;
        float paddingY = getScaledHeight() * 0.15f;
        return new RectF(x + paddingX, y + paddingY, x + getScaledWidth() - paddingX, y + getScaledHeight() - paddingY);
    }

    @Override
    public RectF getBoundingBoxPrecise() {
        float scaledWidth = getScaledWidth();
        float scaledHeight = getScaledHeight();
        float paddingX = scaledWidth * 0.15f;
        float paddingY = scaledHeight * 0.15f;
        return new RectF(x + paddingX, y + paddingY, x + scaledWidth - paddingX, y + scaledHeight - paddingY);
    }

}
