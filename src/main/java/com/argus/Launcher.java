package com.argus;

/**
 * Launcher secondario necessario per JavaFX quando si avvia da Fat JAR
 * senza moduli o argomenti VM complessi.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
