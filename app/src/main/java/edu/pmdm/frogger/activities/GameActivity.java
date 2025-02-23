package edu.pmdm.frogger.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.game.Juego;


public class GameActivity extends AppCompatActivity {

    private Juego juegoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Indicar que queremos manejar manualmente el "fit" de system windows
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Ajustar flags de UI para fullscreen inmersivo
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.activity_game);

        // 1) Referencia a la SurfaceView
        juegoView = findViewById(R.id.gameView);

        // 2) Botones
        ImageButton btnLeft = findViewById(R.id.btnLeft);
        ImageButton btnUp   = findViewById(R.id.btnUp);
        ImageButton btnRight= findViewById(R.id.btnRight);

        // 3) Listeners de los botones
        btnLeft.setOnClickListener(v -> {
            // Llamamos a un método que mueva la rana a la izquierda
            juegoView.movePlayerLeft();
        });

        btnUp.setOnClickListener(v -> {
            // Mover la rana arriba
            juegoView.movePlayerUp();
        });

        btnRight.setOnClickListener(v -> {
            // Mover la rana derecha
            juegoView.movePlayerRight();
        });
    }
}