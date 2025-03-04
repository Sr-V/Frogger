package edu.pmdm.frogger.firebase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import edu.pmdm.frogger.R;

/**
 * {@code FirebaseAuthManager} gestiona la autenticación del usuario en Firebase utilizando Google Sign-In.
 * Esta clase se encarga de configurar y proporcionar métodos para iniciar sesión, obtener el usuario actual
 * y cerrar sesión.
 */
public class FirebaseAuthManager {

    // Instancia de FirebaseAuth para gestionar la autenticación en Firebase
    private final FirebaseAuth mAuth;
    // Cliente de Google Sign-In
    private final GoogleSignInClient mGoogleSignInClient;
    // Instancia singleton de FirebaseAuthManager
    private static FirebaseAuthManager instance;

    /**
     * Constructor privado que inicializa FirebaseAuth y configura Google Sign-In.
     *
     * @param context Contexto de la aplicación.
     */
    private FirebaseAuthManager(Context context) {
        // Obtener la instancia de FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Configuración de GoogleSignInOptions para utilizar el token del cliente web
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id)) // Solicitar token de ID
                .requestEmail() // Solicitar el correo electrónico del usuario
                .build();
        // Inicializar el cliente de Google Sign-In con la configuración especificada
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    /**
     * Retorna la instancia singleton de FirebaseAuthManager. Si aún no ha sido creada, se crea una nueva.
     *
     * @param context Contexto de la aplicación.
     * @return Instancia de FirebaseAuthManager.
     */
    public static FirebaseAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseAuthManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Devuelve el usuario actualmente autenticado en Firebase.
     *
     * @return {@code FirebaseUser} actual o {@code null} si no hay ningún usuario autenticado.
     */
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    /**
     * Devuelve el intent necesario para iniciar el proceso de Google Sign-In.
     *
     * @return Intent para iniciar Google Sign-In.
     */
    public Intent getGoogleSignInIntent() {
        return mGoogleSignInClient.getSignInIntent();
    }

    /**
     * Autentica al usuario en Firebase utilizando el token de Google.
     *
     * @param idToken  Token de identificación obtenido de Google.
     * @param listener Listener que se invoca al completar la tarea de autenticación.
     */
    public void firebaseAuthWithGoogle(String idToken, OnCompleteListener<AuthResult> listener) {
        // Crear las credenciales de autenticación con el token de Google
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        // Iniciar sesión en Firebase con las credenciales creadas y notificar al listener cuando finalice
        mAuth.signInWithCredential(credential).addOnCompleteListener(listener);
    }

    /**
     * Cierra la sesión del usuario tanto en Firebase como en Google Sign-In.
     *
     * @param activity La actividad desde la que se invoca el cierre de sesión.
     * @param listener Listener que se invoca al completar el proceso de cierre de sesión.
     */
    public void signOut(Activity activity, OnCompleteListener<Void> listener) {
        // Cerrar sesión en Firebase
        mAuth.signOut();
        // Cerrar sesión en Google Sign-In y notificar al listener cuando finalice
        mGoogleSignInClient.signOut().addOnCompleteListener(activity, listener);
    }
}