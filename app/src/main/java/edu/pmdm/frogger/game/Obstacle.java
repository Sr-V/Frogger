package edu.pmdm.frogger.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;

import java.util.Random;

import edu.pmdm.frogger.R;

public class Obstacle extends FroggerObject {

    // Caching de los sprites (o colores)
    private static Bitmap[] cachedBitmaps = null;
    private static int lastDrawableId = -1;

    private Bitmap originalSprite;
    private float scaleFactor = 1.0f;
    private int speed;
    private int direction;  // +1: derecha, -1: izquierda
    private int screenWidth;

    public Obstacle(Context context, int startX, int startY, int drawableId) {
        super(context);
        Random rand = new Random();

        if (cachedBitmaps == null || lastDrawableId != drawableId) {
            loadBitmaps(context, drawableId);
            lastDrawableId = drawableId;
        }

        int randomIndex = rand.nextInt(cachedBitmaps.length);
        originalSprite = cachedBitmaps[randomIndex];
        sprite = originalSprite;
        width = sprite.getWidth();
        height = sprite.getHeight();

        x = startX;
        y = startY;

        speed = rand.nextInt(6) + 3;
        direction = rand.nextBoolean() ? 1 : -1;
    }

    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    public void configureScale(int mapHeight, float desiredRatio) {
        int hOriginal = originalSprite.getHeight();
        float desiredHeight = mapHeight * desiredRatio;
        scaleFactor = desiredHeight / hOriginal;

        int newW = (int) (originalSprite.getWidth() * scaleFactor);
        int newH = (int) (originalSprite.getHeight() * scaleFactor);

        sprite = Bitmap.createScaledBitmap(originalSprite, newW, newH, true);
        width = sprite.getWidth();
        height = sprite.getHeight();
    }

    @Override
    public void update() {
        x += speed * direction;

        if (direction > 0 && x > screenWidth) {
            randomizeProperties();
        } else if (direction < 0 && (x + width) < 0) {
            randomizeProperties();
        }
    }

    private void randomizeProperties() {
        Random rand = new Random();

        // No re-asignamos la velocidad para mantenerla
        // speed = rand.nextInt(6) + 3;

        // Reasignamos la dirección y el sprite para dar variación.
        direction = rand.nextBoolean() ? 1 : -1;

        int randomIndex = rand.nextInt(cachedBitmaps.length);
        originalSprite = cachedBitmaps[randomIndex];

        int newW = (int) (originalSprite.getWidth() * scaleFactor);
        int newH = (int) (originalSprite.getHeight() * scaleFactor);
        sprite = Bitmap.createScaledBitmap(originalSprite, newW, newH, true);
        width = sprite.getWidth();
        height = sprite.getHeight();

        if (direction > 0) {
            x = -width;
        } else {
            x = screenWidth;
        }
    }

    private static void loadBitmaps(Context context, int drawableId) {
        Bitmap fullSheet = BitmapFactory.decodeResource(context.getResources(), drawableId);
        int sheetWidth = fullSheet.getWidth();
        int sheetHeight = fullSheet.getHeight();

        int singleWidth = sheetWidth / 2;
        int singleHeight = sheetHeight / 2;

        cachedBitmaps = new Bitmap[4];
        cachedBitmaps[0] = Bitmap.createBitmap(fullSheet, 0, 0, singleWidth, singleHeight);
        cachedBitmaps[1] = Bitmap.createBitmap(fullSheet, singleWidth, 0, singleWidth, singleHeight);
        cachedBitmaps[2] = Bitmap.createBitmap(fullSheet, 0, singleHeight, singleWidth, singleHeight);
        cachedBitmaps[3] = Bitmap.createBitmap(fullSheet, singleWidth, singleHeight, singleWidth, singleHeight);

        fullSheet.recycle();
    }

    // Método agregado para ajustar la velocidad según el nivel.
    public void setSpeed(int speed) {
        this.speed = speed;
    }

    @Override
    public void draw(Canvas canvas) {
        if (sprite == null || canvas == null) return;

        canvas.save();
        if (direction > 0) {
            float centerX = x + (width / 2f);
            float centerY = y + (height / 2f);
            canvas.scale(-1, 1, centerX, centerY);
        }
        canvas.drawBitmap(sprite, x, y, null);
        canvas.restore();
    }

    @Override
    public RectF getBoundingBox() {
        return getBoundingBoxPrecise();
    }
}