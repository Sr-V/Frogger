package edu.pmdm.frogger.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import java.util.Random;
import edu.pmdm.frogger.R;

/**
 * {@code Obstacle} representa un obstáculo móvil en el juego Frogger,
 * como un coche u otro objeto que se mueve a través de la pantalla.
 * Hereda de {@link FroggerObject} e implementa la lógica de actualización,
 * escalado y renderizado del sprite, así como la reconfiguración aleatoria
 * cuando el obstáculo sale de la pantalla.
 */
public class Obstacle extends FroggerObject {

    // Caché de bitmaps obtenidos a partir de un sprite sheet para optimizar el rendimiento
    private static Bitmap[] cachedBitmaps = null;
    // Último identificador de recurso drawable cargado
    private static int lastDrawableId = -1;

    // Sprite original seleccionado del caché
    private Bitmap originalSprite;
    // Factor de escala utilizado para redimensionar el sprite en función del mapa
    private float scaleFactor = 1.0f;
    // Velocidad de desplazamiento del obstáculo
    private int speed;
    // Dirección del movimiento: +1 para moverse a la derecha, -1 para moverse a la izquierda
    private int direction;
    // Ancho de la pantalla, usado para determinar cuándo reubicar el obstáculo
    private int screenWidth;

    /**
     * Crea un nuevo obstáculo.
     *
     * @param context   Contexto de la aplicación.
     * @param startX    Posición X inicial.
     * @param startY    Posición Y inicial.
     * @param drawableId Identificador del recurso drawable que contiene el sprite sheet.
     */
    public Obstacle(Context context, int startX, int startY, int drawableId) {
        super(context);
        Random rand = new Random();

        // Cargar y cachear los bitmaps si aún no se han cargado o si se solicita un drawable diferente
        if (cachedBitmaps == null || lastDrawableId != drawableId) {
            loadBitmaps(context, drawableId);
            lastDrawableId = drawableId;
        }

        // Seleccionar un sprite aleatorio del caché
        int randomIndex = rand.nextInt(cachedBitmaps.length);
        originalSprite = cachedBitmaps[randomIndex];
        sprite = originalSprite;
        width = sprite.getWidth();
        height = sprite.getHeight();

        // Asignar posición inicial
        x = startX;
        y = startY;

        // Configurar velocidad aleatoria entre 3 y 8 (inclusive)
        speed = rand.nextInt(6) + 3;
        // Configurar dirección aleatoria: derecha (+1) o izquierda (-1)
        direction = rand.nextBoolean() ? 1 : -1;
    }

    /**
     * Asigna el ancho de la pantalla, utilizado para determinar los límites de movimiento.
     *
     * @param screenWidth Ancho de la pantalla.
     */
    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    /**
     * Configura el escalado del sprite en función de la altura del mapa y una proporción deseada.
     *
     * @param mapHeight    Altura del mapa.
     * @param desiredRatio Proporción deseada para la altura del obstáculo con respecto al mapa.
     */
    public void configureScale(int mapHeight, float desiredRatio) {
        int hOriginal = originalSprite.getHeight();
        float desiredHeight = mapHeight * desiredRatio;
        scaleFactor = desiredHeight / hOriginal;

        // Calcular nuevas dimensiones y redimensionar el sprite
        int newW = (int) (originalSprite.getWidth() * scaleFactor);
        int newH = (int) (originalSprite.getHeight() * scaleFactor);
        sprite = Bitmap.createScaledBitmap(originalSprite, newW, newH, true);
        width = sprite.getWidth();
        height = sprite.getHeight();
    }

    /**
     * Actualiza la posición del obstáculo moviéndolo horizontalmente según su velocidad y dirección.
     * Si el obstáculo sale de la pantalla, se reconfiguran sus propiedades de forma aleatoria.
     */
    @Override
    public void update() {
        // Mover el obstáculo horizontalmente
        x += speed * direction;

        // Si se mueve fuera de la pantalla, reinicializar sus propiedades
        if (direction > 0 && x > screenWidth) {
            randomizeProperties();
        } else if (direction < 0 && (x + width) < 0) {
            randomizeProperties();
        }
    }

