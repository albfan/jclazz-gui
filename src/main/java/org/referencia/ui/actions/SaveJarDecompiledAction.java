package org.referencia.ui.actions;

import org.referencia.model.FileNode;
import org.referencia.ui.FileVisor;
import ru.andrew.jclazz.core.Clazz;
import ru.andrew.jclazz.decompiler.ClazzSourceView;
import ru.andrew.jclazz.decompiler.ClazzSourceViewFactory;
import ru.andrew.jclazz.decompiler.JarInputStreamBuilder;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
* Created with IntelliJ IDEA.
* User: alberto
* Date: 4/08/12
* Time: 14:03
* To change this template use File | Settings | File Templates.
*/
public class SaveJarDecompiledAction extends AbstractAction {
    private final Object selectedNode;
    private FileVisor fileVisor;

    public SaveJarDecompiledAction(FileVisor fileVisor, Object selectedNode) {
        super("Salvar fuentes");
        this.fileVisor = fileVisor;
        this.selectedNode = selectedNode;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            //TODO: Hay algo que no hace bien (ver enhancedjarFile para saber que es)
            File file = ((FileNode) selectedNode).getFile();
            JarFile jarFile = new JarFile(file);
            //TODO: comprobar que el destino no existe
            JarOutputStream jarOutput = new JarOutputStream(new FileOutputStream(file.getAbsolutePath().replaceAll("\\.jar$", ".src.jar")));
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                InputStream inputStream = jarFile.getInputStream(entry);
                //las clases internas no se están introduciendo en la clase superior, lo que habría que cambiar
                if (entryName.contains("$")) {
                    //La clase se incluira en su clase padre, no hay que hacer nada
                    continue;
                } else if (entryName.endsWith(".class") ) {
                    //TODO: Esta claro que aquí hay redundancia porque la clase que decompila
                    //lee un input para sacar un stringbuffer que aqui se convierte en un inputstream
                    //para volcarse a un outputstream (cambiar el destino del printwriter que usa para
                    //que este accesible aqui, de modo que al escribir en el se vaya volcando al outputstream
                    try {
                        ClazzSourceView clazzSourceView = ClazzSourceViewFactory.getClazzSourceView(new Clazz(entryName, new JarInputStreamBuilder(jarFile)));
                        String source = fileVisor.decompile(clazzSourceView);

                        JarEntry jarEntry = new JarEntry(entryName.replaceAll("\\.class$", ".java"));
                        jarOutput.putNextEntry(jarEntry);
                        jarOutput.write(source.getBytes(), 0, source.length());
                    } catch (Exception e1) {
                        System.out.println("error al decompilar: "+entryName);
                        e1.printStackTrace();
                        continue;
                    }
                } else if (entry.isDirectory()) {
                    //TODO: Hay que poner el separador final?
                    JarEntry jarEntry = new JarEntry(entryName+"/");
                    jarOutput.putNextEntry(jarEntry);
                } else {
                    JarEntry jarEntry = new JarEntry(entryName);
                    jarOutput.putNextEntry(jarEntry);
                    BufferedInputStream in = new BufferedInputStream(inputStream);

                    byte[] buffer = new byte[1024];
                    while (true)
                    {
                        int count = in.read(buffer);
                        if (count == -1)
                            break;
                        jarOutput.write(buffer, 0, count);
                    }

//                        jarOutput.putNextEntry(entry); //TODO: Igual hay que copiar el entry
//                        for (int data = inputStream.read(); data != -1; data = inputStream.read()) {
//                            jarOutput.write(data);
//                        }
                }
                jarOutput.closeEntry();
            }
            jarOutput.close();

            new ReloadAction(fileVisor, ((TreeNode) selectedNode).getParent()).actionPerformed(null);
            //Si no es el padre del jar puede salir mal, habría que
            //recargar el padre del jar decompilado
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
