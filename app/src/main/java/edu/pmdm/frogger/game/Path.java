package edu.pmdm.frogger.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;
import edu.pmdm.frogger.R;
import edu.pmdm.frogger.utils.GameAudioManager;

/**
 * {@code Path} representa el camino seguro en el juego Frogger.
 * Dibuja piezas que conforman el patrón básico del camino, y si el nivel lo requiere,
 * también gestiona la llave y piezas adicionales que se muestran al recogerla.
 */
public class Path {

    // Piezas que forman el camino básico (siempre visibles)
    private List<PathPiece> basicPieces;
    // Piezas adicionales que se muestran tras recoger la llave (opcional)
    private List<PathPiece> additionalPieces;
    // La llave que se muestra sobre una pieza del camino
    private Key key;
    // Indica si la llave ya ha sido recogida
    private boolean keyCollected = false;

    // Líneas de posición vertical del camino (normalizadas, entre 0 y 1)
    private float[] pathLines;
    // Número de columnas del camino (fijas en 5)
    private int columns = 5;
    // Ancho y alto de cada celda del camino
    private int cellWidth, cellHeight;
    // Configuración del camino, que define patrones, posición de la llave, etc.
    private PathConfig config;
    // Gestor de audio para reproducir efectos relacionados con la llave
    private GameAudioManager gam;
    // Contexto de la aplicación para acceder a recursos
    private Context context;

    /**
     * Constructor de {@code Path}.
     *
     * @param context     Contexto de la aplicación.
     * @param screenWidth Ancho total de la pantalla.
     * @param mapHeight   Altura del área de juego.
     * @param config      Configuración del camino para el nivel.
     */
    public Path(Context context, int screenWidth, int mapHeight, PathConfig config) {
        this.context = context;
        this.config = config;
        // Generar las líneas verticales del camino, en este caso 5 líneas entre 0.39 y 0.08
        this.pathLines = generateLines(0.39f, 0.08f, 5);
        cellWidth = screenWidth / columns;
        cellHeight = (int)(mapHeight * 0.10f);
        int pieceWidth = (int)(cellWidth * 0.75f);
        int pieceHeight = (int)(cellHeight * 0.75f);

        gam = GameAudioManager.getInstance(context);

        basicPieces = new ArrayList<>();
        if (!config.hasKey) {
            // Sin llave: se crea el camino básico a partir del patrón básico
            for (int row = 0; row < config.basicPattern.length; row++) {
                int yPos = (int)(pathLines[row] * mapHeight);
                for (int col = 0; col < columns; col++) {
                    if (config.basicPattern[row][col] == 1) {
                        int xPos = col * cellWidth + (cellWidth - pieceWidth) / 2;
                        basicPieces.add(new PathPiece(context, xPos, yPos, config.drawableId, pieceWidth, pieceHeight));
                    }
                }
            }
        } else {
            // Con llave: crear todas las piezas básicas, incluyendo la celda que contendrá la llave
            for (int row = 0; row < config.basicPattern.length; row++) {
                int yPos = (int)(pathLines[row] * mapHeight);
                for (int col = 0; col < columns; col++) {
                    if (config.basicPattern[row][col] == 1) {
                        int xPos = col * cellWidth + (cellWidth - pieceWidth) / 2;
                        basicPieces.add(new PathPiece(context, xPos, yPos, config.drawableId, pieceWidth, pieceHeight));
                    }
                }
            }
            // Crear la llave en la posición indicada (basada en keyRow y keyCol) y ajustada a un 80% del tamaño de la pieza
            int keyBaseX = config.keyCol * cellWidth + (cellWidth - pieceWidth) / 2;
            int keyBaseY = (int)(pathLines[config.keyRow] * mapHeight);
            int keyWidth = (int)(pieceWidth * 0.8f);
            int keyHeight = (int)(pieceHeight * 0.8f);
            int keyX = keyBaseX + (pieceWidth - keyWidth) / 2;
            int keyY = keyBaseY + (pieceHeight - keyHeight) / 2;
            key = new Key(context, keyX, keyY, keyWidth, keyHeight, config.keyDrawableId);

            // Crear las piezas adicionales a partir del patrón adicional, que se dibujan a partir de additionalStartRow
            additionalPieces = new ArrayList<>();
            for (int row = 0; row < config.additionalPattern.length; row++) {
                int actualRow = config.additionalStartRow + row;
                int yPos = (int)(pathLines[actualRow] * mapHeight);
                for (int col = 0; col < columns; col++) {
                    if (config.additionalPattern[row][col] == 1) {
                        int xPos = col * cellWidth + (cellWidth - pieceWidth) / 2;
                        additionalPieces.add(new PathPiece(context, xPos, yPos, config.drawableId, pieceWidth, pieceHeight));
                    }
                }
            }
        }
    }

