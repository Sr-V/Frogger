package edu.pmdm.frogger.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.firebase.FirebaseAuthManager;
import edu.pmdm.frogger.firebase.FirestoreManager;

public class LevelSelectionActivity extends AppCompatActivity {

    private static final String TAG = "LevelSelectionActivity";

    // Gestores de Firestore y Auth
    private FirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;

    // LinearLayout donde agregaremos los botones de nivel
    private LinearLayout linearLayoutLevels;

    // Guardaremos el currentLevel del usuario (o el máximo desbloqueado)
    private int userCurrentLevel = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_level_selection);

        // Ajustar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializamos FirestoreManager y AuthManager
        firestoreManager = FirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance(this);

        // Referencia al contenedor de niveles
        linearLayoutLevels = findViewById(R.id.linearLayoutLevels);

        // 1) Obtener el nivel actual del usuario y luego cargar la lista de niveles
        getUserCurrentLevel();

        Button btnBackToMain = findViewById(R.id.btnBackToMain);
        btnBackToMain.setOnClickListener(v -> {
            // Vuelve a MainActivity
            Intent intent = new Intent(LevelSelectionActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Obtiene el documento del usuario para averiguar su currentLevel
     * y luego carga la lista de niveles.
     */
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
            // Independientemente de si hay error o no, intenta cargar los niveles
            loadAllLevels();
        });
    }

    /**
     * Obtiene todos los niveles de Firestore y crea botones
     * bloqueados o desbloqueados según el currentLevel del usuario.
     */
    private void loadAllLevels() {
        firestoreManager.getAllLevels(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    // Recorremos los documentos de la colección "levels"
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        // El ID del documento es el "number" del nivel (1, 2, 3, etc.)
                        String levelId = doc.getId();
                        String levelName = doc.getString("name");

                        // Crear un botón por cada nivel
                        createLevelButton(levelId, levelName);
                    }
                }
            } else {
                Log.e(TAG, "Error al cargar niveles", task.getException());
            }
        });
    }

    /**
     * Crea un botón para el nivel dado y lo añade al LinearLayout.
     * Marca como bloqueado si el levelId > userCurrentLevel.
     * Si el nombre del nivel es "Proximamente", se mostrará un AlertDialog informando que aún no se ha desarrollado.
     */
    private void createLevelButton(String levelId, String levelName) {
        Button levelButton = new Button(this);

        // Ajustes visuales
        levelButton.setText("Nivel " + levelId + ": " + levelName);
        levelButton.setTextColor(getResources().getColor(R.color.lime_green, null));
        levelButton.setBackgroundTintList(getResources().getColorStateList(R.color.btn_background, null));
        levelButton.setTextSize(12);
        levelButton.setAllCaps(false);

        int levelNumber = Integer.parseInt(levelId);

        // Si el nombre es "Proximamente", se considerará que aún no está desarrollado
        if (levelName != null && levelName.equalsIgnoreCase("Proximamente")) {
            levelButton.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Nivel no disponible")
                        .setMessage("Este nivel aún no se ha desarrollado.")
                        .setPositiveButton("Aceptar", null)
                        .setCancelable(false)
                        .show();
            });
            // Aunque esté desbloqueado, no se lanza GameActivity
            levelButton.setEnabled(true);
            levelButton.setAlpha(1.0f);
        } else {
            // Si el nivel está bloqueado (número mayor que userCurrentLevel)
            if (levelNumber > userCurrentLevel) {
                levelButton.setEnabled(false);
                levelButton.setAlpha(0.5f);
            } else {
                // Desbloqueado: agregar clickListener para ir al juego
                levelButton.setEnabled(true);
                levelButton.setAlpha(1.0f);
                levelButton.setOnClickListener(v -> {
                    // Enviamos el nivel de este botón (levelNumber) a GameActivity
                    Intent intent = new Intent(LevelSelectionActivity.this, GameActivity.class);
                    intent.putExtra("level", levelNumber);
                    // También se pasa el userCurrentLevel para que GameActivity pueda compararlos
                    intent.putExtra("userCurrentLevel", userCurrentLevel);
                    startActivity(intent);
                });
            }
        }

        // Añadir el botón al contenedor
        linearLayoutLevels.addView(levelButton);
    }
}