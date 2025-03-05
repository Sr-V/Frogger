package edu.pmdm.frogger.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.firebase.FirebaseAuthManager;
import edu.pmdm.frogger.firebase.FirestoreManager;
import edu.pmdm.frogger.game.GameEngine;
import edu.pmdm.frogger.game.GameEventsListener;
import edu.pmdm.frogger.game.Juego;
import edu.pmdm.frogger.utils.GameAudioManager;

/**
 * {@code GameActivity} es la actividad principal del juego. Se encarga de inicializar
 * y gestionar la lógica del juego, manejar los eventos de victoria y derrota, y actualizar
 * los datos del usuario en Firebase.
 *
 * <p>Implementa la interfaz {@link GameEventsListener} para reaccionar a los eventos del juego.
 */
public class GameActivity extends AppCompatActivity implements GameEventsListener {

    // Vista personalizada del juego
    private Juego juegoView;
    // Motor del juego
    private GameEngine gameEngine;
    // Botones de control del juego
    private ImageButton btnLeft, btnUp, btnRight, btnDown;
    // Nivel actual que se está jugando
    private int level;
    // Nivel actual del usuario almacenado en Firebase
    private int userCurrentLevel;
    // Gestor de audio del juego (singleton)
    private final GameAudioManager gam = GameAudioManager.getInstance(this);
    // Indicador para saber si la actividad está en pausa
    private boolean paused = false;

