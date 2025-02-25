package edu.pmdm.frogger.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

public abstract class FroggerObject {

    protected int x, y;
    protected int width, height;
    protected Bitmap sprite;
    protected Context context;

    public FroggerObject(Context context) {
        this.context = context;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public abstract void update();

    public void draw(Canvas canvas) {
        if (sprite != null && canvas != null) {
            canvas.drawBitmap(sprite, x, y, null);
        }
    }

    /**
     * Bounding box con padding (para colisiones más precisas).
     */
    public RectF getBoundingBoxPrecise() {
        float paddingX = width * 0.15f;
        float paddingY = height * 0.15f;
        return new RectF(x + paddingX, y + paddingY, x + width - paddingX, y + height - paddingY);
    }

    public abstract RectF getBoundingBox();
}
