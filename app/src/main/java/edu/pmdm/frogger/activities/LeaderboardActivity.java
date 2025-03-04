package edu.pmdm.frogger.activities;

import static android.view.View.TEXT_ALIGNMENT_GRAVITY;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.firebase.FirebaseAuthManager;
import edu.pmdm.frogger.firebase.FirestoreManager;

public class LeaderboardActivity extends AppCompatActivity {

    private FirestoreManager firestoreManager;
    private FirebaseAuthManager authManager;
    private LinearLayout linearLayoutLeaderboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leaderboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firestoreManager = FirestoreManager.getInstance();
        authManager = FirebaseAuthManager.getInstance(this);

        // Referencia al contenedor de la lista de usuarios
        linearLayoutLeaderboard = findViewById(R.id.linearLayoutLeaderboard);

        // Cargar y mostrar el top 10 de usuarios
        loadTop10Users();

        findViewById(R.id.btnBackToMain).setOnClickListener(v ->{
            finish();
        });
    }

    private void loadTop10Users() {
        firestoreManager.getAllUsers(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    // Obtener la lista de usuarios y ordenarla por estrellas
                    List<DocumentSnapshot> users = querySnapshot.getDocuments();
                    users.sort((doc1, doc2) -> {
                        Long stars1 = doc1.getLong("totalStars");
                        Long stars2 = doc2.getLong("totalStars");
                        return stars2.compareTo(stars1); // Orden descendente
                    });

                    // Mostrar solo los primeros 10 usuarios
                    int count = Math.min(users.size(), 10);
                    for (int i = 0; i < count; i++) {
                        DocumentSnapshot user = users.get(i);
                        String displayName = user.getString("displayName");
                        Long totalStars = user.getLong("totalStars");

                        // Crear un TextView para mostrar el usuario
                        createUserView(i + 1, displayName, totalStars);
                    }
                }
            } else {
                Log.e("LeaderboardActivity", "Error al cargar usuarios", task.getException());
            }
        });
    }

    private void createUserView(int position, String displayName, Long totalStars) {
        // Asegúrate de que displayName no sea null o vacío
        if (displayName == null) {
            displayName = "Usuario Desconocido";
        }

        // Dividir por espacios
        String[] parts = displayName.split("\\s+"); // \\s+ para cualquier espacio en blanco

        // Si tiene 2 o más palabras, solo usamos las 2 primeras
        if (parts.length >= 2) {
            displayName = parts[0] + " " + parts[1];
        }

        // Inflamos la tarjeta, similar a level_item.xml
        View itemView = getLayoutInflater().inflate(R.layout.leaderboard_item, linearLayoutLeaderboard, false);

        // Referencias a los TextView dentro de la tarjeta
        TextView tvPositionName = itemView.findViewById(R.id.tvPositionName);
        TextView tvStarsCount = itemView.findViewById(R.id.tvStarsCount);

        // Mostramos "1. NombreUsuario" a la izquierda
        tvPositionName.setText(position + ". " + displayName);

        // Ajustamos el drawable de la estrella (32dp) y lo ponemos a la izquierda del texto "xN"
        Drawable starDrawable = ContextCompat.getDrawable(this, R.drawable.star);
        if (starDrawable != null) {
            int starSize = (int) (32 * getResources().getDisplayMetrics().density); // 32dp
            starDrawable.setBounds(0, 0, starSize, starSize);
            tvStarsCount.setCompoundDrawables(starDrawable, null, null, null);
            tvStarsCount.setCompoundDrawablePadding(8);
        }

        // Ahora mostramos "xN" (por ejemplo, "x3")
        tvStarsCount.setText("x" + totalStars);

        // Agregamos la tarjeta al contenedor
        linearLayoutLeaderboard.addView(itemView);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}