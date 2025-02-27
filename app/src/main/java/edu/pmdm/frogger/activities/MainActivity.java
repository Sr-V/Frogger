package edu.pmdm.frogger.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.utils.AlertsOverlayView;
import edu.pmdm.frogger.firebase.FirebaseAuthManager;
import edu.pmdm.frogger.firebase.FirestoreManager;
import edu.pmdm.frogger.utils.GameAudioManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FroggerMain";
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    // Nivel y puntuación actuales del usuario
    private int currentLevel = 1;
    private int currentScore = 0; // ahora representa totalStars
    private GameAudioManager gam = GameAudioManager.getInstance();

    // Nuevo: referencia al OverlayView para ventanas retro
    private AlertsOverlayView overlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Habilita la interfaz edge-to-edge
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Ajusta los insets para posicionar correctamente la UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa los gestores de autenticación y Firestore
        authManager = FirebaseAuthManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();

        // Inicialmente se oculta el contenido principal
        findViewById(R.id.tvUserName).setVisibility(View.GONE);
        findViewById(R.id.tvCurrentLevel).setVisibility(View.GONE);
        findViewById(R.id.tvCurrentScore).setVisibility(View.GONE);
        findViewById(R.id.playGame).setVisibility(View.GONE);
        findViewById(R.id.levels).setVisibility(View.GONE);
        findViewById(R.id.btnGoogleLogout).setVisibility(View.GONE);

        // Se muestra el ProgressBar mientras se cargan los datos
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        // OverlayView para ventanas retro
        overlayView = new AlertsOverlayView(this);
        // Añadimos la vista sobre la Activity
        addContentView(overlayView,
                new android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
        overlayView.setVisibility(View.GONE); // Oculta por defecto

        // Obtiene o crea los datos del usuario en Firestore
        getUserData();

        // Listener para el botón de LOGOUT
        findViewById(R.id.btnGoogleLogout).setOnClickListener(v -> {
            signOut();
            finish();
        });

        // Listener para el botón PLAY GAME:
        // Si currentLevel es 4, mostramos la ventana retro "Niveles no disponibles"
        findViewById(R.id.playGame).setOnClickListener(v -> {
            if (currentLevel == 4) {
                // Muestra la ventana retro en vez de AlertDialog
                overlayView.showNoNewLevelsWindow();
            } else {
                gam.stopMainThemeSong();
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra("level", currentLevel);
                intent.putExtra("userCurrentLevel", currentLevel);
                startActivity(intent);
            }
        });

        // Listener para el botón LEVELS: envía a LevelSelectionActivity para la selección de niveles
        findViewById(R.id.levels).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LevelSelectionActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Obtiene el documento del usuario en Firestore. Si no existe, lo crea con datos por defecto.
     */
    private void getUserData() {
        String uid = authManager.getCurrentUser().getUid();
        firestoreManager.getUser(uid, task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    // Si el documento existe, actualiza la UI y guarda el nivel y totalStars actuales
                    String displayName = document.getString("displayName");
                    Long levelLong = document.getLong("currentLevel");
                    currentLevel = (levelLong != null) ? levelLong.intValue() : 1;
                    Long starsLong = document.getLong("totalStars");
                    currentScore = (starsLong != null) ? starsLong.intValue() : 0;
                    updateUIWithUserData(displayName, currentLevel, currentScore);
                } else {
                    // Si el documento no existe, crea un nuevo documento con datos por defecto
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("displayName", authManager.getCurrentUser().getDisplayName());
                    userData.put("email", authManager.getCurrentUser().getEmail());
                    userData.put("currentLevel", 1);
                    userData.put("totalStars", 0);

                    firestoreManager.createOrUpdateUser(uid, userData)
                            .addOnCompleteListener(createTask -> {
                                if (createTask.isSuccessful()) {
                                    Log.d(TAG, "Usuario creado exitosamente en Firestore");
                                    crearSubcoleccionMaps(uid);
                                    updateUIWithUserData(authManager.getCurrentUser().getDisplayName(), 1, 0);
                                } else {
                                    Log.e(TAG, "Error al crear usuario", createTask.getException());
                                }
                            });
                }
            } else {
                Log.e(TAG, "Error al obtener datos de usuario", task.getException());
            }
        });
    }

    /**
     * Crea la subcolección "maps" para el usuario, basándose en los niveles de la colección "levels".
     */
    private void crearSubcoleccionMaps(String uid) {
        firestoreManager.getAllLevels(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                task.getResult().getDocuments().forEach(document -> {
                    String levelId = document.getId();
                    Map<String, Object> levelData = new HashMap<>();
                    levelData.put("name", document.getString("name"));
                    levelData.put("stars", 0);
                    firestoreManager.createOrUpdateUserMap(uid, levelId, levelData);
                });
            } else {
                Log.e(TAG, "Error al obtener niveles", task.getException());
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateUIWithUserData(String displayName, int level, int score) {
        TextView tvUserName = findViewById(R.id.tvUserName);
        if (displayName != null) {
            tvUserName.setText("Bienvenido " + displayName);
        }
        TextView tvCurrentLevel = findViewById(R.id.tvCurrentLevel);
        tvCurrentLevel.setText("Nivel: " + level);
        TextView tvCurrentScore = findViewById(R.id.tvCurrentScore);
        tvCurrentScore.setText("Estrellas Totales: " + score);

        // Oculta el ProgressBar y muestra el contenido principal
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        findViewById(R.id.tvUserName).setVisibility(View.VISIBLE);
        findViewById(R.id.tvCurrentLevel).setVisibility(View.VISIBLE);
        findViewById(R.id.tvCurrentScore).setVisibility(View.VISIBLE);
        findViewById(R.id.playGame).setVisibility(View.VISIBLE);
        findViewById(R.id.levels).setVisibility(View.VISIBLE);
        findViewById(R.id.btnGoogleLogout).setVisibility(View.VISIBLE);
    }

    private void signOut() {
        authManager.signOut(this, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Google sign out successful");
            }
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("OnDestroy","Destruida");
        gam.stopMainThemeSong();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("OnPause","Pausada");
        gam.stopMainThemeSong();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("OnResume","Resumida");
        gam.mainThemeSong(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}