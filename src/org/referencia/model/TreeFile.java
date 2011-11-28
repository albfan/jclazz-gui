package org.referencia.model;

import java.io.File;

/**
* Created by IntelliJ IDEA.
* User: alberto
* Date: 28/11/11
* Time: 3:49
*/
public class TreeFile extends File {
    public TreeFile(File parent, String child) {
        super(parent, child);
    }

    public String toString() {
        return getName();
    }
}
