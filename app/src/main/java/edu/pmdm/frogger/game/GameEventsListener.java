package edu.pmdm.frogger.game;

/**
 * {@code GameEventsListener} define los métodos que deben implementar las clases que
 * deseen reaccionar ante los eventos importantes del juego, tales como victoria, derrota
 * o bloqueos de botones durante animaciones.
 */
public interface GameEventsListener {

    /**
     * Se llama cuando la rana alcanza la última fila del juego, lo que indica una victoria.
     *
     * @param shouldIncrementLevel Si es {@code true}, se debe incrementar el currentLevel en Firebase.
     */
    void onGameWon(boolean shouldIncrementLevel);

    /**
     * Se llama cuando el usuario se queda sin vidas, indicando que el juego ha terminado.
     */
    void onGameLost();

    /**
     * Permite bloquear o desbloquear los botones de control del juego,
     * por ejemplo, cuando la rana muere y está en proceso de reaparecer.
     *
     * @param blocked {@code true} para bloquear los botones, {@code false} para desbloquearlos.
     */
    void onButtonsBlocked(boolean blocked);
}