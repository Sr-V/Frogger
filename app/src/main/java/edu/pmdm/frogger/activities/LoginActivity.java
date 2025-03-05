package edu.pmdm.frogger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.firebase.FirebaseAuthManager;

/**
 * {@code LoginActivity} es la actividad encargada de gestionar el proceso de autenticación
 * utilizando Google Sign-In. Si el usuario ya está autenticado, se redirige a la pantalla
 * principal; de lo contrario, se inicia el proceso de inicio de sesión.
 */
public class LoginActivity extends AppCompatActivity {

    // Tag para depuración de log
    private static final String TAG = "FroggerLogin";
    // Gestor de autenticación de Firebase
    private FirebaseAuthManager authManager;
    // Launcher para el proceso de Google Sign-In
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    /**
     * Método del ciclo de vida que se invoca al crear la actividad.
     * Configura la interfaz de usuario, ajusta los insets de los system bars y
     * registra el ActivityResultLauncher para el proceso de Google Sign-In.
     *
     * @param savedInstanceState Estado previo de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Habilitar modo EdgeToEdge para utilizar toda la pantalla
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Ajustar el padding del layout principal según los insets del sistema (barras de estado y navegación)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar el gestor de autenticación de Firebase
        authManager = FirebaseAuthManager.getInstance(this);

        // Si ya hay un usuario logueado, se redirige inmediatamente a la pantalla principal
        if (authManager.getCurrentUser() != null) {
            goToMainMenu();
            return;
        }

        // Registrar el ActivityResultLauncher para manejar el resultado del proceso de Google Sign-In
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Si el resultado es OK, procesar la información devuelta
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        // Obtener la cuenta de Google a partir del intent devuelto
                        Task<GoogleSignInAccount> task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            // Intentar obtener la cuenta; si falla, se lanza ApiException
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                // Autenticar en Firebase con el token obtenido de Google
                                authManager.firebaseAuthWithGoogle(account.getIdToken(), taskResult -> {
                                    if (taskResult.isSuccessful()) {
                                        Log.d(TAG, "signInWithCredential: success");
                                        goToMainMenu();
                                    } else {
                                        Log.w(TAG, "signInWithCredential: failure", taskResult.getException());
                                    }
                                });
                            }
                        } catch (ApiException e) {
                            // Manejar el error de inicio de sesión con Google
                            Log.w(TAG, "Google sign in failed", e);
                        }
                    }
                }
        );

        // Configurar el botón de Google Sign-In para iniciar el proceso de autenticación
        findViewById(R.id.btnGoogleSignIn).setOnClickListener(v -> signInWithGoogle());
    }

    /**
     * Inicia el proceso de Google Sign-In lanzando el intent correspondiente.
     */
    private void signInWithGoogle() {
        Intent signInIntent = authManager.getGoogleSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    /**
     * Redirige al usuario a la pantalla principal (MainActivity) y finaliza la actividad actual.
     */
    private void goToMainMenu() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}