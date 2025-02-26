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

public class Path {

    // Piezas siempre visibles (del patrón básico)
    private List<PathPiece> basicPieces;
    // Piezas adicionales (se muestran al recoger la llave)
    private List<PathPiece> additionalPieces;
    // La llave que se dibuja sobre la pieza del camino
    private Key key;
    private boolean keyCollected = false;

    private float[] pathLines;
    private int columns = 5;
    private int cellWidth, cellHeight;
    private PathConfig config;
    private GameAudioManager gam = GameAudioManager.getInstance();
    private Context context;

    /**
     * Constructor.
     * @param context Contexto de la aplicación.
     * @param screenWidth Ancho total.
     * @param mapHeight Altura del área de juego.
     * @param config Configuración del camino para el nivel.
     */
    public Path(Context context, int screenWidth, int mapHeight, PathConfig config) {
        this.context = context;
        this.config = config;
        this.pathLines = generateLines(0.39f, 0.08f, 5);
        cellWidth = screenWidth / columns;
        cellHeight = (int)(mapHeight * 0.10f);
        int pieceWidth = (int)(cellWidth * 0.75f);
        int pieceHeight = (int)(cellHeight * 0.75f);

        basicPieces = new ArrayList<>();
        if (!config.hasKey) {
            // Sin llave: se crea el camino básico a partir de basicPattern.
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
            // Con llave: se crean las piezas básicas COMPLETAS, incluyendo la celda donde irá la llave.
            for (int row = 0; row < config.basicPattern.length; row++) {
                int yPos = (int)(pathLines[row] * mapHeight);
                for (int col = 0; col < columns; col++) {
                    if (config.basicPattern[row][col] == 1) {
                        int xPos = col * cellWidth + (cellWidth - pieceWidth) / 2;
                        basicPieces.add(new PathPiece(context, xPos, yPos, config.drawableId, pieceWidth, pieceHeight));
                    }
                }
            }
            // Crear la llave en la posición indicada, dibujándola sobre la pieza.
            // Se reduce su tamaño (por ejemplo, al 80% del tamaño de la pieza) y se centra sobre la pieza.
            int keyBaseX = config.keyCol * cellWidth + (cellWidth - pieceWidth) / 2;
            int keyBaseY = (int)(pathLines[config.keyRow] * mapHeight);
            int keyWidth = (int)(pieceWidth * 0.8f);
            int keyHeight = (int)(pieceHeight * 0.8f);
            int keyX = keyBaseX + (pieceWidth - keyWidth) / 2;
            int keyY = keyBaseY + (pieceHeight - keyHeight) / 2;
            key = new Key(context, keyX, keyY, keyWidth, keyHeight, config.keyDrawableId);

            // Crear las piezas adicionales a partir del patrón adicional.
            additionalPieces = new ArrayList<>();
            // Se dibujan a partir de la fila additionalStartRow.
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

    private float[] generateLines(float start, float end, int count) {
        float[] lines = new float[count];
        float step = (start - end) / (count - 1);
        for (int i = 0; i < count; i++) {
            lines[i] = start - i * step;
        }
        return lines;
    }

    public void draw(Canvas canvas) {
        // Dibujar el camino básico.
        for (PathPiece piece : basicPieces) {
            piece.draw(canvas);
        }
        if (config.hasKey) {
            // Dibujar la llave (encima de la pieza) si aún no se ha recogido.
            if (!keyCollected && key != null) {
                key.draw(canvas);
            }
            // Dibujar el camino adicional si se ha recogido la llave.
            if (keyCollected && additionalPieces != null) {
                for (PathPiece piece : additionalPieces) {
                    piece.draw(canvas);
                }
            }
        }
    }

    /**
     * Verifica si el objeto (por ejemplo, la rana) se encuentra en zona segura.
     */
    public boolean isFrogSafe(FroggerObject obj) {
        RectF frogBox = obj.getBoundingBox();
        float centerX = frogBox.left + frogBox.width() / 2;
        float bottomY = frogBox.bottom;
        // Revisar las piezas básicas.
        for (PathPiece piece : basicPieces) {
            RectF pieceBox = piece.getBoundingBox();
            if (pieceBox.contains(centerX, bottomY)) {
                return true;
            }
        }
        // Si hay llave y ya se recogió, se consideran las piezas adicionales.
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
     * Debe llamarse en cada ciclo de actualización.
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

    public static class PathPiece extends FroggerObject {

        private Bitmap sprite;

        public PathPiece(Context context, int x, int y, int drawableId, int pieceWidth, int pieceHeight) {
            super(context);
            this.x = x;
            this.y = y;
            sprite = BitmapFactory.decodeResource(context.getResources(), drawableId);
            sprite = Bitmap.createScaledBitmap(sprite, pieceWidth, pieceHeight, true);
            this.width = sprite.getWidth();
            this.height = sprite.getHeight();
        }

        @Override
        public void draw(Canvas canvas) {
            if (sprite != null && canvas != null) {
                canvas.drawBitmap(sprite, x, y, null);
            }
        }

        @Override
        public RectF getBoundingBox() {
            return new RectF(x, y, x + width, y + height);
        }

        @Override
        public void update() {
            // Las piezas son estáticas.
        }
    }

    public static class Key extends FroggerObject {

        private Bitmap sprite;

        public Key(Context context, int x, int y, int keyWidth, int keyHeight, int drawableId) {
            super(context);
            this.x = x;
            this.y = y;
            sprite = BitmapFactory.decodeResource(context.getResources(), drawableId);
            sprite = Bitmap.createScaledBitmap(sprite, keyWidth, keyHeight, true);
            this.width = sprite.getWidth();
            this.height = sprite.getHeight();
        }

        @Override
        public void draw(Canvas canvas) {
            if (sprite != null && canvas != null) {
                canvas.drawBitmap(sprite, x, y, null);
            }
        }

        @Override
        public RectF getBoundingBox() {
            return new RectF(x, y, x + width, y + height);
        }

        @Override
        public void update() {
            // La llave es estática.
        }
    }

    /**
     * Clase de configuración para el camino.
     */
    public static class PathConfig {
        public int[][] basicPattern;         // Patrón de piezas básicas (filas siempre visibles)
        public boolean hasKey;               // ¿Este nivel incluye llave?
        public int keyRow;                   // Fila en la que se coloca la llave (0 a 4)
        public int keyCol;                   // Columna en la que se coloca la llave (0 a 4)
        public int[][] additionalPattern;    // Patrón para las piezas adicionales (se dibujan al recoger la llave)
        public int additionalStartRow;       // Fila a partir de la cual se muestran las piezas adicionales
        public int drawableId;               // Drawable para las piezas del camino
        public int keyDrawableId;            // Drawable para la llave

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
     * Método de ayuda para obtener una configuración por nivel.
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
                // Otro nivel sin llave, con patrón distinto.
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
                // Patrón básico para las primeras 3 filas; en la celda (fila 2, columna 2) se dibujará la llave encima de la pieza.
                int[][] basic3 = {
                        {1, 1, 1, 1, 1},
                        {1, 0, 1, 0, 1}
                };
                int[][] additional3 = {
                        {1, 1, 0, 1, 1},
                        {0, 1, 1, 1, 0},
                        {0, 0, 1, 0, 0}
                };
                // La llave se coloca en la fila 2, columna 2 y las piezas adicionales se dibujan a partir de la fila 3.
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
