package edu.pmdm.frogger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AlertDialog;
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

        // Obtenemos el UID del usuario logueado
        String uid = FirebaseAuthManager.getInstance(this).getCurrentUser().getUid();

        // 1) Primero, comprobamos cuántas estrellas tenía ya en este nivel
        FirestoreManager.getInstance()
                .createOrUpdateUserMap(uid, String.valueOf(level), new HashMap<>())
                // ↑ Si quieres asegurarte de que el documento exista, puedes crear uno vacío antes de leerlo.
                //   Si ya tienes un método distinto, puedes omitirlo.
                .addOnSuccessListener(aVoid -> {
                    // Leer el documento del nivel en la subcolección "maps"
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

                                // 2) Comparar y actualizar si es necesario
                                if (estrellas > firebaseStars) {
                                    Map<String, Object> levelData = new HashMap<>();
                                    levelData.put("stars", estrellas);
                                    // Puedes poner también "name" si quieres actualizar el nombre, p.ej.:
                                    // levelData.put("name", "Talavera de la Reina");

                                    FirestoreManager.getInstance()
                                            .createOrUpdateUserMap(uid, String.valueOf(level), levelData)
                                            .addOnSuccessListener(unused -> {
                                                // Una vez actualizado, recalcular el total
                                                recalcTotalStars(uid);
                                            })
                                            .addOnFailureListener(e -> {
                                                // Manejar error si lo deseas
                                                recalcTotalStars(uid);
                                            });
                                } else {
                                    // Si no es mayor, de todas formas recalculamos el total
                                    recalcTotalStars(uid);
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Si hubo error al obtener el documento, igualmente podemos recalcular
                                recalcTotalStars(uid);
                            });
                });

        // 3) Si el usuario está jugando justo en su currentLevel, incrementamos
        if (userCurrentLevel == level) {
            int newLevel = userCurrentLevel + 1;
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentLevel", newLevel);

            FirestoreManager.getInstance().updateUserFields(uid, updates)
                    .addOnSuccessListener(aVoid -> {
                        userCurrentLevel = newLevel;
                        showVictoryAlert(true, estrellas);
                    })
                    .addOnFailureListener(e -> {
                        showVictoryAlert(false, estrellas);
                    });
        } else {
            // Si no se incrementa el nivel, simplemente mostramos la alerta
            showVictoryAlert(false, estrellas);
        }
    }

    /**
     * Método auxiliar para recalcular el total de estrellas de TODOS los mapas de un usuario.
     * Al final, se actualiza el campo totalStars en el documento principal del usuario.
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
                        // Recorremos cada documento (cada nivel) y sumamos sus estrellas
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            if (doc.contains("stars")) {
                                totalStars += doc.getLong("stars").intValue();
                            }
                        }
                    }
                    // Actualizar el campo totalStars en el documento principal del usuario
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("totalStars", totalStars);
                    int finalTotalStars = totalStars;
                    FirestoreManager.getInstance().updateUserFields(uid, updates)
                            .addOnSuccessListener(aVoid -> {
                                // Si quieres hacer algo después de actualizar totalStars, lo pones aquí
                                Log.d("FIRESTORE", "totalStars actualizado a: " + finalTotalStars);
                            })
                            .addOnFailureListener(e -> {
                                // Manejar error
                                Log.e("FIRESTORE", "Error al actualizar totalStars", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Error al leer subcolección maps", e);
                });
    }

    @Override
    public void onGameLost() {
        runOnUiThread(() -> {
            setButtonsEnabled(false);
            showDefeatAlert();
        });
    }

    @Override
    public void onButtonsBlocked(boolean blocked) {
        runOnUiThread(() -> setButtonsEnabled(!blocked));
    }

    private void showVictoryAlert(boolean levelIncremented, int estrellas) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        String msg = "¡Has ganado!\nEstrellas obtenidas: " + estrellas;
        if (levelIncremented) {
            msg += "\n¡Se ha desbloqueado el siguiente nivel!";
        }
        if(level == 1){
            gam.stopLevelOneTheme();
            gam.stopIdleSound();
        }
        if(level == 2){
            gam.stopLevelTwoTheme();
            gam.stopIdleSound();
        }
        if(level == 3){
            gam.stopLevelThreeTheme();
            gam.stopIdleSound();
        }
        builder.setTitle("Victoria")
                .setMessage(msg)
                .setPositiveButton("Reintentar", (dialog, which) -> recreate())
                .setNegativeButton("Menú Principal", (dialog, which) -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .show();
    }

    private void showDefeatAlert() {
        String message;
        gam.stopIdleSound();
        if(level == 1){
            gam.stopLevelOneTheme();
        }
        if(level == 2){
            gam.stopLevelTwoTheme();
        }
        if(level == 3){
            gam.stopLevelThreeTheme();
        }
        if (gameEngine.isLostByTime()) {
            message = "¡Tiempo agotado!\nNo lograste completar el nivel a tiempo.";
        } else {
            message = "Te has quedado sin vidas. ¿Deseas reintentar o volver al menú?";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Derrota")
                .setMessage(message)
                .setPositiveButton("Reintentar", (dialog, which) -> recreate())
                .setNegativeButton("Menú Principal", (dialog, which) -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .show();
    }

    private void setButtonsEnabled(boolean enabled) {
        btnLeft.setEnabled(enabled);
        btnUp.setEnabled(enabled);
        btnRight.setEnabled(enabled);
        btnDown.setEnabled(enabled);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gam.stopIdleSound();
        if(level == 1){
            gam.stopLevelOneTheme();
        }
        if(level == 2){
            gam.stopLevelTwoTheme();
        }
        if(level == 3){
            gam.stopLevelThreeTheme();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        gam.stopIdleSound();
        if(level == 1){
            gam.stopLevelOneTheme();
        }
        if(level == 2){
            gam.stopLevelTwoTheme();
        }
        if(level == 3){
            gam.stopLevelThreeTheme();
        }
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(paused) {
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