    /**
     * Método de ciclo de vida que se llama al crear la actividad.
     * Inicializa la interfaz de usuario, configura el modo inmersivo y prepara los controles.
     *
     * @param savedInstanceState Un {@code Bundle} que contiene el estado previo de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Configurar el modo fullscreen inmersivo
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN      // Ocultar la barra de estado
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION  // Ocultar la barra de navegación
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY   // Modo inmersivo
        );

        // Establecer el layout de la actividad
        setContentView(R.layout.activity_game);

        // Recuperar datos del Intent que inició la actividad
        level = getIntent().getIntExtra("level", 1);
        userCurrentLevel = getIntent().getIntExtra("userCurrentLevel", 1);

        // Obtener referencia al SurfaceView donde se dibuja el juego
        juegoView = findViewById(R.id.gameView);
        // Configurar el nivel del juego en la vista
        juegoView.setLevel(level);

        // Inicializar los botones de movimiento
        btnLeft  = findViewById(R.id.btnLeft);
        btnUp    = findViewById(R.id.btnUp);
        btnRight = findViewById(R.id.btnRight);
        btnDown  = findViewById(R.id.btnDown);

        // Crear el GameEngine y asociarlo a la vista del juego
        gameEngine = new GameEngine(this, level, userCurrentLevel, this);
        juegoView.setGameEngine(gameEngine);

        // Configurar listeners para los botones de movimiento
        btnLeft.setOnClickListener(v -> juegoView.movePlayerLeft());   // Mover jugador a la izquierda
        btnUp.setOnClickListener(v -> juegoView.movePlayerUp());         // Mover jugador hacia arriba
        btnRight.setOnClickListener(v -> juegoView.movePlayerRight());   // Mover jugador a la derecha
        btnDown.setOnClickListener(v -> juegoView.movePlayerDown());       // Mover jugador hacia abajo
    }

    /**
     * Maneja la pulsación del botón "Atrás".
     * Solicita confirmación de salida al juego y evita el comportamiento por defecto.
     */
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Solicitar confirmación para salir del juego
        juegoView.requestExitConfirmation();
        // No se llama a super.onBackPressed() para evitar salir sin confirmación
    }

    /**
     * Método invocado cuando el juego se gana.
     * Calcula las estrellas obtenidas, actualiza Firebase y controla la transición de nivel.
     *
     * @param shouldIncrementLevel Indica si se debe incrementar el nivel.
     */
    @Override
    public void onGameWon(boolean shouldIncrementLevel) {
        // Desactivar los botones de control del juego
        setButtonsEnabled(false);

        // Obtener el tiempo final transcurrido y el límite de tiempo del nivel
        long finalElapsed = gameEngine.getFinalElapsedTime();
        long timeLimit = gameEngine.getLevelTimeLimit();
        float porcentaje = (float) finalElapsed / timeLimit;

        // Determinar la cantidad de estrellas en función del porcentaje de tiempo transcurrido
        int estrellas;
        if (porcentaje <= 0.20f) {
            estrellas = 3; // 3 estrellas por terminar en menos del 20% del tiempo
        } else if (porcentaje <= 0.50f) {
            estrellas = 2; // 2 estrellas por terminar en menos del 50% del tiempo
        } else {
            estrellas = 1; // 1 estrella en caso contrario
        }

        // Informar al SurfaceView del número de estrellas obtenidas
        juegoView.setVictoryStars(estrellas);

        // Obtener el UID del usuario autenticado en Firebase
        String uid = FirebaseAuthManager.getInstance(this).getCurrentUser().getUid();

        // 1) Comprobar y actualizar las estrellas en la subcolección "maps"
        FirestoreManager.getInstance()
                .createOrUpdateUserMap(uid, String.valueOf(level), new HashMap<>())
                .addOnSuccessListener(aVoid -> {
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .collection("maps")
                            .document(String.valueOf(level))
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                int firebaseStars = 0;
                                // Si el documento existe y contiene el campo "stars", obtener el valor
                                if (documentSnapshot.exists() && documentSnapshot.contains("stars")) {
                                    firebaseStars = Objects.requireNonNull(documentSnapshot.getLong("stars")).intValue();
                                }
                                // 2) Actualizar el documento si se han obtenido más estrellas que las registradas en Firebase
                                if (estrellas > firebaseStars) {
                                    Map<String, Object> levelData = new HashMap<>();
                                    levelData.put("stars", estrellas);
                                    FirestoreManager.getInstance()
                                            .createOrUpdateUserMap(uid, String.valueOf(level), levelData)
                                            .addOnSuccessListener(unused -> recalcTotalStars(uid))
                                            .addOnFailureListener(e -> recalcTotalStars(uid));
                                } else {
                                    recalcTotalStars(uid);
                                }
                            })
                            .addOnFailureListener(e -> recalcTotalStars(uid));
                });

        // 3) Si el usuario juega en su currentLevel, se incrementa el nivel en Firebase
        if (userCurrentLevel == level) {
            int newLevel = userCurrentLevel + 1;
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentLevel", newLevel);
            FirestoreManager.getInstance().updateUserFields(uid, updates)
                    .addOnSuccessListener(aVoid -> userCurrentLevel = newLevel)
                    .addOnFailureListener(e -> {
                        // En caso de error, se continúa mostrando la victoria a través de Juego.draw()
                    });
        }
        // La ventana de victoria se dibuja en el método Juego.draw()
    }

    /**
     * Método invocado cuando el juego se pierde.
     * Desactiva los botones y dibuja la ventana de derrota.
     */
    @Override
    public void onGameLost() {
        runOnUiThread(() -> {
            setButtonsEnabled(false);
            // La ventana de derrota se dibuja en el método Juego.draw()
        });
    }

    /**
     * Método para bloquear o desbloquear los botones de control del juego.
     *
     * @param blocked Si {@code true}, desactiva los botones; si {@code false}, los activa.
     */
    @Override
    public void onButtonsBlocked(boolean blocked) {
        runOnUiThread(() -> setButtonsEnabled(!blocked));
    }

    /**
     * Habilita o deshabilita los botones de movimiento.
     *
     * @param enabled {@code true} para habilitar, {@code false} para deshabilitar.
     */
    private void setButtonsEnabled(boolean enabled) {
        btnLeft.setEnabled(enabled);
        btnUp.setEnabled(enabled);
        btnRight.setEnabled(enabled);
        btnDown.setEnabled(enabled);
    }

    /**
     * Recalcula el total de estrellas de TODOS los mapas del usuario y actualiza el campo totalStars en Firebase.
     *
     * @param uid Identificador único del usuario.
     */
    private void recalcTotalStars(String uid) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("maps")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalStars = 0;
                    // Verificar que la consulta no esté vacía
                    if (!querySnapshot.isEmpty()) {
                        // Sumar las estrellas de cada nivel
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            if (doc.contains("stars")) {
                                totalStars += Objects.requireNonNull(doc.getLong("stars")).intValue();
                            }
                        }
                    }
                    // Actualizar el campo totalStars en el documento principal del usuario
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("totalStars", totalStars);
                    FirestoreManager.getInstance().updateUserFields(uid, updates)
                            .addOnSuccessListener(aVoid -> {
                                // totalStars actualizado correctamente
                            })
                            .addOnFailureListener(e -> {
                                // Error al actualizar totalStars en Firebase
                            });
                })
                .addOnFailureListener(e -> {
                    // Error al leer la subcolección "maps" del usuario
                });
    }

    /**
     * Método del ciclo de vida invocado cuando la actividad se destruye.
     * Detiene los sonidos asociados al juego.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detener el sonido de idle
        gam.stopIdleSound();
        // Detener la música del nivel correspondiente
        if (level == 1) {
            gam.stopLevelOneTheme();
        }
        if (level == 2) {
            gam.stopLevelTwoTheme();
        }
        if (level == 3) {
            gam.stopLevelThreeTheme();
        }
    }

    /**
     * Método del ciclo de vida invocado cuando la actividad pasa a estado de pausa.
     * Detiene los sonidos y establece el indicador de pausa.
     */
    @Override
    protected void onPause() {
        super.onPause();
        // Detener sonidos de idle y música del nivel
        gam.stopIdleSound();
        if (level == 1) {
            gam.stopLevelOneTheme();
        }
        if (level == 2) {
            gam.stopLevelTwoTheme();
        }
        if (level == 3) {
            gam.stopLevelThreeTheme();
        }
        paused = true;
    }

    /**
     * Método del ciclo de vida invocado cuando la actividad se reanuda.
     * Reactiva los sonidos del juego si la actividad estaba en pausa.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            // Reiniciar la música y sonidos de idle según el nivel actual
            if (level == 1) {
                gam.levelOneTheme(this);
                gam.idleCroak(this);
                gam.carHonks(this);
            }
            if (level == 2) {
                gam.levelTwoTheme(this);
                gam.idleCroak(this);
                gam.carHonks(this);
            }
            if (level == 3) {
                gam.levelThreeTheme(this);
                gam.idleCroak(this);
                gam.carHonks(this);
            }
        }
        paused = false;
    }
}