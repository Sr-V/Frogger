package edu.pmdm.frogger.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

/**
 * {@code FroggerObject} es una clase abstracta que representa un objeto en el juego Frogger.
 * Define atributos y métodos comunes para los objetos del juego, como posición, dimensiones,
 * sprite y detección de colisiones.
 */
public abstract class FroggerObject {

    // Posición (x, y) del objeto
    protected int x, y;
    // Dimensiones del objeto
    protected int width, height;
    // Imagen que representa al objeto
    protected Bitmap sprite;
    // Contexto de la aplicación, utilizado para acceder a recursos
    protected Context context;

    /**
     * Constructor que recibe el contexto de la aplicación.
     *
     * @param context Contexto de la aplicación.
     */
    public FroggerObject(Context context) {
        this.context = context;
    }

    /**
     * Establece la posición del objeto en el juego.
     *
     * @param x Coordenada x.
     * @param y Coordenada y.
     */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Actualiza la lógica del objeto.
     * Debe ser implementado por las clases que extiendan FroggerObject.
     */
    public abstract void update();

    /**
     * Dibuja el objeto en el canvas proporcionado.
     *
     * @param canvas Canvas sobre el que se dibuja el objeto.
     */
    public void draw(Canvas canvas) {
        // Si existe un sprite y el canvas no es nulo, se dibuja el sprite en la posición (x, y)
        if (sprite != null && canvas != null) {
            canvas.drawBitmap(sprite, x, y, null);
        }
    }

    /**
     * Devuelve la "bounding box" del objeto con un padding para colisiones más precisas.
     *
     * @return Un objeto {@code RectF} que representa la caja de colisión con padding.
     */
    public RectF getBoundingBoxPrecise() {
        // Calcular el padding en función del ancho y alto del objeto (15% de cada dimensión)
        float paddingX = width * 0.15f;
        float paddingY = height * 0.15f;
        // Retornar el rectángulo que representa la caja de colisión ajustada
        return new RectF(x + paddingX, y + paddingY, x + width - paddingX, y + height - paddingY);
    }

    /**
     * Devuelve la "bounding box" del objeto sin padding.
     * Este método debe ser implementado por las subclases para definir su propia caja de colisión.
     *
     * @return Un objeto {@code RectF} que representa la caja de colisión.
     */
    public abstract RectF getBoundingBox();
}