    /**
     * Reconfigura aleatoriamente las propiedades del obstáculo, como la dirección y el sprite,
     * y lo posiciona justo fuera del borde opuesto de la pantalla.
     */
    private void randomizeProperties() {
        Random rand = new Random();

        // Mantener la velocidad actual y cambiar la dirección aleatoriamente
        direction = rand.nextBoolean() ? 1 : -1;

        // Seleccionar un nuevo sprite aleatorio del caché
        int randomIndex = rand.nextInt(cachedBitmaps.length);
        originalSprite = cachedBitmaps[randomIndex];

        // Redimensionar el sprite usando el factor de escala previamente calculado
        int newW = (int) (originalSprite.getWidth() * scaleFactor);
        int newH = (int) (originalSprite.getHeight() * scaleFactor);
        sprite = Bitmap.createScaledBitmap(originalSprite, newW, newH, true);
        width = sprite.getWidth();
        height = sprite.getHeight();

        // Reposicionar el obstáculo en el borde opuesto de la pantalla según la dirección
        if (direction > 0) {
            x = -width;
        } else {
            x = screenWidth;
        }
    }

    /**
     * Carga y cachea los bitmaps a partir de un sprite sheet dado.
     * Divide el sprite sheet en 4 frames (2 columnas x 2 filas).
     *
     * @param context    Contexto de la aplicación.
     * @param drawableId Identificador del recurso drawable del sprite sheet.
     */
    private static void loadBitmaps(Context context, int drawableId) {
        // Cargar la imagen completa del sprite sheet
        Bitmap fullSheet = BitmapFactory.decodeResource(context.getResources(), drawableId);
        int sheetWidth = fullSheet.getWidth();
        int sheetHeight = fullSheet.getHeight();

        // Dividir el sprite sheet en 4 partes iguales
        int singleWidth = sheetWidth / 2;
        int singleHeight = sheetHeight / 2;

        cachedBitmaps = new Bitmap[4];
        cachedBitmaps[0] = Bitmap.createBitmap(fullSheet, 0, 0, singleWidth, singleHeight);
        cachedBitmaps[1] = Bitmap.createBitmap(fullSheet, singleWidth, 0, singleWidth, singleHeight);
        cachedBitmaps[2] = Bitmap.createBitmap(fullSheet, 0, singleHeight, singleWidth, singleHeight);
        cachedBitmaps[3] = Bitmap.createBitmap(fullSheet, singleWidth, singleHeight, singleWidth, singleHeight);

        // Liberar la memoria del sprite sheet completo
        fullSheet.recycle();
    }

    /**
     * Ajusta la velocidad del obstáculo. Este método puede ser utilizado para modificar
     * la dificultad del juego en función del nivel.
     *
     * @param speed Nueva velocidad para el obstáculo.
     */
    public void setSpeed(int speed) {
        this.speed = speed;
    }

    /**
     * Dibuja el obstáculo en el canvas. Si la dirección es hacia la derecha, el sprite se voltea horizontalmente.
     *
     * @param canvas Canvas sobre el que se dibuja el obstáculo.
     */
    @Override
    public void draw(Canvas canvas) {
        if (sprite == null || canvas == null) return;

        canvas.save();
        // Si la dirección es positiva, voltear horizontalmente el sprite alrededor de su centro
        if (direction > 0) {
            float centerX = x + (width / 2f);
            float centerY = y + (height / 2f);
            canvas.scale(-1, 1, centerX, centerY);
        }
        canvas.drawBitmap(sprite, x, y, null);
        canvas.restore();
    }

    /**
     * Retorna la caja de colisión del obstáculo utilizando la implementación precisa heredada.
     *
     * @return {@link RectF} que representa la bounding box precisa del obstáculo.
     */
    @Override
    public RectF getBoundingBox() {
        return getBoundingBoxPrecise();
    }
}