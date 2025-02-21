# Frogger App

¡Bienvenido/a a **Frogger App**! Este es un proyecto de Android (Java) que recrea el clásico juego de Frogger con nuevos niveles temáticos, desbloqueo de personajes, logros y respaldo de datos en Firebase.

---

## Tabla de Contenidos
1. [Descripción](#descripción)
2. [Características Principales](#características-principales)
3. [Tecnologías Utilizadas](#tecnologías-utilizadas)
4. [Requisitos](#requisitos)
5. [Configuración del Proyecto](#configuración-del-proyecto)
6. [Estructura del Proyecto](#estructura-del-proyecto)
7. [Uso de Git y Flujo de Trabajo](#uso-de-git-y-flujo-de-trabajo)
8. [Contribuir](#contribuir)
9. [Autores](#autores)
10. [Licencia](#licencia)

---

## Descripción
**Frogger App** es un juego donde controlarás una rana que debe cruzar diversos obstáculos (carros, ríos, troncos, etc.) para llegar a un punto seguro. En esta versión, se incluyen:
- Múltiples niveles con distintas temáticas.
- Personajes desbloqueables.
- Logros que se obtienen cumpliendo retos específicos.
- Integración con Firebase para autenticación y guardado de progreso en Firestore.

---

## Características Principales
- **Juego estilo Frogger**: Movimientos simples y mecánicas de colisión.
- **Niveles temáticos**: Variedad de escenarios (ciudad, bosque, espacio, etc.) con distintas dificultades.
- **Desbloqueo de personajes**: El jugador puede ganar o comprar diferentes skins de la rana.
- **Logros**: Sistema de desafíos para mantener la motivación del jugador.
- **Autenticación con Google**: Utilizamos Firebase Auth para que los jugadores se registren o inicien sesión fácilmente.
- **Guardado en Firestore**: Progreso, logros y personajes desbloqueados se almacenan en tiempo real en la nube.

---

## Tecnologías Utilizadas
- **Java**: Lenguaje principal de la aplicación.
- **Android Studio**: Entorno de desarrollo (IDE).
- **Firebase Auth**: Para el inicio de sesión con Google.
- **Cloud Firestore**: Base de datos NoSQL para almacenar la información de usuarios, logros, personajes y niveles.

---

## Requisitos
1. **Android Studio** (versión recomendada >= 4.0).
2. **SDK de Android** mínimo 21 (Android 5.0) o superior.
3. **Cuenta de Firebase** y proyecto configurado para Auth y Firestore.
4. **Gradle** (se maneja automáticamente con Android Studio).
5. **Acceso a Internet** para la autenticación en tiempo real con Firebase.

---

## Configuración del Proyecto

1. **Clonar o Descargar** este repositorio:
   ```bash
   git clone https://github.com/tu-usuario/frogger-app.git
   
