package edu.pmdm.frogger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import edu.pmdm.frogger.R;
import edu.pmdm.frogger.firebase.FirebaseAuthManager;
import edu.pmdm.frogger.firebase.FirestoreManager;
import edu.pmdm.frogger.game.GameEngine;
import edu.pmdm.frogger.game.GameEventsListener;
import edu.pmdm.frogger.game.Juego;
import edu.pmdm.frogger.utils.GameAudioManager;

public class GameActivity extends AppCompatActivity implements GameEventsListener {

    private Juego juegoView;
    private GameEngine gameEngine;
    private ImageButton btnLeft, btnUp, btnRight, btnDown;
    private int level;            // Nivel que se está jugando
    private int userCurrentLevel; // Nivel actual del usuario en Firebase
    private GameAudioManager gam = GameAudioManager.getInstance();
    private boolean paused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Modo fullscreen inmersivo
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.activity_game);

        // Recuperar datos del Intent
        level = getIntent().getIntExtra("level", 1);
        userCurrentLevel = getIntent().getIntExtra("userCurrentLevel", 1);

        // Referencia al SurfaceView del juego
        juegoView = findViewById(R.id.gameView);
        juegoView.setLevel(level);

        // Inicializar botones de movimiento
        btnLeft  = findViewById(R.id.btnLeft);
        btnUp    = findViewById(R.id.btnUp);
        btnRight = findViewById(R.id.btnRight);
        btnDown  = findViewById(R.id.btnDown);

        // Crear GameEngine y asociarlo a la vista
        gameEngine = new GameEngine(this, level, userCurrentLevel, this);
        juegoView.setGameEngine(gameEngine);

        // Configurar listeners de botones
        btnLeft.setOnClickListener(v -> juegoView.movePlayerLeft());
        btnUp.setOnClickListener(v -> juegoView.movePlayerUp());
        btnRight.setOnClickListener(v -> juegoView.movePlayerRight());
        btnDown.setOnClickListener(v -> juegoView.movePlayerDown());
    }

    @Override
    public void onGameWon(boolean shouldIncrementLevel) {
        setButtonsEnabled(false);
        // Obtener el tiempo final transcurrido y el límite de tiempo
        long finalElapsed = gameEngine.getFinalElapsedTime();
        long timeLimit = gameEngine.getLevelTimeLimit();
        float porcentaje = (float) finalElapsed / timeLimit;

        // Determinar la cantidad de estrellas en función del tiempo
        int estrellas;
        if (porcentaje <= 0.20f) {
            estrellas = 3;
        } else if (porcentaje <= 0.50f) {
            estrellas = 2;
        } else {
            estrellas = 1;
        }

        // **** AÑADIR ESTA LÍNEA ****
        // Informamos al SurfaceView (Juego) del número de estrellas
        juegoView.setVictoryStars(estrellas);

        // Obtenemos el UID del usuario logueado
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
                                if (documentSnapshot.exists() && documentSnapshot.contains("stars")) {
                                    firebaseStars = documentSnapshot.getLong("stars").intValue();
                                }
                                // 2) Si se han obtenido más estrellas, se actualiza el documento
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
        // La ventana de victoria se dibuja en Juego.draw()
    }

    @Override
    public void onGameLost() {
        runOnUiThread(() -> {
            setButtonsEnabled(false);
            // La ventana de derrota se dibuja en Juego.draw()
        });
    }

    @Override
    public void onButtonsBlocked(boolean blocked) {
        runOnUiThread(() -> setButtonsEnabled(!blocked));
    }

    private void setButtonsEnabled(boolean enabled) {
        btnLeft.setEnabled(enabled);
        btnUp.setEnabled(enabled);
        btnRight.setEnabled(enabled);
        btnDown.setEnabled(enabled);
    }

    /**
     * Recalcula el total de estrellas de TODOS los mapas del usuario y actualiza el campo totalStars.
     */
    private void recalcTotalStars(String uid) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("maps")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalStars = 0;
                    if (!querySnapshot.isEmpty()) {
                        // Sumamos las estrellas de cada nivel
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            if (doc.contains("stars")) {
                                totalStars += doc.getLong("stars").intValue();
                            }
                        }
                    }
                    // Actualizar el campo totalStars en el documento principal del usuario
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("totalStars", totalStars);
                    FirestoreManager.getInstance().updateUserFields(uid, updates)
                            .addOnSuccessListener(aVoid -> {
                                // totalStars actualizado
                            })
                            .addOnFailureListener(e -> {
                                // Error al actualizar totalStars
                            });
                })
                .addOnFailureListener(e -> {
                    // Error al leer la subcolección "maps"
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
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

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
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