package edu.pmdm.frogger.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import edu.pmdm.frogger.utils.AlertsOverlayView;
import edu.pmdm.frogger.utils.GameAudioManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FroggerMain";
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;

    // Nivel y puntuación actuales del usuario
    private int currentLevel = 1;
    private int currentScore = 0; // ahora representa totalStars

    // Para reproducir audio
    private GameAudioManager gam;

    // Overlay para ventanas retro
    private AlertsOverlayView overlayView;

    // Referencia al ProgressBar (para mostrar/hide mientras se cargan datos)
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gam = GameAudioManager.getInstance(this);

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

        // Referencia al ProgressBar
        progressBar = findViewById(R.id.progressBar);

        // Se muestra el ProgressBar mientras se cargan los datos
        progressBar.setVisibility(View.VISIBLE);

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
                    // Si el documento no existe, crea uno nuevo con datos por defecto
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

    /**
     * Actualiza la UI con los datos del usuario (nombre, nivel, estrellas) y crea los ítems del menú.
     */
    @SuppressLint("SetTextI18n")
    private void updateUIWithUserData(String displayName, int level, int score) {
        // Ocultamos el ProgressBar
        progressBar.setVisibility(View.GONE);

        // Creamos los ítems de la parte superior (usuario, nivel, estrellas)
        setupTopInfo(displayName, level, score);

        // Ahora creamos dinámicamente las tarjetas de menú (PLAY GAME, LEVELS, etc.)
        setupMenuItems();
    }

    /**
     * Crea tres tarjetas (items) en la parte superior:
     *  1) "Bienvenido X"
     *  2) "Nivel: Y"
     *  3) "Estrellas Totales: Z"
     */
    private void setupTopInfo(String displayName, int level, int score) {
        LinearLayout linearLayoutTopInfo = findViewById(R.id.linearLayoutTopInfo);
        linearLayoutTopInfo.removeAllViews();

        // Definimos los textos de cada item
        String[] topTexts = new String[]{
                "Bienvenido " + displayName,
                "Nivel: " + level,
                "Estrellas Totales: " + score
        };

        // Inflamos un layout "main_top_item.xml" por cada uno
        for (String text : topTexts) {
            View topView = getLayoutInflater().inflate(R.layout.top_info_item, linearLayoutTopInfo, false);
            TextView tv = topView.findViewById(R.id.tvTopInfo);
            tv.setText(text);

            linearLayoutTopInfo.addView(topView);
        }
    }

    /**
     * Crea la lista de opciones de menú como tarjetas y las añade a linearLayoutMenu.
     */
    private void setupMenuItems() {
        LinearLayout linearLayoutMenu = findViewById(R.id.linearLayoutMenu);

        // Definimos cada opción con su título, subtítulo, icono y acción
        MenuOption[] menuOptions = new MenuOption[]{
                new MenuOption("PLAY GAME", "Comienza la aventura", R.drawable.joystick, v -> {
                    if (currentLevel == 4) {
                        overlayView.showNoNewLevelsWindow();
                    } else {
                        gam.stopMainThemeSong();
                        Intent intent = new Intent(MainActivity.this, GameActivity.class);
                        intent.putExtra("level", currentLevel);
                        intent.putExtra("userCurrentLevel", currentLevel);
                        startActivity(intent);
                    }
                }),
                new MenuOption("LEVELS", "Elige tu nivel", R.drawable.flag, v -> {
                    Intent intent = new Intent(MainActivity.this, LevelSelectionActivity.class);
                    startActivity(intent);
                }),
                new MenuOption("LEADERBOARD", "Ver clasificaciones", R.drawable.crown, v -> {
                    Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
                    startActivity(intent);
                }),
                new MenuOption("SETTINGS", "Configura el juego", R.drawable.settings, v -> {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                }),
                new MenuOption("LOGOUT", "Salir de la cuenta", R.drawable.logout, v -> {
                    signOut();
                    finish();
                })
        };

        // Inflamos un "main_menu_item" por cada opción
        for (MenuOption option : menuOptions) {
            View itemView = getLayoutInflater().inflate(R.layout.main_menu_item, linearLayoutMenu, false);

            TextView tvTitle = itemView.findViewById(R.id.tvMenuTitle);
            TextView tvSubtitle = itemView.findViewById(R.id.tvMenuSubtitle);
            ImageView ivIcon = itemView.findViewById(R.id.ivMenuIcon);

            tvTitle.setText(option.title);
            tvSubtitle.setText(option.subtitle);
            ivIcon.setImageResource(option.iconRes);

            // Asignamos la acción de click a la tarjeta entera
            itemView.setOnClickListener(option.onClick);

            // Añadimos la vista al contenedor
            linearLayoutMenu.addView(itemView);
        }
    }

    /**
     * Clase interna para representar cada opción del menú.
     */
    private static class MenuOption {
        String title;
        String subtitle;
        int iconRes;
        View.OnClickListener onClick;

        MenuOption(String t, String sub, int icon, View.OnClickListener click) {
            title = t;
            subtitle = sub;
            iconRes = icon;
            onClick = click;
        }
    }

    /**
     * Cierra la sesión con Google y regresa a la pantalla de Login.
     */
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
        Log.d("OnDestroy", "Destruida");
        gam.stopMainThemeSong();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("OnPause", "Pausada");
        gam.stopMainThemeSong();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("OnStart", "Iniciada");
        gam.mainThemeSong(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
