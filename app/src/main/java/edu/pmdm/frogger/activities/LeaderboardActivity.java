package edu.pmdm.frogger.activities;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

import java.util.List;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.firebase.FirebaseAuthManager;
import edu.pmdm.frogger.firebase.FirestoreManager;

/**
 * {@code LeaderboardActivity} muestra el ranking de usuarios basado en sus estrellas totales.
 * Esta actividad carga el top 10 de usuarios desde Firebase, los ordena de manera descendente
 * según el número de estrellas, y los muestra en una lista.
 */
public class LeaderboardActivity extends AppCompatActivity {

    // Instancia del FirestoreManager para gestionar las consultas a Firebase
    private FirestoreManager firestoreManager;
    // Contenedor del layout donde se añaden los items del leaderboard
    private LinearLayout linearLayoutLeaderboard;

    /**
     * Método del ciclo de vida que se invoca al crear la actividad.
     * Configura el modo EdgeToEdge, aplica los insets, inicializa Firebase y carga el top 10 de usuarios.
     *
     * @param savedInstanceState Estado previo de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Habilitar el modo EdgeToEdge para la actividad
        EdgeToEdge.enable(this);
        // Establecer el layout de la actividad
        setContentView(R.layout.activity_leaderboard);

        // Ajustar el padding del layout principal según los insets del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Obtener la instancia de FirestoreManager
        firestoreManager = FirestoreManager.getInstance();
        // Se puede obtener el authManager si es necesario, aunque no se use en este código
        FirebaseAuthManager authManager = FirebaseAuthManager.getInstance(this);

        // Referencia al contenedor de la lista de usuarios (LinearLayout)
        linearLayoutLeaderboard = findViewById(R.id.linearLayoutLeaderboard);

        // Cargar y mostrar el top 10 de usuarios
        loadTop10Users();

        // Configurar el botón para volver a la actividad principal
        findViewById(R.id.btnBackToMain).setOnClickListener(v -> {
            finish(); // Finaliza la actividad y regresa
        });
    }

    /**
     * Carga todos los usuarios de Firebase, los ordena por su total de estrellas y muestra
     * los primeros 10 en el leaderboard.
     */
    private void loadTop10Users() {
        // Realiza la consulta a Firebase para obtener todos los usuarios
        firestoreManager.getAllUsers(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    // Obtener la lista de documentos de usuarios
                    List<DocumentSnapshot> users = querySnapshot.getDocuments();
                    // Ordenar la lista de usuarios de forma descendente por "totalStars"
                    users.sort((doc1, doc2) -> {
                        Long stars1 = doc1.getLong("totalStars");
                        Long stars2 = doc2.getLong("totalStars");
                        // Aseguramos que los valores no sean null
                        assert stars2 != null;
                        assert stars1 != null;
                        return stars2.compareTo(stars1);
                    });

                    // Mostrar solamente los primeros 10 usuarios o el total si hay menos de 10
                    int count = Math.min(users.size(), 10);
                    for (int i = 0; i < count; i++) {
                        DocumentSnapshot user = users.get(i);
                        // Obtener el nombre a mostrar y el total de estrellas
                        String displayName = user.getString("displayName");
                        Long totalStars = user.getLong("totalStars");
                        // Crear y añadir la vista para el usuario en el leaderboard
                        createUserView(i + 1, displayName, totalStars);
                    }
                }
            } else {
                // Registrar un error en el log si ocurre un fallo al cargar los usuarios
                Log.e("LeaderboardActivity", "Error al cargar usuarios", task.getException());
            }
        });
    }

    /**
     * Crea y configura la vista de un usuario para mostrar en el leaderboard.
     * Esta vista incluye la posición, el nombre y la cantidad de estrellas.
     *
     * @param position    Posición del usuario en el ranking.
     * @param displayName Nombre del usuario.
     * @param totalStars  Total de estrellas obtenidas por el usuario.
     */
    @SuppressLint("SetTextI18n")
    private void createUserView(int position, String displayName, Long totalStars) {
        // Validar que displayName no sea null; de serlo, asignar un valor por defecto.
        if (displayName == null) {
            displayName = "Usuario Desconocido";
        }

        // Dividir el nombre completo en partes separadas por espacios
        String[] parts = displayName.split("\\s+");
        // Si el nombre tiene dos o más palabras, se utilizan las dos primeras
        if (parts.length >= 2) {
            displayName = parts[0] + " " + parts[1];
        }

        // Inflar la vista del item del leaderboard (similar a level_item.xml)
        View itemView = getLayoutInflater().inflate(R.layout.leaderboard_item, linearLayoutLeaderboard, false);

        // Obtener referencias a los TextView de la tarjeta
        TextView tvPositionName = itemView.findViewById(R.id.tvPositionName);
        TextView tvStarsCount = itemView.findViewById(R.id.tvStarsCount);

        // Establecer el texto de la posición y el nombre (por ejemplo, "1. NombreUsuario")
        tvPositionName.setText(position + ". " + displayName);

        // Configurar el drawable de la estrella que se mostrará a la izquierda del texto de estrellas
        Drawable starDrawable = ContextCompat.getDrawable(this, R.drawable.star);
        if (starDrawable != null) {
            // Convertir 32dp a píxeles
            int starSize = (int) (32 * getResources().getDisplayMetrics().density);
            starDrawable.setBounds(0, 0, starSize, starSize);
            // Establecer el drawable a la izquierda del texto
            tvStarsCount.setCompoundDrawables(starDrawable, null, null, null);
            // Establecer un padding entre el drawable y el texto
            tvStarsCount.setCompoundDrawablePadding(8);
        }

        // Mostrar el total de estrellas en el formato "xN" (por ejemplo, "x3")
        tvStarsCount.setText("x" + totalStars);

        // Agregar la vista del usuario al contenedor del leaderboard
        linearLayoutLeaderboard.addView(itemView);
    }

    /**
     * Maneja la pulsación del botón "Atrás".
     * Llama al método {@code finish()} para cerrar la actividad.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}