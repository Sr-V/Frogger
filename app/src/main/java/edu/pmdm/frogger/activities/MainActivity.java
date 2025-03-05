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

/**
 * {@code MainActivity} es la actividad principal del juego Frogger.
 * Se encarga de mostrar la información del usuario (nombre, nivel y estrellas),
 * configurar el menú principal y gestionar la navegación a otras actividades.
 *
 * <p>La actividad obtiene o crea los datos del usuario en Firestore,
 * actualiza la UI dinámicamente y reproduce la música principal.
 */
public class MainActivity extends AppCompatActivity {

    // Tag para mensajes de log
    private static final String TAG = "FroggerMain";
    // Gestor de autenticación de Firebase
    private FirebaseAuthManager authManager;
    // Gestor de Firestore para consultas y actualizaciones
    private FirestoreManager firestoreManager;

    // Nivel actual y la puntuación (totalStars) del usuario
    private int currentLevel = 1;
    private int currentScore = 0; // Representa las estrellas totales del usuario

    // Gestor de audio para reproducir sonidos y música
    private GameAudioManager gam;

    // Overlay para mostrar ventanas retro (alertas)
    private AlertsOverlayView overlayView;

    // Barra de progreso para indicar carga de datos
    private ProgressBar progressBar;

    /**
     * Método del ciclo de vida que se invoca al crear la actividad.
     * Inicializa los gestores, configura la interfaz edge-to-edge,
     * ajusta los insets y carga los datos del usuario.
     *
     * @param savedInstanceState Estado previo de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializa el gestor de audio
        gam = GameAudioManager.getInstance(this);

        // Habilita la interfaz edge-to-edge para utilizar toda la pantalla
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Ajusta los insets para que la UI se posicione correctamente con respecto a las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa los gestores de autenticación y Firestore
        authManager = FirebaseAuthManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();

        // Obtiene la referencia al ProgressBar y lo muestra mientras se cargan los datos
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        // Inicializa el overlay para alertas y lo añade a la Activity
        overlayView = new AlertsOverlayView(this);
        addContentView(overlayView,
                new android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
        overlayView.setVisibility(View.GONE); // Oculta el overlay por defecto

        // Obtiene o crea los datos del usuario en Firestore
        getUserData();
    }

    /**
     * Obtiene el documento del usuario en Firestore. Si el documento existe,
     * actualiza la UI con los datos (nombre, nivel y estrellas). Si no existe,
     * crea uno con datos por defecto y además crea la subcolección "maps".
     */
    private void getUserData() {
        String uid = authManager.getCurrentUser().getUid();
        firestoreManager.getUser(uid, task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    // Si el documento existe, se extraen los datos del usuario
                    String displayName = document.getString("displayName");
                    Long levelLong = document.getLong("currentLevel");
                    currentLevel = (levelLong != null) ? levelLong.intValue() : 1;
                    Long starsLong = document.getLong("totalStars");
                    currentScore = (starsLong != null) ? starsLong.intValue() : 0;
                    updateUIWithUserData(displayName, currentLevel, currentScore);
                } else {
                    // Si el documento no existe, se crea uno nuevo con datos por defecto
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("displayName", authManager.getCurrentUser().getDisplayName());
                    userData.put("email", authManager.getCurrentUser().getEmail());
                    userData.put("currentLevel", 1);
                    userData.put("totalStars", 0);

                    firestoreManager.createOrUpdateUser(uid, userData)
                            .addOnCompleteListener(createTask -> {
                                if (createTask.isSuccessful()) {
                                    Log.d(TAG, "Usuario creado exitosamente en Firestore");
                                    // Crea la subcolección "maps" para almacenar datos de cada nivel
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
     * Crea la subcolección "maps" para el usuario. Esta subcolección contiene documentos
     * para cada nivel obtenido de la colección "levels", inicializando la cantidad de estrellas en 0.
     *
     * @param uid Identificador único del usuario.
     */
    private void crearSubcoleccionMaps(String uid) {
        firestoreManager.getAllLevels(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                // Para cada nivel, se crea un documento en la subcolección "maps" con datos iniciales
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
     * Actualiza la interfaz de usuario con los datos del usuario,
     * tales como el nombre, el nivel actual y la cantidad de estrellas.
     * Además, configura los elementos del menú principal.
     *
     * @param displayName Nombre del usuario.
     * @param level       Nivel actual del usuario.
     * @param score       Puntuación total (estrellas) del usuario.
     */
    @SuppressLint("SetTextI18n")
    private void updateUIWithUserData(String displayName, int level, int score) {
        // Oculta el ProgressBar al completar la carga de datos
        progressBar.setVisibility(View.GONE);

        // Configura la información superior (usuario, nivel y estrellas)
        setupTopInfo(displayName, level, score);

        // Configura y muestra las opciones del menú principal
        setupMenuItems();
    }

    /**
     * Configura la información superior de la pantalla principal mostrando:
     *  1) "Bienvenido [Nombre]"
     *  2) "Nivel: [Número]"
     *  3) "Estrellas Totales: [Puntuación]"
     *
     * @param displayName Nombre del usuario.
     * @param level       Nivel actual.
     * @param score       Total de estrellas.
     */
    private void setupTopInfo(String displayName, int level, int score) {
        LinearLayout linearLayoutTopInfo = findViewById(R.id.linearLayoutTopInfo);
        // Se remueven vistas anteriores para evitar duplicados
        linearLayoutTopInfo.removeAllViews();

        // Definir los textos a mostrar
        String[] topTexts = new String[]{
                "Bienvenido " + displayName,
                "Nivel: " + level,
                "Estrellas Totales: " + score
        };

        // Inflar el layout "top_info_item.xml" para cada uno de los textos
        for (String text : topTexts) {
            View topView = getLayoutInflater().inflate(R.layout.top_info_item, linearLayoutTopInfo, false);
            TextView tv = topView.findViewById(R.id.tvTopInfo);
            tv.setText(text);
            linearLayoutTopInfo.addView(topView);
        }
    }

    /**
     * Configura las opciones del menú principal mostrando tarjetas para:
     * PLAY GAME, LEVELS, LEADERBOARD, SETTINGS y LOGOUT.
     * Cada tarjeta tiene un título, subtítulo, ícono y una acción asociada.
     */
    private void setupMenuItems() {
        LinearLayout linearLayoutMenu = findViewById(R.id.linearLayoutMenu);

        // Limpiar las opciones anteriores para evitar duplicados
        linearLayoutMenu.removeAllViews();

        // Definir cada opción del menú con su título, subtítulo, ícono y acción
        MenuOption[] menuOptions = new MenuOption[]{
                new MenuOption("PLAY GAME", "Comienza la aventura", R.drawable.joystick, v -> {
                    // Si se alcanza el nivel 4, muestra una alerta de que no hay nuevos niveles
                    if (currentLevel == 4) {
                        overlayView.showNoNewLevelsWindow();
                    } else {
                        // Detener la música principal y lanzar GameActivity con el nivel actual
                        gam.stopMainThemeSong();
                        Intent intent = new Intent(MainActivity.this, GameActivity.class);
                        intent.putExtra("level", currentLevel);
                        intent.putExtra("userCurrentLevel", currentLevel);
                        startActivity(intent);
                    }
                }),
                new MenuOption("LEVELS", "Elige tu nivel", R.drawable.flag, v -> {
                    // Lanza la actividad de selección de niveles
                    Intent intent = new Intent(MainActivity.this, LevelSelectionActivity.class);
                    startActivity(intent);
                }),
                new MenuOption("LEADERBOARD", "Ver clasificaciones", R.drawable.crown, v -> {
                    // Lanza la actividad del leaderboard
                    Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
                    startActivity(intent);
                }),
                new MenuOption("SETTINGS", "Configura el juego", R.drawable.settings, v -> {
                    // Lanza la actividad de configuración del juego
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                }),
                new MenuOption("LOGOUT", "Salir de la cuenta", R.drawable.logout, v -> {
                    // Realiza el proceso de cierre de sesión y finaliza la actividad
                    signOut();
                    finish();
                })
        };

        // Inflar el layout "main_menu_item.xml" para cada opción del menú
        for (MenuOption option : menuOptions) {
            View itemView = getLayoutInflater().inflate(R.layout.main_menu_item, linearLayoutMenu, false);

            TextView tvTitle = itemView.findViewById(R.id.tvMenuTitle);
            TextView tvSubtitle = itemView.findViewById(R.id.tvMenuSubtitle);
            ImageView ivIcon = itemView.findViewById(R.id.ivMenuIcon);

            tvTitle.setText(option.title);
            tvSubtitle.setText(option.subtitle);
            ivIcon.setImageResource(option.iconRes);

            // Asigna la acción de clic definida en la opción
            itemView.setOnClickListener(option.onClick);

            // Agrega la vista al contenedor del menú
            linearLayoutMenu.addView(itemView);
        }
    }

    /**
     * Clase interna para representar cada opción del menú principal.
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
     * Cierra la sesión del usuario con Google y redirige a la pantalla de Login.
     */
    private void signOut() {
        authManager.signOut(this, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Google sign out successful");
            }
            // Inicia la actividad de Login y finaliza la actual
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Detiene la música principal cuando la actividad se destruye.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("OnDestroy", "Destruida");
        gam.stopMainThemeSong();
    }

    /**
     * Detiene la música principal cuando la actividad pasa a estado de pausa.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("OnPause", "Pausada");
        gam.stopMainThemeSong();
    }

    /**
     * Reproduce la música principal cuando la actividad se inicia.
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d("OnStart", "Iniciada");
        gam.mainThemeSong(this);
    }

    /**
     * Maneja la pulsación del botón "Atrás", finalizando la actividad.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    /**
     * Actualiza los datos del usuario cada vez que la actividad se reanuda.
     */
    @Override
    protected void onResume() {
        super.onResume();
        getUserData();
    }
}