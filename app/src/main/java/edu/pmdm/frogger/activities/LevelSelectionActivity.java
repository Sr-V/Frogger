package edu.pmdm.frogger.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.firebase.FirebaseAuthManager;
import edu.pmdm.frogger.firebase.FirestoreManager;
import edu.pmdm.frogger.utils.AlertsOverlayView;

/**
 * {@code LevelSelectionActivity} es la actividad encargada de mostrar la selección de niveles
 * disponibles para el usuario. Muestra cada nivel como un botón personalizado que indica si el nivel
 * está bloqueado, desbloqueado o es "Próximamente".
 * <p>
 * Se utiliza Firebase para obtener el nivel actual del usuario y los datos de cada nivel.
 */
public class LevelSelectionActivity extends AppCompatActivity {

    // Tag para log de depuración
    private static final String TAG = "LevelSelectionActivity";

    // Gestores de Firestore y autenticación
    private FirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;

    // Contenedor para los items de nivel (donde se agregan las vistas de cada nivel)
    private LinearLayout linearLayoutLevels;

    // Nivel actual (o máximo desbloqueado) del usuario
    private int userCurrentLevel = 1;

    // Overlay para mostrar ventanas retro (por ejemplo, "Próximamente")
    private AlertsOverlayView overlayView;

