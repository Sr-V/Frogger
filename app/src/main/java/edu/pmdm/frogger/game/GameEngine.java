package edu.pmdm.frogger.game;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameEngine {

    private PlayerFrog player;
    private List<ObstacleCar> cars;
    private CollisionManager collisionManager;

    // Carriles de coches (verticales)
    private final float[] roadLines = {
            0.82f, 0.74f, 0.66f, 0.58f, 0.50f
    };

    // Líneas verticales de la rana
    private final float[] frogLines = {
            0.92f, 0.87f, 0.79f, 0.71f, 0.63f, 0.55f
    };
    private int frogLineIndex = 0; // índice vertical

    // ===== 5 COLUMNAS HORIZONTALES (en píxeles) =====
    // columnsX se rellena en configurePositions(...)
    private int[] columnsX = new int[5];
    private int frogColumnIndex = 2; // empezamos en la columna central

    private int screenWidth;
    private int mapHeight;

    public GameEngine(Context context, int level) {
        collisionManager = new CollisionManager();
        player = new PlayerFrog(context);
        cars = new ArrayList<>();
    }

    public void configurePositions(int screenWidth, int mapHeight) {
        this.screenWidth = screenWidth;
        this.mapHeight   = mapHeight;

        // 1) Escalamos la rana
        player.configureScale(mapHeight, 0.06f);

        // 2) Creamos 5 columnas iguales
        //    ancho de cada columna = screenWidth / 5
        int columnWidth = screenWidth / 5;
        for (int i = 0; i < 5; i++) {
            // Queremos el centro de cada columna => i * columnWidth + (columnWidth/2)
            columnsX[i] = i * columnWidth + (columnWidth / 2);
        }

        // 3) Posición vertical: frogLineIndex = 0 => primer "row"
        frogLineIndex = 0;
        float frogY = frogLines[frogLineIndex] * mapHeight;

        // 4) Posición horizontal: frogColumnIndex = 2 => columna central
        frogColumnIndex = 2;
        float frogScaledWidth = player.getScaledWidth();
        // columnsX[2] es el centro de la columna => restamos la mitad del sprite
        float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);

        // 5) Guardar la posición inicial
        player.storeInitialPosition((int) frogX, (int) frogY);

        // 6) Generar coches
        cars.clear();
        for (float line : roadLines) {
            float carY = line * mapHeight;
            float carX = new Random().nextFloat() * (screenWidth - 100);

            ObstacleCar car = new ObstacleCar(player.context, (int) carX, (int) carY);
            car.configureScale(mapHeight, 0.10f);
            cars.add(car);
        }
    }

    public void update() {
        player.update();
        for (ObstacleCar car : cars) {
            car.update();
        }
        for (ObstacleCar car : cars) {
            if (collisionManager.checkCollision(player, car)) {
                // Al colisionar => anim de muerte
                player.playDeathAnimation();
                // Reiniciamos índice vertical y horizontal
                frogLineIndex = 0;
                frogColumnIndex = 2;
            }
        }
    }

    public void draw(android.graphics.Canvas canvas) {
        player.draw(canvas);
        for (ObstacleCar car : cars) {
            car.draw(canvas);
        }
    }

    // ===== MOVIMIENTO VERTICAL =====
    public void movePlayerUp() {
        if (frogLineIndex < frogLines.length - 1) {
            frogLineIndex++;
            // Mantenemos la columna
            float frogScaledWidth = player.getScaledWidth();

            // X actual => columnsX[frogColumnIndex]
            float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);

            // Y => siguiente línea
            float frogY = frogLines[frogLineIndex] * mapHeight;

            player.setPosition((int) frogX, (int) frogY);
            player.moveUpSmall();
        }
    }

    // ===== MOVIMIENTOS HORIZONTALES =====
    public void movePlayerLeft() {
        if (frogColumnIndex > 0) {
            frogColumnIndex--;
            // Mantenemos la línea vertical actual => player.y
            float frogY = player.y;
            float frogScaledWidth = player.getScaledWidth();
            float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);

            player.setPosition((int) frogX, (int) frogY);
            player.moveLeft(); // anim horizontal
        }
    }

    public void movePlayerRight() {
        if (frogColumnIndex < columnsX.length - 1) {
            frogColumnIndex++;
            // Mantenemos la línea vertical => player.y
            float frogY = player.y;
            float frogScaledWidth = player.getScaledWidth();
            float frogX = columnsX[frogColumnIndex] - (frogScaledWidth / 2f);

            player.setPosition((int) frogX, (int) frogY);
            player.moveRight();
        }
    }
}