    /**
     * Genera un arreglo de líneas distribuidas uniformemente entre dos valores.
     *
     * @param start Valor inicial.
     * @param end   Valor final.
     * @param count Número de líneas a generar.
     * @return Arreglo de valores flotantes que representan las posiciones verticales.
     */
    private float[] generateLines(float start, float end, int count) {
        float[] lines = new float[count];
        float step = (start - end) / (count - 1);
        for (int i = 0; i < count; i++) {
            lines[i] = start - i * step;
        }
        return lines;
    }

    /**
     * Dibuja el camino en el canvas.
     * Se dibujan primero las piezas básicas, luego la llave (si no se ha recogido) y,
     * si la llave ha sido recogida, se dibujan las piezas adicionales.
     *
     * @param canvas Canvas sobre el que se dibuja el camino.
     */
    public void draw(Canvas canvas) {
        // Dibujar todas las piezas básicas
        for (PathPiece piece : basicPieces) {
            piece.draw(canvas);
        }
        if (config.hasKey) {
            // Si hay llave, dibujar la llave si aún no se ha recogido
            if (!keyCollected && key != null) {
                key.draw(canvas);
            }
            // Si la llave ha sido recogida, dibujar las piezas adicionales
            if (keyCollected && additionalPieces != null) {
                for (PathPiece piece : additionalPieces) {
                    piece.draw(canvas);
                }
            }
        }
    }

