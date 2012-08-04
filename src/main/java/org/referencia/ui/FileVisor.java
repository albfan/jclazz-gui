package org.referencia.ui;

import org.referencia.config.Config;
import org.referencia.model.CompresedFileTreeModel;
import org.referencia.model.CompressedNode;
import org.referencia.model.FileNode;
import org.referencia.model.JarUtils;
import ru.andrew.jclazz.core.Clazz;
import ru.andrew.jclazz.core.ClazzException;
import ru.andrew.jclazz.decompiler.ClazzSourceView;
import ru.andrew.jclazz.decompiler.ClazzSourceViewFactory;
import ru.andrew.jclazz.decompiler.JarInputStreamBuilder;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Visor de archivos normales y comprimidos
 * User: nenopera
 * Date: 24/09/11
 * Time: 1:27
 */
public class FileVisor extends JFrame {
    private JTree fileTree;

    private JTextArea fileDetailsTextArea = new JTextArea();

    public FileVisor(String directory) {
        super(Config.getInstance().getMessage("fileVisor.title"));
        buildGUI(directory);
    }

    private void buildGUI(String directory) {
        fileDetailsTextArea.setEditable(false);
        fileTree = new JTree(buildTreeModel(directory));
        fileTree.setEditable(true);
        fileTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                Object lastPathComponent = event.getPath().getLastPathComponent();
                if (lastPathComponent instanceof FileNode) {
                    ((FileNode) lastPathComponent).buildChilds();
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                Object lastPathComponent = event.getPath().getLastPathComponent();
                if (lastPathComponent instanceof FileNode) {
                    ((FileNode) lastPathComponent).setLoaded(false);
                }
            }
        });
        fileTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent event) {
                Object selectedNode = fileTree.getLastSelectedPathComponent();
                if (selectedNode == null) {
                    return;
                }
                if (selectedNode instanceof FileNode) {
                    File file = ((FileNode) selectedNode).getFile();
                    fileDetailsTextArea.setText(getFileDetails(file));
                } else {
                    CompressedNode compNode = (CompressedNode) selectedNode;
                    fileDetailsTextArea.setText(getCompressedDetails(compNode, event));
                    fileDetailsTextArea.setCaretPosition(0);
                }
            }
        });
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                final Object selectedNode = fileTree.getLastSelectedPathComponent();
                if (selectedNode == null) {
                    return;
                }
                if (e.isPopupTrigger()) {
                    boolean showPopUp = false;
                    JPopupMenu popup = new JPopupMenu("Acciones");
                    if (selectedNode == fileTree.getModel().getRoot()) {
                        if (selectedNode instanceof FileNode) {
                            File file = ((FileNode)selectedNode).getFile();
                            if (file.isDirectory()) {
                                showPopUp = true;
                                popup.add(new UpAction(file));
                                popup.add(new ReloadAction(selectedNode));
                            }
                        } else if (selectedNode instanceof CompressedNode) {

                        }
                    } else if (selectedNode instanceof FileNode) {
                        File file = ((FileNode)selectedNode).getFile();
                        if (file.isDirectory()) {
                            popup.add(new IsolateAction(file));
                        } else if (file.getName().endsWith(".jar") || file.getName().endsWith(".apk")) {
                            popup.add(new SaveJarDecompiled(selectedNode));
                        }
                        showPopUp = true;
                        //TODO: Acciones para ficheros normales, comparar etc...
                    } else {
                        CompressedNode compNode = (CompressedNode) selectedNode;
                        final String filePath = compNode.getFilePath();
                        if (filePath.endsWith(".jar")) {
                            popup.add(new AbstractAction("Mensaje") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    JOptionPane.showMessageDialog(FileVisor.this, filePath);
                                }
                            });
                        }
                    }
                    if (showPopUp) {
                        popup.show(fileTree, e.getX(), e.getY());
                    }
                } else if (e.getClickCount() == 2) {
                    if (selectedNode == fileTree.getModel().getRoot()) {
                        if (selectedNode instanceof FileNode) {
                            File file = ((FileNode)selectedNode).getFile();
                            if (file.isDirectory()) {
                                new UpAction(file).actionPerformed(null);
                            }
                        }
                    } else {
                        if (selectedNode instanceof FileNode) {
                            File file = ((FileNode)selectedNode).getFile();
                            if (file.isDirectory()) {
                                new IsolateAction(file).actionPerformed(null);
                            }
                        }
                    }
                }
            }
        }

        );
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(
                fileTree), new JScrollPane(fileDetailsTextArea));

        getContentPane().add(splitPane);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setSize(640, 480);

        setVisible(true);

    }

    private void reload(TreeNode selectedNode) {
        //TODO: Se podría llamar también al suceder un cambio en el sistema (haría falta un filesystem)
        if (selectedNode instanceof FileNode) {
            ((FileNode) selectedNode).buildChilds();
        }
        ((DefaultTreeModel)fileTree.getModel()).nodeStructureChanged(selectedNode);
    }

    private String getCompressedDetails(CompressedNode compNode, TreeSelectionEvent event) {
        String compressedFile = compNode.toString();
        CompressedNode node = compNode;
        String file = node.getFilePath();
        String fileComp = node.getCompressPath();

        StringBuffer sb = new StringBuffer();
        sb.append("File: ").append(compressedFile).append("\n");
        sb.append("Prop: ").append(Config.getInstance().getMessage("fileVisor.compressedFile")).append("\n");
        sb.append("Contents:\n");
        try {
            JarFile jarFile = new JarFile(file);
            byte[] buffer = new byte[4096];
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.equals(fileComp)) {
                    InputStream inputStream = jarFile.getInputStream(entry);
                    if (fileComp.endsWith(".class")) {
                        String sourceText = decompile(ClazzSourceViewFactory.getClazzSourceView(new Clazz(entryName, new JarInputStreamBuilder(jarFile))));
                        if (sourceText != null) {
                            sb.append("Decompilado:\n");
                            sb.append(sourceText);
                        }
                    } else {
                        // TODO: entry.getSize() para parar la lectura de un archivo muy grande
                        // ojo a veces no funciona
                        //TODO: Detectar otros tipos de archivos (ejemplo AndroidManifest.xml compilado (usar apktool)
                        //Tendrá que ser un condicionante que diga (un archivo dentro de un apk que se llame *.xml leer con apktool)
                        System.out.println(entry.getSize());
                        //Si es una imagen o es muy grande no habr�a que volcarlo
                        int readed = 0;
                        boolean allReaded = false;
                        while (readed < 30000) { //Evita que se lean archivos muy grandes
                            int read = inputStream.read(buffer);
                            sb.append(new String(buffer, Charset.defaultCharset()));
                            if (read == -1) {
                                allReaded = true;
                                break;
                            } else {
                                readed += read;
                            }
                        }
                        if (!allReaded) {
                            sb.append("\n...\nnot all readed\n"); //TODO: Usar locale message
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String decompile(String fileName) throws IOException, ClazzException {
        return decompile(new Clazz(fileName));
    }

    private String decompile(Clazz clazz) throws IOException, ClazzException {
        return decompile(ClazzSourceViewFactory.getClazzSourceView(clazz));
    }

    private String decompile(ClazzSourceView clazzSourceView) throws IOException, ClazzException {
        HashMap params = new HashMap();
        params.put(ClazzSourceView.WITH_LINE_NUMBERS, "yes");
        params.put(ClazzSourceView.SUPPRESS_EXCESSED_THIS, "yes");

        //TODO: Se podría añadir el tipo de identación o intentar poner las lineas en el sitio correcto...
        clazzSourceView.setDecompileParameters(params);
        String sourceText = clazzSourceView.getSource();

        return sourceText;
    }

    private TreeModel buildTreeModel(String directory) {
        Config.getInstance().setShowHiddenFiles(false);
        Config.getInstance().setIgnorecase(true);
        return new CompresedFileTreeModel(new File(directory));
    }

    private String getFileDetails(File file) {
        if (file == null)
            return "";
        StringBuffer buffer = new StringBuffer();
        buffer.append("Name: " + file.getName() + "\n");
        buffer.append("Path: " + file.getPath() + "\n");
        buffer.append("Size: " + file.length() + "\n");
        try {
            if (JarUtils.isCompressFile(file.getName())) {
                buffer.append("Contents: Archivo comprimido");
            } else {
                FileInputStream fileInputStream = new FileInputStream(file);
                if (file.getName().endsWith(".class")) {
                    try {
                        String sourceText = decompile(file.getAbsolutePath());
                        if (sourceText != null) {
                            buffer.append("Decompilado:\n");
                            buffer.append(sourceText);
                        }
                    } catch (IOException e) {
                        buffer.append("Error al decompilar:\n");
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        buffer.append(sw.toString());
                    } catch (ClazzException e) {
                        buffer.append("Error al decompilar:\n");
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        buffer.append(sw.toString());
                    }
                } else {
                    readContents(buffer, fileInputStream);
                }
            }
        } catch (FileNotFoundException e) {
        }
        return buffer.toString();
    }

    private void readContents(StringBuffer buffer, InputStream fileInputStream) {
        buffer.append("Contents:\n");
        try {

            //TODO: Este no es un modo correcto de leer un fichero:
            // Leer correctamente y permitir cancelar la lectura (es decir, leer en segundo plano)
            InputStream input = fileInputStream;

            byte[] fileData = new byte[input.available()];

            input.read(fileData);
            input.close();
            buffer.append(new String(fileData, Charset.defaultCharset()));
        } catch (IOException e) {
            buffer.append("error reading: " + e.getMessage());
        }
    }

    private class UpAction extends AbstractAction {
        private final File file;

        public UpAction(File file) {
            super("Subir");
            this.file = file;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String absolutePath = file.getAbsolutePath();
            int endIndex = absolutePath.lastIndexOf(System.getProperty("file.separator"));
            String upperDir = absolutePath.substring(0
                    , absolutePath.lastIndexOf(System.getProperty("file.separator"), endIndex));
            fileTree.setModel(buildTreeModel(upperDir));
        }
    }

    private class ReloadAction extends AbstractAction {
        private final Object selectedNode;

        public ReloadAction(Object selectedNode) {
            super("Recargar");
            this.selectedNode = selectedNode;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            reload((TreeNode) selectedNode);
        }
    }

    private class IsolateAction extends AbstractAction {
        private final File file;

        public IsolateAction(File file) {
            super("Aislar");
            this.file = file;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fileTree.setModel(buildTreeModel(file.getAbsolutePath()));
        }
    }

    private class SaveJarDecompiled extends AbstractAction {
        private final Object selectedNode;

        public SaveJarDecompiled(Object selectedNode) {
            super("Salvar fuentes");
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
                            String source = decompile(clazzSourceView);

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
                reload(((TreeNode) selectedNode).getParent()); //Si no es el padre del jar puede salir mal, habría que
                //recargar el padre del jar decompilado
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}