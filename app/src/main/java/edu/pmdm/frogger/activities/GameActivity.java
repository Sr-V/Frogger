package edu.pmdm.frogger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import java.util.HashMap;
import java.util.Map;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.firebase.FirebaseAuthManager;
import edu.pmdm.frogger.firebase.FirestoreManager;
import edu.pmdm.frogger.game.GameEngine;
import edu.pmdm.frogger.game.GameEventsListener;
import edu.pmdm.frogger.game.Juego;

public class GameActivity extends AppCompatActivity implements GameEventsListener {

    private Juego juegoView;
    private GameEngine gameEngine;

    private ImageButton btnLeft, btnUp, btnRight;

    private int level;            // nivel que estamos jugando
    private int userCurrentLevel; // nivel actual del usuario en Firebase

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

        // Recuperar datos (por ejemplo, si los pasaste en el Intent)
        // Si no, pon valores por defecto
        level = getIntent().getIntExtra("level", 1);
        userCurrentLevel = getIntent().getIntExtra("userCurrentLevel", 1);

        // Referencia al SurfaceView
        juegoView = findViewById(R.id.gameView);
        // Configurar el mapa según el nivel recibido
        juegoView.setLevel(level);

        // Botones
        btnLeft  = findViewById(R.id.btnLeft);
        btnUp    = findViewById(R.id.btnUp);
        btnRight = findViewById(R.id.btnRight);

        // Crear GameEngine con la interfaz
        gameEngine = new GameEngine(
                this,         // contexto
                level,
                userCurrentLevel,
                this          // GameEventsListener
        );
        // Pasar el gameEngine a juegoView
        juegoView.setGameEngine(gameEngine);

        // Listeners de botones
        btnLeft.setOnClickListener(v -> juegoView.movePlayerLeft());
        btnUp.setOnClickListener(v -> juegoView.movePlayerUp());
        btnRight.setOnClickListener(v -> juegoView.movePlayerRight());
    }

    // ============ Implementación GameEventsListener ============

    /**
     * Llamado cuando la rana llega a la última fila (victoria).
     * @param shouldIncrementLevel true => hay que subir currentLevel en Firebase.
     */
    @Override
    public void onGameWon(boolean shouldIncrementLevel) {
        // Bloquear botones para que no se siga moviendo
        setButtonsEnabled(false);

        if (shouldIncrementLevel) {
            // Subir currentLevel en Firebase
            // Obtener el UID del usuario logueado
            String uid = FirebaseAuthManager
                    .getInstance(this)
                    .getCurrentUser()
                    .getUid();

            int newLevel = userCurrentLevel + 1;

            Map<String, Object> updates = new HashMap<>();
            updates.put("currentLevel", newLevel);

            FirestoreManager.getInstance().updateUserFields(uid, updates)
                    .addOnSuccessListener(aVoid -> {
                        userCurrentLevel = newLevel;
                        showVictoryAlert(true);
                    })
                    .addOnFailureListener(e -> {
                        // Si falla la subida de nivel, igualmente mostramos la alerta
                        showVictoryAlert(false);
                    });
        } else {
            showVictoryAlert(false);
        }
    }

    /**
     * Llamado cuando el usuario se queda sin vidas.
     */
    @Override
    public void onGameLost() {
        // Asegurarse de que se ejecute en el hilo principal
        runOnUiThread(() -> {
            setButtonsEnabled(false);
            showDefeatAlert();
        });
    }

    /**
     * Bloquear o desbloquear botones cuando la rana muere y reaparece.
     */
    @Override
    public void onButtonsBlocked(boolean blocked) {
        // Ejecutar la actualización de la UI en el hilo principal
        runOnUiThread(() -> setButtonsEnabled(!blocked));
    }

    // ============ Alertas de Victoria / Derrota ============

    private void showVictoryAlert(boolean levelIncremented) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String msg = "¡Has ganado!";
        if (levelIncremented) {
            msg += "\n¡Se ha desbloqueado el siguiente nivel!";
        }
        builder.setTitle("Victoria")
                .setMessage(msg)
                .setPositiveButton("Reintentar", (dialog, which) -> {
                    // Reiniciar la Activity
                    recreate();
                })
                .setNegativeButton("Menú Principal", (dialog, which) -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .show();
    }

    private void showDefeatAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Derrota")
                .setMessage("Te has quedado sin vidas. ¿Deseas reintentar o volver al menú?")
                .setPositiveButton("Reintentar", (dialog, which) -> {
                    // Reiniciar la Activity
                    recreate();
                })
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
    }
}