package edu.pmdm.frogger.game;

public interface GameEventsListener {
    /**
     * Llamado cuando la rana llega a la última fila (victoria).
     * @param shouldIncrementLevel true => subir currentLevel en Firebase.
     */
    void onGameWon(boolean shouldIncrementLevel);

    /**
     * Llamado cuando el usuario se queda sin vidas.
     */
    void onGameLost();

    /**
     * Bloquear o desbloquear botones cuando la rana muere y está reapareciendo.
     */
    void onButtonsBlocked(boolean blocked);
}

