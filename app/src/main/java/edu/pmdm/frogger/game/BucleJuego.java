package edu.pmdm.frogger.game;

import android.graphics.Canvas;

/**
 * {@code BucleJuego} implementa el bucle principal del juego, el cual actualiza la lógica del juego
 * y dibuja los frames a una tasa de FPS constante.
 */
public class BucleJuego extends Thread {

    // Frames por segundo deseados
    private static final long FPS = 30;
    // Bandera para controlar la ejecución del bucle del juego
    private boolean isRunning;
    // Referencia a la instancia de Juego, que contiene la lógica y el renderizado
    private final Juego juego;

    /**
     * Constructor que recibe la instancia del juego.
     *
     * @param juego Instancia de {@link Juego} que se actualizará y dibujará.
     */
    public BucleJuego(Juego juego) {
        this.juego = juego;
    }

    /**
     * Permite activar o desactivar la ejecución del bucle del juego.
     *
     * @param running {@code true} para iniciar el bucle, {@code false} para detenerlo.
     */
    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    /**
     * Método principal del hilo. Ejecuta el bucle del juego:
     * 1) Actualiza la lógica del juego.
     * 2) Dibuja el frame actual en el canvas.
     * 3) Controla la tasa de frames (FPS) para lograr una animación suave.
     */
    @Override
    public void run() {
        long startTime;         // Tiempo de inicio de cada iteración
        long timeMillis;        // Tiempo transcurrido en cada iteración
        long waitTime;          // Tiempo que se debe dormir para mantener el FPS deseado
        // Duración de cada frame en milisegundos
        long frameDuration = 1000 / FPS;

        while (isRunning) {
            startTime = System.currentTimeMillis();

            // 1) Actualizar la lógica del juego
            juego.update();

            // 2) Dibujar el frame actual
            Canvas canvas = null;
            try {
                // Bloquear el canvas para dibujar
                canvas = juego.getHolder().lockCanvas();
                synchronized (juego.getHolder()) {
                    if (canvas != null) {
                        // Dibujar el contenido del juego en el canvas
                        juego.draw(canvas);
                    }
                }
            } finally {
                // Desbloquear el canvas y publicar el dibujo si no es nulo
                if (canvas != null) {
                    juego.getHolder().unlockCanvasAndPost(canvas);
                }
            }

            // 3) Calcular el tiempo transcurrido y determinar el tiempo de espera para mantener el FPS deseado
            timeMillis = System.currentTimeMillis() - startTime;
            waitTime = frameDuration - timeMillis;
            if (waitTime > 0) {
                try {
                    // Dormir el hilo durante el tiempo necesario para ajustar el FPS
                    sleep(waitTime);
                } catch (InterruptedException e) {
                    // Imprimir la traza de la excepción en caso de interrupción
                    e.printStackTrace();
                }
            }
        }
    }
}