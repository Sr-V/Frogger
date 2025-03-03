package edu.pmdm.frogger.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
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

import java.util.HashMap;
import java.util.Map;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.firebase.FirebaseAuthManager;
import edu.pmdm.frogger.firebase.FirestoreManager;
import edu.pmdm.frogger.utils.AlertsOverlayView;

public class LevelSelectionActivity extends AppCompatActivity {

    private static final String TAG = "LevelSelectionActivity";

    // Gestores de Firestore y Auth
    private FirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;

    // Contenedor para los ítems de nivel
    private LinearLayout linearLayoutLevels;

    // Nivel actual (o máximo desbloqueado) del usuario
    private int userCurrentLevel = 1;

    // Overlay para ventanas retro
    private AlertsOverlayView overlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_level_selection);

        // Ajustar los insets para los system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializamos los gestores
        firestoreManager = FirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance(this);

        // Referencia al contenedor de niveles
        linearLayoutLevels = findViewById(R.id.linearLayoutLevels);

        // Agregar el overlay para ventanas retro
        overlayView = new AlertsOverlayView(this);
        addContentView(overlayView,
                new android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
        );
        overlayView.setVisibility(View.GONE);

        // Obtenemos el nivel actual del usuario y luego cargamos la lista de niveles
        getUserCurrentLevel();

        Button btnBackToMain = findViewById(R.id.btnBackToMain);
        btnBackToMain.setOnClickListener(v -> finish());
    }

    private void getUserCurrentLevel() {
        String uid = authManager.getCurrentUser().getUid();
        firestoreManager.getUser(uid, task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    Long levelLong = document.getLong("currentLevel");
                    if (levelLong != null) {
                        userCurrentLevel = levelLong.intValue();
                    }
                }
            } else {
                Log.e(TAG, "Error al obtener datos de usuario", task.getException());
            }
            // Cargar niveles después de obtener el nivel actual
            loadAllLevels();
        });
    }

    private void loadAllLevels() {
        firestoreManager.getAllLevels(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot levelQuery = task.getResult();
                if (levelQuery != null) {
                    // Guardamos la lista de niveles
                    // (cada doc tiene un ID = "1", "2", "3", etc. y un campo "name")
                    String uid = authManager.getCurrentUser().getUid();

                    // Ahora consultamos la subcolección "maps" del usuario para obtener las estrellas
                    firestoreManager.getUserMaps(uid, mapsTask -> {
                        if (mapsTask.isSuccessful()) {
                            QuerySnapshot mapsSnapshot = mapsTask.getResult();

                            // Construimos un map <levelId, starsCount> con las estrellas de cada nivel
                            Map<String, Integer> userStarsMap = new HashMap<>();
                            if (mapsSnapshot != null) {
                                for (DocumentSnapshot mapDoc : mapsSnapshot) {
                                    Long starsLong = mapDoc.getLong("stars");
                                    int starCount = (starsLong != null) ? starsLong.intValue() : 0;
                                    userStarsMap.put(mapDoc.getId(), starCount);
                                }
                            }

                            // Ahora creamos el CardView para cada nivel
                            for (DocumentSnapshot doc : levelQuery.getDocuments()) {
                                String levelId = doc.getId();         // p. ej. "1"
                                String levelName = doc.getString("name"); // p. ej. "Talavera..."
                                // Obtenemos cuántas estrellas tiene el usuario en este nivel
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

    private void createLevelButton(String levelId, String levelName, int starCount) {
        // Inflamos el layout personalizado "level_item.xml"
        LayoutInflater inflater = LayoutInflater.from(this);
        View levelView = inflater.inflate(R.layout.level_item, linearLayoutLevels, false);

        // Referencias a las vistas
        TextView tvLevelNumber = levelView.findViewById(R.id.tvLevelNumber);
        TextView tvLevelName = levelView.findViewById(R.id.tvLevelName);
        ImageView ivLockOrStars = levelView.findViewById(R.id.ivLockOrStars);
        TextView tvStarsCount = levelView.findViewById(R.id.tvStarsCount);

        // Rellenamos datos
        int levelNumber = Integer.parseInt(levelId);
        tvLevelNumber.setText("Nivel " + levelId);
        tvLevelName.setText(levelName);

        // Caso especial "Próximamente"
        if (levelName != null && levelName.equalsIgnoreCase("Proximamente")) {
            // Mostramos el candado y ocultamos estrellas
            ivLockOrStars.setVisibility(View.VISIBLE);
            ivLockOrStars.setImageResource(R.drawable.lock);
            tvStarsCount.setVisibility(View.GONE);

            levelView.setOnClickListener(v -> overlayView.showProximamenteWindow());
            levelView.setAlpha(1.0f);

        } else {
            // Si el nivel está bloqueado (levelNumber > userCurrentLevel)
            if (levelNumber > userCurrentLevel) {
                ivLockOrStars.setVisibility(View.VISIBLE);
                ivLockOrStars.setImageResource(R.drawable.lock);
                tvStarsCount.setVisibility(View.GONE);

                levelView.setAlpha(0.5f);
                levelView.setOnClickListener(null);

            } else {
                // Nivel desbloqueado: ocultamos el candado y mostramos las estrellas
                ivLockOrStars.setVisibility(View.GONE);
                tvStarsCount.setVisibility(View.VISIBLE);

                // Ajustamos el drawable de la estrella para que sea 32dp x 32dp
                Drawable starDrawable = ContextCompat.getDrawable(this, R.drawable.star);
                if (starDrawable != null) {
                    int starSize = (int) (32 * getResources().getDisplayMetrics().density); // 32dp
                    starDrawable.setBounds(0, 0, starSize, starSize);
                    tvStarsCount.setCompoundDrawables(starDrawable, null, null, null);
                }

                // Mostramos la cantidad real de estrellas
                tvStarsCount.setText("x" + starCount);
                tvStarsCount.setCompoundDrawablePadding(8);

                levelView.setAlpha(1.0f);
                levelView.setOnClickListener(v -> {
                    // Ir al juego
                    Intent intent = new Intent(LevelSelectionActivity.this, GameActivity.class);
                    intent.putExtra("level", levelNumber);
                    intent.putExtra("userCurrentLevel", userCurrentLevel);
                    startActivity(intent);
                });
            }
        }

        // Agregamos la vista inflada al contenedor
        linearLayoutLevels.addView(levelView);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
