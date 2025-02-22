package edu.pmdm.frogger.firebase;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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

    private final FirebaseFirestore db;
    private static FirestoreManager instance;

    /**
     * Constructor privado para implementar el patrón singleton.
     */
    private FirestoreManager() {
        // Obtiene la instancia de Firestore
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Método para obtener la instancia única de FirestoreManager.
     *
     * @return instancia única de FirestoreManager.
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
     * Lee un documento de nivel.
     * <p>
     * El documento se identifica por el número del nivel (por ejemplo, "1", "2", "3")
     * y contiene los campos "name" y "theme".
     * </p>
     *
     * @param levelId  Identificador del nivel.
     * @param listener Listener que manejará el resultado de la lectura.
     */
    public void getLevel(String levelId, OnCompleteListener<DocumentSnapshot> listener) {
        // Referencia al documento del nivel en la colección "levels"
        DocumentReference levelRef = db.collection(COLLECTION_LEVELS).document(levelId);
        // Obtiene el documento y agrega el listener para manejar el resultado
        levelRef.get().addOnCompleteListener(listener);
    }
}