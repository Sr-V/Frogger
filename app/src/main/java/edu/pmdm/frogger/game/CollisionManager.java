package edu.pmdm.frogger.game;

import android.graphics.Rect;

public class CollisionManager {

    public boolean checkCollision(FroggerObject objA, FroggerObject objB) {
        Rect rectA = objA.getBoundingBox();
        Rect rectB = objB.getBoundingBox();
        return Rect.intersects(rectA, rectB);
    }
}
