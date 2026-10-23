package com.northpeak.hrms;

/**
 * Plain main class used by the executable (shaded) jar.
 * JavaFX requires the class passed to the JVM not to extend Application
 * when the JavaFX modules are loaded from the classpath.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
