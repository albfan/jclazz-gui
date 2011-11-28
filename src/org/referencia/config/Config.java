package org.referencia.config;

import java.util.ResourceBundle;

/**
* Gestor de la Configuracion
* User: alberto
* Date: 26/11/11
* Time: 11:56
*/
public class Config {

    private ResourceBundle messages;

    static Config myConfig;

    private Config() { }

    public static Config getInstance() {
        if (myConfig == null) {
            myConfig = new Config();
        }
        return myConfig;
    }

    /**
     * Inicia la configuracion
     * @param args
     */
    public void init(String[] args) {

        //TODO: Parser para los args

        System.setProperty("file.encoding", "UTF8");
//        Locale.setDefault(Locale.CHINA);

        messages = ResourceBundle.getBundle("org.referencia.config.MessagesBundle");
    }


    /**
     * Devuelve el texto internacionalizado
     * @param message
     * @return
     */
    public String getMessage(String message) {
        return messages.getString(message);
    }

}
