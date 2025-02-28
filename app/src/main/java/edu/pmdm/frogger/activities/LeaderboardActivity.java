package edu.pmdm.frogger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
        TextView userView = new TextView(this);
        userView.setText(position + ". " + displayName + " - " + totalStars + " estrellas");
        userView.setTextSize(18);
        userView.setTextColor(getResources().getColor(R.color.lime_green, null));
        userView.setPadding(0, 8, 0, 8);

        // Añadir el TextView al contenedor
        linearLayoutLeaderboard.addView(userView);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}