    /**
     * Método del ciclo de vida que se invoca al crear la actividad.
     * Configura la interfaz de usuario, inicializa los gestores y carga los niveles.
     *
     * @param savedInstanceState Estado previo de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Habilitar modo EdgeToEdge para un layout de pantalla completa
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_level_selection);

        // Ajustar los insets para los system bars (barras de estado y navegación)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar gestores de Firebase y autenticación
        firestoreManager = FirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance(this);

        // Obtener la referencia del contenedor de niveles
        linearLayoutLevels = findViewById(R.id.linearLayoutLevels);

        // Agregar el overlay para ventanas retro
        overlayView = new AlertsOverlayView(this);
        addContentView(overlayView,
                new android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
        // Inicialmente ocultamos el overlay
        overlayView.setVisibility(View.GONE);

        // Obtener el nivel actual del usuario y, al finalizar, cargar la lista de niveles
        getUserCurrentLevel();

        // Configurar el botón para volver a la actividad principal
        Button btnBackToMain = findViewById(R.id.btnBackToMain);
        btnBackToMain.setOnClickListener(v -> finish());
    }

    /**
     * Muestra el GIF animado de candado (lock) si la API es 28 o superior.
     * En versiones anteriores se muestra una imagen estática de candado.
     *
     * @param imageView La vista de imagen en la que se mostrará el candado.
     */
    private void showLockGif(ImageView imageView) {
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                // Crear fuente para el decodificador de imágenes usando el recurso R.raw.lock
                ImageDecoder.Source source = ImageDecoder.createSource(getResources(), R.raw.lock);
                // Decodificar el drawable a partir de la fuente
                Drawable drawable = ImageDecoder.decodeDrawable(source);

                // Si el drawable es animado, iniciar la animación
                if (drawable instanceof AnimatedImageDrawable) {
                    ((AnimatedImageDrawable) drawable).start();
                }
                // Establecer el drawable en la ImageView
                imageView.setImageDrawable(drawable);

            } catch (IOException e) {
                e.printStackTrace();
                // En caso de error, usar la imagen estática de candado como fallback
                imageView.setImageResource(R.drawable.lock);
            }
        } else {
            // Fallback para versiones anteriores a API 28
            imageView.setImageResource(R.drawable.lock);
        }
    }

    /**
     * Obtiene el nivel actual del usuario desde Firebase y luego carga todos los niveles.
     */
    private void getUserCurrentLevel() {
        String uid = authManager.getCurrentUser().getUid();
        // Consulta a Firebase para obtener el documento del usuario
        firestoreManager.getUser(uid, task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    // Obtener el campo "currentLevel" del documento
                    Long levelLong = document.getLong("currentLevel");
                    if (levelLong != null) {
                        userCurrentLevel = levelLong.intValue();
                    }
                }
            } else {
                Log.e(TAG, "Error al obtener datos de usuario", task.getException());
            }
            // Cargar la lista de niveles después de obtener el nivel actual del usuario
            loadAllLevels();
        });
    }

    /**
     * Carga todos los niveles disponibles desde Firebase y, para cada uno, obtiene
     * la cantidad de estrellas del usuario para mostrar la información correspondiente.
     */
    private void loadAllLevels() {
        // Consulta a Firebase para obtener todos los niveles
        firestoreManager.getAllLevels(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot levelQuery = task.getResult();
                if (levelQuery != null) {
                    String uid = authManager.getCurrentUser().getUid();

                    // Obtener los datos de los mapas del usuario (donde se guardan las estrellas)
                    firestoreManager.getUserMaps(uid, mapsTask -> {
                        if (mapsTask.isSuccessful()) {
                            QuerySnapshot mapsSnapshot = mapsTask.getResult();

                            // Construir un mapa <levelId, starsCount> con la cantidad de estrellas por nivel
                            Map<String, Integer> userStarsMap = new HashMap<>();
                            if (mapsSnapshot != null) {
                                for (DocumentSnapshot mapDoc : mapsSnapshot) {
                                    Long starsLong = mapDoc.getLong("stars");
                                    int starCount = (starsLong != null) ? starsLong.intValue() : 0;
                                    userStarsMap.put(mapDoc.getId(), starCount);
                                }
                            }

                            // Para cada nivel obtenido, crear el botón (vista) correspondiente
                            for (DocumentSnapshot doc : levelQuery.getDocuments()) {
                                String levelId = doc.getId();  // Por ejemplo: "1", "2", "3", ...
                                String levelName = doc.getString("name");
                                int starCount = userStarsMap.getOrDefault(levelId, 0);

                                createLevelButton(levelId, levelName, starCount);
                            }
                        } else {
                            Log.e(TAG, "Error al obtener maps del usuario", mapsTask.getException());
                        }
                    });
                }
            } else {
                Log.e(TAG, "Error al cargar niveles", task.getException());
            }
        });
    }

    /**
     * Crea y configura el botón (vista) para un nivel determinado.
     * Dependiendo del estado del nivel (bloqueado, desbloqueado o "Próximamente"),
     * se asigna un comportamiento y apariencia específicos.
     *
     * @param levelId   Identificador del nivel (ej. "1", "2", "3", ...)
     * @param levelName Nombre del nivel.
     * @param starCount Cantidad de estrellas obtenidas por el usuario en este nivel.
     */
    @SuppressLint("SetTextI18n")
    private void createLevelButton(String levelId, String levelName, int starCount) {
        // Inflar el layout personalizado para el nivel (definido en level_item.xml)
        LayoutInflater inflater = LayoutInflater.from(this);
        View levelView = inflater.inflate(R.layout.level_item, linearLayoutLevels, false);

        // Obtener las referencias a las vistas del layout inflado
        TextView tvLevelNumber = levelView.findViewById(R.id.tvLevelNumber);
        TextView tvLevelName = levelView.findViewById(R.id.tvLevelName);
        ImageView ivLockOrStars = levelView.findViewById(R.id.ivLockOrStars);
        TextView tvStarsCount = levelView.findViewById(R.id.tvStarsCount);

        // Convertir el levelId a número
        int levelNumber = Integer.parseInt(levelId);
        // Mostrar el número de nivel (por ejemplo, "Nivel 1")
        tvLevelNumber.setText("Nivel " + levelId);
        // Mostrar el nombre del nivel
        tvLevelName.setText(levelName);

        // Caso especial: Nivel con nombre "Proximamente"
        if (levelName != null && levelName.equalsIgnoreCase("Proximamente")) {
            // Mostrar el candado animado
            ivLockOrStars.setVisibility(View.VISIBLE);
            showLockGif(ivLockOrStars);
            // Ocultar la cuenta de estrellas
            tvStarsCount.setVisibility(View.GONE);

            // Si el nivel "Proximamente" coincide con el nivel actual del usuario,
            // asignar un OnClickListener para mostrar una ventana de alerta
            if (levelNumber == userCurrentLevel) {
                levelView.setAlpha(1.0f); // Se muestra como habilitado
                levelView.setOnClickListener(v -> {
                    // Mostrar la ventana de alerta para "Próximamente"
                    overlayView.showProximamenteWindow();
                });
            } else {
                // Si no coincide con el nivel actual, mostrarlo como bloqueado y sin listener
                levelView.setAlpha(0.5f);
                levelView.setOnClickListener(null);
            }
        } else {
            // Si el nivel está bloqueado (nivel superior al actual)
            if (levelNumber > userCurrentLevel) {
                ivLockOrStars.setVisibility(View.VISIBLE);
                showLockGif(ivLockOrStars);
                tvStarsCount.setVisibility(View.GONE);
                levelView.setAlpha(0.5f);
                levelView.setOnClickListener(null);
            } else {
                // Nivel desbloqueado: ocultar el candado y mostrar las estrellas obtenidas
                ivLockOrStars.setVisibility(View.GONE);
                tvStarsCount.setVisibility(View.VISIBLE);

                // Configurar el drawable de la estrella
                Drawable starDrawable = ContextCompat.getDrawable(this, R.drawable.star);
                if (starDrawable != null) {
                    int starSize = (int) (32 * getResources().getDisplayMetrics().density);
                    starDrawable.setBounds(0, 0, starSize, starSize);
                    tvStarsCount.setCompoundDrawables(starDrawable, null, null, null);
                }

                // Mostrar la cantidad de estrellas en formato "xN" (por ejemplo, "x3")
                tvStarsCount.setText("x" + starCount);
                tvStarsCount.setCompoundDrawablePadding(8);

                levelView.setAlpha(1.0f);
                // Al hacer clic en un nivel desbloqueado, iniciar la actividad del juego correspondiente
                levelView.setOnClickListener(v -> {
                    Intent intent = new Intent(LevelSelectionActivity.this, GameActivity.class);
                    intent.putExtra("level", levelNumber);
                    intent.putExtra("userCurrentLevel", userCurrentLevel);
                    startActivity(intent);
                });
            }
        }

        // Agregar la vista del nivel al contenedor de niveles
        linearLayoutLevels.addView(levelView);
    }

    /**
     * Maneja la pulsación del botón "Atrás".
     * Finaliza la actividad y regresa a la pantalla anterior.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}