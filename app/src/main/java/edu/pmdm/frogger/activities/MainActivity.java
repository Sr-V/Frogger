package edu.pmdm.frogger.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import edu.pmdm.frogger.firebase.FirebaseAuthManager;
import edu.pmdm.frogger.firebase.FirestoreManager;

/**
 * Actividad principal de la aplicación Frogger.
 * <p>
 * Esta clase se encarga de:
 * <ul>
 *   <li>Configurar la interfaz de usuario para una experiencia edge-to-edge.</li>
 *   <li>Obtener o crear los datos del usuario en Firestore.</li>
 *   <li>Actualizar la UI con el nombre del usuario, nivel actual y puntuación.</li>
 *   <li>Permitir la navegación a GameActivity y LevelSelectionActivity.</li>
 *   <li>Gestionar el cierre de sesión.</li>
 * </ul>
 * </p>
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FroggerMain";
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    // Variable global para almacenar el nivel actual del usuario
    private int currentLevel = 1;
    // Variable global para almacenar la puntuación actual del usuario
    private int currentScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Habilita la interfaz edge-to-edge
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Ajusta los insets para que la UI se posicione correctamente
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa los gestores de autenticación y Firestore
        authManager = FirebaseAuthManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();

        // Obtiene o crea los datos del usuario en Firestore
        getUserData();

        // Listener para el botón de LOGOUT
        findViewById(R.id.btnGoogleLogout).setOnClickListener(v -> signOut());

        // Listener para el botón PLAY GAME: envía a GameActivity el nivel actual del usuario
        findViewById(R.id.playGame).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("level", currentLevel);
            startActivity(intent);
        });

        // Listener para el botón LEVELS: envía a LevelSelectionActivity para la selección de niveles
        findViewById(R.id.levels).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LevelSelectionActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Obtiene el documento del usuario en Firestore. Si no existe, lo crea con datos por defecto.
     * <p>
     * Los datos por defecto incluyen:
     * <ul>
     *   <li>displayName: nombre del usuario autenticado.</li>
     *   <li>email: correo electrónico del usuario autenticado.</li>
     *   <li>currentLevel: nivel inicial (1).</li>
     *   <li>score: puntuación inicial (0).</li>
     * </ul>
     * </p>
     */
    private void getUserData() {
        String uid = authManager.getCurrentUser().getUid();
        firestoreManager.getUser(uid, task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    // Si el documento existe, actualiza la UI y guarda el nivel y puntuación actuales del usuario
                    String displayName = document.getString("displayName");
                    Long levelLong = document.getLong("currentLevel");
                    currentLevel = (levelLong != null) ? levelLong.intValue() : 1;
                    Long scoreLong = document.getLong("score");
                    currentScore = (scoreLong != null) ? scoreLong.intValue() : 0;
                    updateUIWithUserData(displayName, currentLevel, currentScore);
                } else {
                    // Si el documento no existe, crea un nuevo documento con datos por defecto
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("displayName", authManager.getCurrentUser().getDisplayName());
                    userData.put("email", authManager.getCurrentUser().getEmail());
                    userData.put("currentLevel", 1); // Nivel inicial
                    userData.put("score", 0);        // Puntuación inicial

                    firestoreManager.createOrUpdateUser(uid, userData)
                            .addOnCompleteListener(createTask -> {
                                if (createTask.isSuccessful()) {
                                    Log.d(TAG, "Usuario creado exitosamente en Firestore");
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
     * Actualiza la interfaz de usuario con la información obtenida del usuario.
     *
     * @param displayName Nombre del usuario para mostrar en la UI.
     * @param level       Nivel actual del usuario.
     * @param score       Puntuación actual del usuario.
     */
    @SuppressLint("SetTextI18n")
    private void updateUIWithUserData(String displayName, int level, int score) {
        // Actualiza el TextView con el nombre del usuario
        TextView tvUserName = findViewById(R.id.tvUserName);
        if (displayName != null) {
            tvUserName.setText("Bienvenido " + displayName);
        }
        // Actualiza el TextView con el nivel actual
        TextView tvCurrentLevel = findViewById(R.id.tvCurrentLevel);
        tvCurrentLevel.setText("Nivel: " + level);
        // Actualiza el TextView con la puntuación actual
        TextView tvCurrentScore = findViewById(R.id.tvCurrentScore);
        tvCurrentScore.setText("Puntuación: " + score);
    }

    /**
     * Cierra la sesión del usuario y redirige a LoginActivity.
     */
    private void signOut() {
        authManager.signOut(this, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Google sign out successful");
            }
            // Redirige a LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}