package edu.pmdm.frogger.game;

import android.graphics.RectF;

public class CollisionManager {

    public boolean checkCollision(FroggerObject objA, FroggerObject objB) {
        RectF rectA = objA.getBoundingBoxPrecise();
        RectF rectB = objB.getBoundingBoxPrecise();
        return rectA.intersect(rectB);
    }
}