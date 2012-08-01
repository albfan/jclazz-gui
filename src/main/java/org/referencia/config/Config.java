package org.referencia.config;

import java.io.File;
import java.util.Comparator;
import java.util.ResourceBundle;

/**
* Gestor de la Configuracion
* User: alberto
* Date: 26/11/11
* Time: 11:56
*/
public class Config {

    // instance fields
    private boolean ignorecase;
    private boolean showHiddenFiles;

    private Comparator<File> fileSorter;

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

    public void setShowHiddenFiles(boolean showHiddenFiles) {
        this.showHiddenFiles = showHiddenFiles;
    }

    public boolean isShowHiddenFiles() {
        return showHiddenFiles;
    }

    public boolean isIgnorecase() {
        return ignorecase;
    }

    public void setIgnorecase(boolean ignorecase) {
        this.ignorecase = ignorecase;
        if (ignorecase) {
            fileSorter = new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getAbsolutePath().compareToIgnoreCase(o2.getAbsolutePath());
                }
            };
        } else {
            fileSorter = null;
        }
    }

    public Comparator<File> getFileSorter() {
        return fileSorter;
    }
}
