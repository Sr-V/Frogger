package edu.pmdm.frogger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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
        if (userCurrentLevel == level) {
            String uid = FirebaseAuthManager.getInstance(this).getCurrentUser().getUid();
            int newLevel = userCurrentLevel + 1;
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentLevel", newLevel);
            FirestoreManager.getInstance().updateUserFields(uid, updates)
                    .addOnSuccessListener(aVoid -> {
                        userCurrentLevel = newLevel;
                        showVictoryAlert(true);
                    })
                    .addOnFailureListener(e -> {
                        showVictoryAlert(false);
                    });
        } else {
            showVictoryAlert(false);
        }
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

    private void showVictoryAlert(boolean levelIncremented) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        String msg = "¡Has ganado!";

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
        if (levelIncremented) {
            msg += "\n¡Se ha desbloqueado el siguiente nivel!";
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