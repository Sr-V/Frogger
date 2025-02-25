package edu.pmdm.frogger.game;

import android.graphics.Canvas;

public class BucleJuego extends Thread {

    private static final long FPS = 30; // Frames por segundo deseados
    private boolean isRunning;
    private final Juego juego;

    public BucleJuego(Juego juego) {
        this.juego = juego;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    @Override
    public void run() {
        long startTime;
        long timeMillis;
        long waitTime;
        long frameDuration = 1000 / FPS; // Duración de cada frame en ms

        while (isRunning) {
            startTime = System.currentTimeMillis();

            // 1) Actualizar lógica
            juego.update();

            // 2) Dibujar
            Canvas canvas = null;
            try {
                canvas = juego.getHolder().lockCanvas();
                synchronized (juego.getHolder()) {
                    if (canvas != null) {
                        juego.draw(canvas);
                    }
                }
            } finally {
                if (canvas != null) {
                    juego.getHolder().unlockCanvasAndPost(canvas);
                }
            }

            // Calcular tiempo para mantener FPS
            timeMillis = System.currentTimeMillis() - startTime;
            waitTime = frameDuration - timeMillis;
            if (waitTime > 0) {
                try {
                    sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
