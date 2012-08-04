package org.referencia.model.jar;

/**
 * Created with IntelliJ IDEA.
 * User: nenopera
 * Date: 31/07/12
 * Time: 20:09
 * To change this template use File | Settings | File Templates.
 */

/*
 * The contents of this file are subject to the Sapient Public License
 * Version 1.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://carbon.sf.net/License.html.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is The Carbon Component Framework.
 *
 * The Initial Developer of the Original Code is Sapient Corporation
 *
 * Copyright (C) 2003 Sapient Corporation. All Rights Reserved.
 */


import org.referencia.model.JarUtils;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * An output stream that is used by JarUtils2 to write entries to a jar.
 * This implementation uses a ByteArrayOutputStream to buffer the output
 * until the stream is closed.  When the stream is closed, the output is written
 * to the jar.
 * <p/>
 * Copyright 2002 Sapient
 *
 * @author Douglas Voet, April 2002
 * @version $Revision: 1.9 $($Author: dvoet $ / $Date: 2003/05/05 21:21:23 $)
 * @since carbon 1.0
 */
public class JarEntryOutputStream extends ByteArrayOutputStream {

    private String jarEntryName;
    private JarFile jar;

    /**
     * Constructor
     *
     * @param jar          the JarFile that this instance will write to
     * @param jarEntryName the name of the entry to be written
     */
    public JarEntryOutputStream(JarFile jar, String jarEntryName) {
        super();

        this.jarEntryName = jarEntryName;
        this.jar = jar;
    }

    /**
     * Closes the stream and writes entry to the jar
     */
    public void close() throws IOException {
        writeToJar();
        super.close();
    }

    /**
     * Writes the entry to a the jar file.  This is done by creating a
     * temporary jar file, copying the contents of the existing jar to the
     * temp jar, skipping the entry named by jarEntryName if it exists.
     * Then, if the stream was written to, then contents are written as a
     * new entry.  Last, a callback is made to the JarUtils2 to
     * swap the temp jar in for the old jar.
     */
    private void writeToJar() throws IOException {

        File jarDir = new File(jar.getName()).getParentFile();
        // create new jar
        File newJarFile = File.createTempFile("config", ".jar", jarDir);
        newJarFile.deleteOnExit();
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(newJarFile));

        try {
            Enumeration entries = jar.entries();

            // copy all current entries into the new jar
            while (entries.hasMoreElements()) {
                JarEntry nextEntry = (JarEntry) entries.nextElement();
                // skip the entry named jarEntryName
                if (!jarEntryName.equals(nextEntry.getName())) {
                    // the next 3 lines of code are a work around for
                    // bug 4682202 in the java.sun.com bug parade, see:
                    // http://developer.java.sun.com/developer/bugParade/bugs/4682202.html
                    JarEntry entryCopy = new JarEntry(nextEntry);
                    entryCopy.setCompressedSize(-1);
                    jarOutputStream.putNextEntry(entryCopy);

                    InputStream intputStream = jar.getInputStream(nextEntry);
                    // write the data
                    for (int data = intputStream.read(); data != -1; data = intputStream.read()) {
                        jarOutputStream.write(data);
                    }
                }
            }

            // write the new or modified entry to the jar
            if (size() > 0) {
                jarOutputStream.putNextEntry(new JarEntry(jarEntryName));
                jarOutputStream.write(buf, 0, size());
                jarOutputStream.closeEntry();
            }
        } finally {
            try {
                jarOutputStream.close();
            } catch (IOException ioe) {
            }
        }

        // swap the jar
        JarUtils.swapJars(jar, newJarFile);
    }
}



