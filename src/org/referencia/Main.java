package org.referencia;

import org.referencia.config.Config;
import org.referencia.ui.FileVisor;

/**
 * Clase principal donde cargar la configuración
 * User: alberto
 * Date: 26/11/11
 * Time: 11:30
 */
public class Main {

    public static void main(String[] args) {
        Config.getInstance().init(args);

        new FileVisor(System.getProperty("user.home"));
    }
}
