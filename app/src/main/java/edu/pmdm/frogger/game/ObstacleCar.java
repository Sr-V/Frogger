package edu.pmdm.frogger.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import java.util.Random;

import edu.pmdm.frogger.R;

public class ObstacleCar extends FroggerObject {

    private static Bitmap[] carBitmaps = null;
    private Bitmap originalCar; // sub-bitmap sin escalar
    private float scaleFactor = 1.0f;
    private int speed;
    private int direction;  // +1 => va de izq a der, -1 => va de der a izq

    public ObstacleCar(Context context, int startX, int startY) {
        super(context);

        // Cargar (si no se han cargado ya) los sub-bitmaps del spritesheet
        if (carBitmaps == null) {
            loadCarBitmaps(context);
        }

        // Elegir uno de los 4 coches aleatoriamente
        Random rand = new Random();
        int randomIndex = rand.nextInt(carBitmaps.length);
        originalCar = carBitmaps[randomIndex];

        // El sprite inicial es el sub-bitmap (por defecto mirando a la IZQUIERDA)
        sprite = originalCar;
        width = sprite.getWidth();
        height = sprite.getHeight();

        // Posición inicial
        x = startX;
        y = startY;

        // Velocidad aleatoria (3..8)
        speed = rand.nextInt(6) + 3;
        // Dirección aleatoria: +1 => izq->der, -1 => der->izq
        direction = rand.nextBoolean() ? 1 : -1;
    }

    /**
     * Ajusta la escala en función de la altura del mapa (mapHeight)
     *
     * @param desiredRatio Fracción de la altura del mapa que ocupará el coche
     */
    public void configureScale(int mapHeight, float desiredRatio) {
        int hOriginal = originalCar.getHeight();
        float desiredHeight = mapHeight * desiredRatio;
        scaleFactor = desiredHeight / hOriginal;

        // Crear sprite escalado
        int newW = (int) (originalCar.getWidth() * scaleFactor);
        int newH = (int) (originalCar.getHeight() * scaleFactor);

        sprite = Bitmap.createScaledBitmap(originalCar, newW, newH, true);
        width = sprite.getWidth();
        height = sprite.getHeight();
    }

    /**
     * Carga la imagen cars.png y la recorta en 4 sub-Bitmaps (2 filas x 2 columnas).
     * Se asume que cada coche, por defecto, "mira" a la IZQUIERDA.
     */
    private static void loadCarBitmaps(Context context) {
        Bitmap fullSheet = BitmapFactory.decodeResource(context.getResources(), R.drawable.cars);
        int sheetWidth = fullSheet.getWidth();
        int sheetHeight = fullSheet.getHeight();

        int singleCarWidth = sheetWidth / 2;
        int singleCarHeight = sheetHeight / 2;

        carBitmaps = new Bitmap[4];
        // Coche 0: arriba-izquierda
        carBitmaps[0] = Bitmap.createBitmap(fullSheet, 0, 0, singleCarWidth, singleCarHeight);
        // Coche 1: arriba-derecha
        carBitmaps[1] = Bitmap.createBitmap(fullSheet, singleCarWidth, 0, singleCarWidth, singleCarHeight);
        // Coche 2: abajo-izquierda
        carBitmaps[2] = Bitmap.createBitmap(fullSheet, 0, singleCarHeight, singleCarWidth, singleCarHeight);
        // Coche 3: abajo-derecha
        carBitmaps[3] = Bitmap.createBitmap(fullSheet, singleCarWidth, singleCarHeight, singleCarWidth, singleCarHeight);

        fullSheet.recycle();
    }

    @Override
    public void update() {
        // Mover el coche
        x += speed * direction;

        // Ajusta si se sale de la pantalla (cambia 2000 por el ancho real)
        if (direction > 0 && x > 2000) {
            x = -width;
        } else if (direction < 0 && (x + width) < 0) {
            x = 2000;
        }
    }

    /**
     * Sobrescribimos draw para VOLTEAR el coche si va de izq->der (direction > 0).
     * Porque el sprite base está orientado a la izquierda por defecto.
     */
    @Override
    public void draw(Canvas canvas) {
        if (sprite == null || canvas == null) return;

        // Guardamos el estado previo del canvas
        canvas.save();

        if (direction > 0) {
            // Va de izq->der => flip horizontal (por defecto mira izq)
            float centerX = x + (width / 2f);
            float centerY = y + (height / 2f);
            canvas.scale(-1, 1, centerX, centerY);
        }

        // Dibujamos el bitmap
        canvas.drawBitmap(sprite, x, y, null);

        // Restauramos
        canvas.restore();
    }
}