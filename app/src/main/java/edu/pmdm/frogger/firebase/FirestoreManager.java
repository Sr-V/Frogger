package edu.pmdm.frogger.firebase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;

/**
 * Clase que gestiona las operaciones de Firestore para la aplicación Frogger.
 * <p>
 * Permite:
 * <ul>
 *   <li>Crear o actualizar usuarios en la colección "users".</li>
 *   <li>Leer datos de niveles desde la colección "levels".</li>
 *   <li>Actualizar los campos currentLevel y score del usuario.</li>
 * </ul>
 * </p>
 */
public class FirestoreManager {

    // Constantes para los nombres de las colecciones
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_LEVELS = "levels";

    // Instancia de FirebaseFirestore para acceder a la base de datos
    private final FirebaseFirestore db;
    // Instancia singleton de FirestoreManager
    private static FirestoreManager instance;

    /**
     * Constructor privado para implementar el patrón singleton.
     * Obtiene la instancia de Firestore.
     */
    private FirestoreManager() {
        // Obtiene la instancia de FirebaseFirestore
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Método para obtener la instancia única de FirestoreManager.
     *
     * @return Instancia única de FirestoreManager.
     */
    public static FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }

    /* ===== OPERACIONES CON USUARIOS ===== */

    /**
     * Crea o actualiza un documento de usuario identificado por uid.
     * <p>
     * Se espera que en userData se incluyan los campos:
     * <ul>
     *   <li>currentLevel: nivel actual (por defecto 1 al crearse).</li>
     *   <li>displayName: nombre del usuario logueado.</li>
     *   <li>email: correo del usuario logueado.</li>
     *   <li>score: puntuación (por defecto 0 al crearse).</li>
     * </ul>
     * </p>
     *
     * @param uid      Identificador único del usuario.
     * @param userData Mapa con los datos del usuario.
     * @return Tarea asíncrona que indica el resultado de la operación.
     */
    public Task<Void> createOrUpdateUser(String uid, Map<String, Object> userData) {
        // Referencia al documento del usuario en la colección "users"
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(uid);
        // Crea o actualiza el documento con los datos proporcionados
        return userRef.set(userData);
    }

    /**
     * Lee un documento de usuario mediante su uid.
     *
     * @param uid      Identificador único del usuario.
     * @param listener Listener que manejará el resultado de la lectura.
     */
    public void getUser(String uid, OnCompleteListener<DocumentSnapshot> listener) {
        // Referencia al documento del usuario en la colección "users"
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(uid);
        // Obtiene el documento y agrega el listener para manejar el resultado
        userRef.get().addOnCompleteListener(listener);
    }

    /**
     * Obtiene todos los usuarios en la colección "users".
     *
     * @param listener Listener que manejará el resultado de la consulta.
     */
    public void getAllUsers(OnCompleteListener<QuerySnapshot> listener){
        db.collection(COLLECTION_USERS)
                .get()
                .addOnCompleteListener(listener);
    }

    /**
     * Actualiza campos específicos de un usuario.
     * <p>
     * Puede ser utilizado para actualizar campos como currentLevel o score.
     * </p>
     *
     * @param uid     Identificador único del usuario.
     * @param updates Mapa con los campos a actualizar y sus nuevos valores.
     * @return Tarea asíncrona que indica el resultado de la operación.
     */
    public Task<Void> updateUserFields(String uid, Map<String, Object> updates) {
        // Referencia al documento del usuario en la colección "users"
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(uid);
        // Actualiza los campos específicos del documento
        return userRef.update(updates);
    }

    /* ===== OPERACIONES CON NIVELES (SOLO LECTURA) ===== */

    /**
     * Obtiene la subcolección "maps" del usuario, que contiene información de cada nivel.
     *
     * @param uid      Identificador único del usuario.
     * @param listener Listener que manejará el resultado de la consulta.
     */
    public void getUserMaps(String uid, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(COLLECTION_USERS)
                .document(uid)
                .collection("maps")
                .get()
                .addOnCompleteListener(listener);
    }

    /**
     * Obtiene todos los documentos de la colección "levels".
     *
     * @param listener Listener que manejará el resultado de la consulta.
     */
    public void getAllLevels(OnCompleteListener<QuerySnapshot> listener) {
        db.collection(COLLECTION_LEVELS)
                .get()
                .addOnCompleteListener(listener);
    }

    /**
     * Crea o actualiza un documento en la subcolección "maps" de un usuario.
     * <p>
     * Este método se utiliza para crear o actualizar la información de cada nivel (mapa)
     * en la subcolección "maps" dentro del documento del usuario.
     * </p>
     *
     * @param uid       Identificador único del usuario.
     * @param levelId   Identificador del nivel.
     * @param levelData Mapa con los datos del nivel, por ejemplo, "name" y "stars".
     * @return Tarea asíncrona que indica el resultado de la operación.
     */
    public Task<Void> createOrUpdateUserMap(String uid, String levelId, Map<String, Object> levelData) {
        // Referencia al documento del nivel en la subcolección "maps" del usuario
        DocumentReference mapRef = db.collection(COLLECTION_USERS)
                .document(uid)
                .collection("maps")
                .document(levelId);
        // Usamos merge para que solo se actualicen los campos indicados y se conserven los demás (como "name")
        return mapRef.set(levelData, SetOptions.merge());
    }
}