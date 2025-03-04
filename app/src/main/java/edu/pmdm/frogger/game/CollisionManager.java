package edu.pmdm.frogger.game;

import android.graphics.RectF;

/**
 * {@code CollisionManager} gestiona la detección de colisiones entre objetos del juego.
 * Proporciona métodos para comprobar si dos objetos, definidos por sus cajas de colisión,
 * se intersectan.
 */
public class CollisionManager {

    /**
     * Comprueba si existe una colisión entre dos objetos del juego.
     * <p>
     * Se obtienen las cajas de colisión precisas de ambos objetos y se verifica si se intersectan.
     * </p>
     *
     * @param objA Primer objeto de tipo {@code FroggerObject}.
     * @param objB Segundo objeto de tipo {@code FroggerObject}.
     * @return {@code true} si las cajas de colisión se intersectan (colisión detectada), {@code false} en caso contrario.
     */
    public boolean checkCollision(FroggerObject objA, FroggerObject objB) {
        // Obtener la caja de colisión precisa del primer objeto
        RectF rectA = objA.getBoundingBoxPrecise();
        // Obtener la caja de colisión precisa del segundo objeto
        RectF rectB = objB.getBoundingBoxPrecise();
        // Retornar true si ambas cajas se intersectan, false de lo contrario
        return rectA.intersect(rectB);
    }
}