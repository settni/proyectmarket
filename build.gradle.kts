// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Definición del plugin de Android con la versión 8.1.3 (Para resolver el conflicto 8.13.0)
    id("com.android.application") version "8.1.3" apply false

    // Definición del plugin de Firebase/Google Services
    id("com.google.gms.google-services") version "4.4.1" apply false
}