    /**
     * Verifica si un objeto (por ejemplo, la rana) se encuentra en una zona segura del camino.
     * Se comprueba si el centro inferior de la rana está contenido en alguna de las piezas básicas,
     * o en las piezas adicionales en caso de haber recogido la llave.
     *
     * @param obj Objeto del juego cuya seguridad se verifica.
     * @return {@code true} si el objeto está sobre alguna pieza del camino, {@code false} en caso contrario.
     */
    public boolean isFrogSafe(FroggerObject obj) {
        RectF frogBox = obj.getBoundingBox();
        float centerX = frogBox.left + frogBox.width() / 2;
        float bottomY = frogBox.bottom;
        // Revisar cada pieza básica
        for (PathPiece piece : basicPieces) {
            RectF pieceBox = piece.getBoundingBox();
            if (pieceBox.contains(centerX, bottomY)) {
                return true;
            }
        }
        // Si hay llave y ha sido recogida, revisar las piezas adicionales
        if (config.hasKey && keyCollected && additionalPieces != null) {
            for (PathPiece piece : additionalPieces) {
                RectF pieceBox = piece.getBoundingBox();
                if (pieceBox.contains(centerX, bottomY)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verifica si la rana ha recogido la llave.
     * Se debe llamar en cada ciclo de actualización para detectar la colisión entre la rana y la llave.
     *
     * @param obj Objeto del juego (por ejemplo, la rana).
     */
    public void checkKeyCollected(FroggerObject obj) {
        if (config.hasKey && !keyCollected && key != null) {
            RectF frogBox = obj.getBoundingBox();
            RectF keyBox = key.getBoundingBox();
            if (RectF.intersects(frogBox, keyBox)) {
                keyCollected = true;
                gam.keyCollected(context);
                key = null;
            }
        }
    }

    /**
     * {@code PathPiece} representa una pieza estática del camino.
     * Se utiliza tanto para las piezas básicas como para las adicionales.
     */
    public static class PathPiece extends FroggerObject {

        private Bitmap sprite;

        /**
         * Crea una nueva pieza del camino.
         *
         * @param context    Contexto de la aplicación.
         * @param x          Posición X de la pieza.
         * @param y          Posición Y de la pieza.
         * @param drawableId Identificador del recurso drawable para la pieza.
         * @param pieceWidth Ancho deseado para la pieza.
         * @param pieceHeight Alto deseado para la pieza.
         */
        public PathPiece(Context context, int x, int y, int drawableId, int pieceWidth, int pieceHeight) {
            super(context);
            this.x = x;
            this.y = y;
            sprite = BitmapFactory.decodeResource(context.getResources(), drawableId);
            sprite = Bitmap.createScaledBitmap(sprite, pieceWidth, pieceHeight, true);
            this.width = sprite.getWidth();
            this.height = sprite.getHeight();
        }

        /**
         * Dibuja la pieza del camino en el canvas.
         *
         * @param canvas Canvas sobre el que se dibuja la pieza.
         */
        @Override
        public void draw(Canvas canvas) {
            if (sprite != null && canvas != null) {
                canvas.drawBitmap(sprite, x, y, null);
            }
        }

        /**
         * Retorna la caja de colisión de la pieza.
         *
         * @return {@code RectF} que representa la bounding box de la pieza.
         */
        @Override
        public RectF getBoundingBox() {
            return new RectF(x, y, x + width, y + height);
        }

        /**
         * Las piezas del camino son estáticas, por lo que no se actualizan.
         */
        @Override
        public void update() {
            // No se actualiza nada, ya que las piezas son estáticas.
        }
    }

    /**
     * {@code Key} representa la llave que se debe recoger en ciertos niveles.
     * La llave se dibuja sobre la pieza del camino y es utilizada para desbloquear
     * piezas adicionales del camino.
     */
    public static class Key extends FroggerObject {

        private Bitmap sprite;

        /**
         * Crea una nueva llave.
         *
         * @param context    Contexto de la aplicación.
         * @param x          Posición X de la llave.
         * @param y          Posición Y de la llave.
         * @param keyWidth   Ancho deseado para la llave.
         * @param keyHeight  Alto deseado para la llave.
         * @param drawableId Identificador del recurso drawable para la llave.
         */
        public Key(Context context, int x, int y, int keyWidth, int keyHeight, int drawableId) {
            super(context);
            this.x = x;
            this.y = y;
            sprite = BitmapFactory.decodeResource(context.getResources(), drawableId);
            sprite = Bitmap.createScaledBitmap(sprite, keyWidth, keyHeight, true);
            this.width = sprite.getWidth();
            this.height = sprite.getHeight();
        }

        /**
         * Dibuja la llave en el canvas.
         *
         * @param canvas Canvas sobre el que se dibuja la llave.
         */
        @Override
        public void draw(Canvas canvas) {
            if (sprite != null && canvas != null) {
                canvas.drawBitmap(sprite, x, y, null);
            }
        }

        /**
         * Retorna la caja de colisión de la llave.
         *
         * @return {@code RectF} que representa la bounding box de la llave.
         */
        @Override
        public RectF getBoundingBox() {
            return new RectF(x, y, x + width, y + height);
        }

        /**
         * La llave es estática y no se actualiza.
         */
        @Override
        public void update() {
            // No se actualiza la llave, ya que es estática.
        }
    }

    /**
     * {@code PathConfig} encapsula la configuración del camino para un nivel,
     * incluyendo los patrones de piezas básicas y adicionales, la posición de la llave,
     * y los recursos drawable a utilizar.
     */
    public static class PathConfig {
        public int[][] basicPattern;         // Patrón de piezas básicas (filas siempre visibles)
        public boolean hasKey;               // Indica si el nivel incluye llave
        public int keyRow;                   // Fila en la que se coloca la llave (0 a 4)
        public int keyCol;                   // Columna en la que se coloca la llave (0 a 4)
        public int[][] additionalPattern;    // Patrón para las piezas adicionales (se muestran al recoger la llave)
        public int additionalStartRow;       // Fila a partir de la cual se dibujan las piezas adicionales
        public int drawableId;               // Recurso drawable para las piezas del camino
        public int keyDrawableId;            // Recurso drawable para la llave

        /**
         * Constructor para {@code PathConfig}.
         *
         * @param basicPattern        Patrón de piezas básicas.
         * @param hasKey              Indica si el nivel tiene llave.
         * @param keyRow              Fila de la llave.
         * @param keyCol              Columna de la llave.
         * @param additionalPattern   Patrón de piezas adicionales.
         * @param additionalStartRow  Fila a partir de la cual se muestran las piezas adicionales.
         * @param drawableId          Drawable para las piezas del camino.
         * @param keyDrawableId       Drawable para la llave.
         */
        public PathConfig(int[][] basicPattern, boolean hasKey, int keyRow, int keyCol,
                          int[][] additionalPattern, int additionalStartRow,
                          int drawableId, int keyDrawableId) {
            this.basicPattern = basicPattern;
            this.hasKey = hasKey;
            this.keyRow = keyRow;
            this.keyCol = keyCol;
            this.additionalPattern = additionalPattern;
            this.additionalStartRow = additionalStartRow;
            this.drawableId = drawableId;
            this.keyDrawableId = keyDrawableId;
        }
    }

    /**
     * Retorna una configuración de camino basada en el nivel.
     *
     * @param level Nivel actual.
     * @return Instancia de {@code PathConfig} con la configuración correspondiente.
     */
    public static PathConfig getPathConfigForLevel(int level) {
        // Ejemplos de configuraciones para distintos niveles.
        switch (level) {
            case 1:
                // Nivel sin llave.
                int[][] basic1 = {
                        {1, 1, 1, 1, 1},
                        {1, 1, 0, 1, 1},
                        {0, 1, 0, 1, 0},
                        {0, 1, 1, 1, 0},
                        {0, 0, 1, 0, 0}
                };
                return new PathConfig(basic1, false, 0, 0, null, 0, R.drawable.lilypad, 0);
            case 2:
                // Nivel con llave y patrón diferente.
                int[][] basic2 = {
                        {1, 1, 1, 1, 1},
                        {1, 0, 0, 0, 1},
                };
                int[][] additional2 = {
                        {0, 0, 0, 0, 1},
                        {1, 1, 1, 1, 1},
                        {1, 0, 0, 0, 0}
                };
                return new PathConfig(basic2, true, 1, 0, additional2, 2, R.drawable.desert_path, R.drawable.desert_key);
            case 3:
                // Nivel con llave.
                int[][] basic3 = {
                        {1, 1, 1, 1, 1},
                        {1, 0, 1, 0, 1}
                };
                int[][] additional3 = {
                        {1, 1, 0, 1, 1},
                        {0, 1, 1, 1, 0},
                        {0, 0, 1, 0, 0}
                };
                // La llave se coloca en la fila 2, columna 2; piezas adicionales a partir de la fila 3.
                return new PathConfig(basic3, true, 1, 2, additional3, 2, R.drawable.space_path, R.drawable.space_key);
            default:
                // Configuración por defecto sin llave.
                int[][] basicDefault = {
                        {1, 0, 0, 0, 0},
                        {0, 1, 0, 0, 0},
                        {0, 0, 1, 0, 0},
                        {0, 0, 0, 1, 0},
                        {0, 0, 0, 0, 1}
                };
                return new PathConfig(basicDefault, false, 0, 0, null, 0, R.drawable.lilypad, 0);
        }
    }
}