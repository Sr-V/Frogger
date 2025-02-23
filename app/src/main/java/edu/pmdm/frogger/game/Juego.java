package edu.pmdm.frogger.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import edu.pmdm.frogger.R;

public class Juego extends SurfaceView implements SurfaceHolder.Callback {

    private BucleJuego bucleJuego;      // Hilo de juego
    private GameEngine gameEngine;      // Lógica del juego
    private Bitmap background;          // Fondo
    private boolean positionsConfigured = false; // Para evitar re-configurar varias veces

    // Constructor si usas XML y no pasas level
    public Juego(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, 1); // nivel por defecto
    }

    private void init(Context context, int level) {
        getHolder().addCallback(this);

        // Cargar fondo
        switch (level) {
            case 1:
                background = BitmapFactory.decodeResource(getResources(), R.drawable.map_level1);
                break;
            default:
                background = BitmapFactory.decodeResource(getResources(), R.drawable.map_level1);
                break;
        }

        // Crear GameEngine
        gameEngine = new GameEngine(context, level);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Configurar posiciones y escalas en función del tamaño real del SurfaceView
        if (!positionsConfigured) {
            int canvasWidth = getWidth();
            int canvasHeight = getHeight();
            configurePositions(canvasWidth, canvasHeight);
            positionsConfigured = true;
        }

        // Iniciar el hilo de juego
        bucleJuego = new BucleJuego(this);
        bucleJuego.setRunning(true);
        bucleJuego.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Podrías re-configurar si cambian dimensiones (rotación, etc.)
        // configurePositions(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        bucleJuego.setRunning(false);
        while (retry) {
            try {
                bucleJuego.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Ajusta el fondo y llama a GameEngine para configurar posiciones de rana y coches.
     */
    private void configurePositions(int width, int height) {
        // Reserva un offset inferior para botones
        int bottomOffset = 300; // píxeles para botones (ajusta si tu layout es mayor/menor)

        // Altura total para el mapa
        int mapHeight = height - bottomOffset;

        // Pasa estas dimensiones al GameEngine
        gameEngine.configurePositions(width, mapHeight);
    }

    public void update() {
        if (gameEngine != null) {
            gameEngine.update();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        int bottomOffset = 300;

        // Escalar el fondo para ocupar [0..mapHeight]
        Rect dstRect = new Rect(0, 0, canvasWidth, canvasHeight - bottomOffset);
        if (background != null) {
            canvas.drawBitmap(background, null, dstRect, null);
        }

        // Dibujar el resto de elementos
        if (gameEngine != null) {
            gameEngine.draw(canvas);
        }
    }

    // Métodos para mover al player
    public void movePlayerLeft()  { if (gameEngine != null) gameEngine.movePlayerLeft(); }
    public void movePlayerUp()    { if (gameEngine != null) gameEngine.movePlayerUp(); }
    public void movePlayerRight() { if (gameEngine != null) gameEngine.movePlayerRight(); }
}