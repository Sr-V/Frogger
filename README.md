# Frogger App

¡Bienvenido/a a **Frogger App**! Este es un proyecto Android (Java) que recrea el clásico juego de Frogger, incorporando nuevas mecánicas, niveles temáticos y funciones avanzadas. La aplicación incluye animaciones, sonido, autenticación y almacenamiento en la nube a través de Firebase.

---

## Tabla de Contenidos
1. [Descripción](#descripción)
2. [Características Principales](#características-principales)
3. [Tecnologías Utilizadas](#tecnologías-utilizadas)
4. [Requisitos](#requisitos)

---

## Descripción
**Frogger App** es un juego en el que controlarás una rana que debe sortear diversos obstáculos para llegar a zonas seguras. En esta versión se implementan:
- **Animación y Sonido:** La rana y otros elementos cuentan con animaciones automáticas y se reproducen efectos de audio y música ambiental.
- **Bucle de Juego Real-Time:** Uso de un bucle de juego con temporización basada en FPS, que actualiza la lógica y renderiza la acción en un canvas.
- **Firebase:** Integración con Firebase Auth para el inicio de sesión (con Google) y Firestore para el almacenamiento en la nube del progreso del usuario.
- **Evolución de la Dificultad:** Niveles temáticos con distintos patrones, obstáculos y condiciones de victoria/derrota.

---

## Características Principales

### Animación Automática y Efectos Visuales
- Se utilizan *AnimationDrawable* para las animaciones de la rana (*PlayerFrog*) y para otras transiciones en el juego.
- Animaciones automáticas previas a la acción y durante eventos (muerte, movimiento, etc.).

### Reproducción de Música y Sonidos
- La clase **GameAudioManager** administra la música de fondo y efectos de sonido (movimientos, muerte, sonidos ambientales y notificaciones de eventos como la recogida de la llave).

### Bucle de Videojuego
- **BucleJuego:** Extiende de `Thread` y controla la temporización mediante una tasa de FPS (30 FPS) para actualizar y renderizar la acción.
- **GameEngine y Juego:** Implementan los métodos `update()` y `draw(Canvas canvas)`, permitiendo la sincronización de la lógica y el dibujo en tiempo real.

### Detección de Colisiones
- **CollisionManager:** Gestiona la detección de colisiones utilizando bounding boxes precisos de objetos que heredan de *FroggerObject*.

### Adaptación a Diferentes Dispositivos
- Métodos de escalado y configuración de posiciones (como `configurePositions()` y `configureScale()`) en clases como *PlayerFrog*, *Obstacle* y *GameEngine* aseguran una visualización correcta en distintos tamaños y densidades de pantalla.

### Condiciones de Victoria y Derrota
- **GameEngine:** Define condiciones claras para ganar (cuando la rana alcanza la última fila) o perder (por colisión o por agotamiento del tiempo), mostrando ventanas finales y reiniciando o permitiendo la selección de niveles.
- **AlertsOverlayView:** Implementa un overlay retro para mostrar mensajes y ventanas de confirmación en el juego.

### Evolución de la Dificultad
- La aplicación cuenta con múltiples niveles temáticos con distintas configuraciones en *GameEngine*, *Path* y *LevelSelectionActivity*, variando en velocidad, tiempo límite y patrones de obstáculos.

### Integración con Firebase
- **FirebaseAuthManager:** Permite iniciar sesión con Google.
- **FirestoreManager:** Se encarga del registro y actualización de usuarios, niveles, mapas y logros en la base de datos en tiempo real.

### Interfaz y Navegación
- Las actividades principales incluyen:
   - **MainActivity:** Menú principal y navegación.
   - **LoginActivity:** Gestión del inicio de sesión.
   - **LevelSelectionActivity:** Selección de niveles.
   - **LeaderboardActivity:** Visualización de clasificaciones.
   - **SettingsActivity:** Configuración de audio y otros parámetros.
   - **GameActivity:** Pantalla de juego que integra el motor y la lógica.

---

## Tecnologías Utilizadas
- **Java:** Lenguaje principal de la aplicación.
- **Android Studio:** Entorno de desarrollo (IDE).
- **Firebase Auth:** Para el inicio de sesión con Google.
- **Cloud Firestore:** Base de datos NoSQL para almacenar la información de usuarios y niveles.
- **SurfaceView:** Utilizado en la clase *Juego* para renderizar la animación en tiempo real.
- **MediaPlayer:** Reproducción de audio y efectos.

---

## Requisitos
1. **Android Studio** (versión recomendada >= 4.0).
2. **SDK de Android** mínimo 21 (Android 5.0) o superior.
3. **Cuenta de Firebase** y proyecto configurado para Auth y Firestore.
4. **Gradle** (manejado automáticamente con Android Studio).
5. **Acceso a Internet** para la autenticación y sincronización en tiempo real con Firebase.
