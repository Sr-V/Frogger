package edu.pmdm.frogger.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Paint;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import edu.pmdm.frogger.R;
import edu.pmdm.frogger.utils.GameAudioManager;

public class Juego extends SurfaceView implements SurfaceHolder.Callback {

    private BucleJuego bucleJuego;
    private GameEngine gameEngine;
    private Bitmap background;
    private boolean positionsConfigured = false;
    private GameAudioManager gam = GameAudioManager.getInstance();

    public Juego(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public void setLevel(int level) {
        int mapResource;
        switch (level) {
            case 1:
                mapResource = R.drawable.map_level1;
                gam.levelOneTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());
                break;
            case 2:
                mapResource = R.drawable.map_level2;
                gam.levelTwoTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());
                break;
            case 3:
                mapResource = R.drawable.map_level3;
                gam.levelThreeTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());
                break;
            default:
                mapResource = R.drawable.map_level1;
                gam.levelOneTheme(getContext());
                gam.idleCroak(getContext());
                gam.carHonks(getContext());
        }
        background = BitmapFactory.decodeResource(getResources(), mapResource);
    }

    public void setGameEngine(GameEngine engine) {
        this.gameEngine = engine;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!positionsConfigured && gameEngine != null) {
            int canvasWidth = getWidth();
            int canvasHeight = getHeight();
            configurePositions(canvasWidth, canvasHeight);
            positionsConfigured = true;
        }
        bucleJuego = new BucleJuego(this);
        bucleJuego.setRunning(true);
        bucleJuego.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

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

    private void configurePositions(int width, int height) {
        if (gameEngine == null) return;
        int bottomOffset = 300;
        int mapHeight = height - bottomOffset;
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
        Rect dstRect = new Rect(0, 0, canvasWidth, canvasHeight - bottomOffset);
        if (background != null) {
            canvas.drawBitmap(background, null, dstRect, null);
        }
        if (gameEngine != null) {
            gameEngine.draw(canvas);
            // Dibujar la barra de tiempo en la parte superior
            float timeRatio = gameEngine.getTimeRatio();
            int barHeight = 20;
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.DKGRAY);
            Paint timeBarPaint = new Paint();
            timeBarPaint.setColor(Color.RED);
            canvas.drawRect(0, 0, canvasWidth, barHeight, bgPaint);
            canvas.drawRect(0, 0, canvasWidth * timeRatio, barHeight, timeBarPaint);
        }
    }

    public void movePlayerLeft() {
        if (gameEngine != null) {
            gameEngine.movePlayerLeft();
            gam.playerMovement(getContext());
        }
    }

    public void movePlayerUp() {
        if (gameEngine != null) {
            gameEngine.movePlayerUp();
            gam.playerMovement(getContext());
        }
    }

    public void movePlayerRight() {
        if (gameEngine != null) {
            gameEngine.movePlayerRight();
            gam.playerMovement(getContext());
        }
    }

    public void movePlayerDown() {
        if (gameEngine != null) {
            gameEngine.movePlayerDown();
            gam.playerMovement(getContext());
        }
    }
}