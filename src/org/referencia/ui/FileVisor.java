package org.referencia.ui;

import org.referencia.config.Config;
import org.referencia.model.CompresedFileTreeModel;
import org.referencia.model.CompressedNode;
import org.referencia.model.FileNode;
import ru.andrew.jclazz.core.Clazz;
import ru.andrew.jclazz.core.ClazzException;
import ru.andrew.jclazz.decompiler.ClazzSourceView;
import ru.andrew.jclazz.decompiler.ClazzSourceViewFactory;

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
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * Visor de archivos normales y comprimidos
 * User: alberto
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
                if (e.isPopupTrigger()) {
                    final Object selectedNode = fileTree.getLastSelectedPathComponent();
                    if (selectedNode == fileTree.getModel().getRoot()) {
                        final File file = ((FileNode)selectedNode).getFile();
                        JPopupMenu popup = new JPopupMenu("Acciones");
                        if (file.isDirectory()) {
                            popup.add(new AbstractAction("Subir") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    String absolutePath = file.getAbsolutePath();
                                    int endIndex = absolutePath.lastIndexOf(System.getProperty("file.separator"));
                                    String upperDir = absolutePath.substring(0
                                            , absolutePath.lastIndexOf(System.getProperty("file.separator"), endIndex));
                                    fileTree.setModel(buildTreeModel(upperDir));
                                }
                            });
                        }
                        //TODO: Acciones para ficheros normales, comparar etc...
                        popup.show(fileTree, e.getX(), e.getY());
                    } else if (selectedNode instanceof FileNode) {
                        final File file = ((FileNode) selectedNode).getFile();
                        JPopupMenu popup = new JPopupMenu("Acciones");
                        if (file.isDirectory()) {
                            popup.add(new AbstractAction("Aislar") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    fileTree.setModel(buildTreeModel(file.getAbsolutePath()));
                                }
                            });
                            popup.add(new AbstractAction("Recargar") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    reload((TreeNode) selectedNode);
                                }
                            });
                        } else if (file.getAbsolutePath().endsWith(".jar")) {
                            popup.add(new AbstractAction("Salvar fuentes") {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    try {
                                        JarFile jarFile = new JarFile(file);
                                        JarInputStream jarInput = new JarInputStream(new FileInputStream(file));
                                        //TODO: comprobar que el destino no existe
                                        JarOutputStream jarOutput = new JarOutputStream(new FileOutputStream(file.getAbsolutePath().replaceAll("\\.jar$", ".src.jar")));
                                        JarEntry entry;
                                        while ((entry = jarInput.getNextJarEntry()) != null) {
                                            String entryName = entry.getName();
                                            //Las clases internas se obtienen en la clase superior asi que no se usan (ver si es posible llamar a una clase con $
                                            if (entryName.endsWith(".class") && !entryName.contains("$")) {
                                                //TODO: Esta claro que aquí hay redundancia porque la clase que decompila
                                                //lee un input para sacar un stringbuffer que aqui se convierte en un inputstream
                                                //para volcarse a un outputstream (cambiar el destino del printwriter que usa para
                                                //que este accesible aqui, de modo que al escribir en el se vaya volcando al outputstream
                                                ClazzSourceView clazzSourceView = ClazzSourceViewFactory.getJarClazzSourceView(new Clazz(entryName, jarInput), jarFile);
                                                String source = decompile(clazzSourceView);

                                                JarEntry jarEntry = new JarEntry(entryName.replaceAll("\\.class$", ".java"));
                                                jarOutput.putNextEntry(jarEntry);
                                                jarOutput.write(source.getBytes(), 0, source.length());
                                            } else {
                                                jarOutput.putNextEntry(entry);
                                                for (int data = jarInput.read(); data != -1; data = jarInput.read()) {
                                                    jarOutput.write(data);
                                                }
                                            }
                                        }
                                        jarOutput.finish();
                                        jarOutput.close();
                                        jarInput.close();
                                        reload(((TreeNode) selectedNode).getParent());
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    } catch (ClazzException e1) {
                                        e1.printStackTrace();
                                    }
                                }

                            });
                        }
                        //TODO: Acciones para ficheros normales, comparar etc...
                        popup.show(fileTree, e.getX(), e.getY());
                    } else {
                        CompressedNode compNode = (CompressedNode) selectedNode;
                        final String filePath = compNode.getFilePath();
                        if (filePath.endsWith(".jar")) {
                            //TODO: decompilar todo y sacar a un jar
                            JPopupMenu popup = new JPopupMenu("Acciones");
                            if (true) {
                                popup.add(new AbstractAction("Mensaje") {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        JOptionPane.showMessageDialog(FileVisor.this, filePath);
                                    }
                                });
                            }
                            popup.show(fileTree, e.getX(), e.getY());
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
        sb.append("File: " + compressedFile);
        //TODO: Añadir propiedades
        sb.append("Prop: " + Config.getInstance().getMessage("fileVisor.compressedFile") + "\n");
        sb.append("Contents:\n");
        try {
            JarInputStream jarInput = new JarInputStream(new FileInputStream(file));
            JarEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = jarInput.getNextJarEntry()) != null) {
                if (entry.getName().equals(fileComp)) {
                    if (fileComp.endsWith(".class")) {
                        String sourceText = decompile(fileComp, jarInput);
                        if (sourceText != null) {
                            sb.append("Decompilado:\n");
                            sb.append(sourceText);
                        }
                    } else {
                        //Si es una imagen o es muy grande no habría que volcarlo
                        while (jarInput.read(buffer) != -1) {
                            sb.append(new String(buffer, Charset.defaultCharset()));
                        }
                    }
                }
            }
            //TODO: No funciona porque la libreria del jd-gui necesita tener abierto el eclipse
            //(seguro que se puede trucar. Habría que probar con el eclipse abierto, pero por ahora
            //voy a usar otra libreria jclazz

//            readContents(sb, jarInput);
//            loadLibrary();
//
//            String result = new Decompiler().decomp(file.getAbsolutePath(), fileComp);


        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String decompile(String fileName, InputStream in) throws IOException, ClazzException {
        return decompile(new Clazz(fileName, in));
    }

    private String decompile(Clazz clazz) throws IOException, ClazzException {
        return decompile(ClazzSourceViewFactory.getFileClazzSourceView(clazz));
    }

    private String decompile(ClazzSourceView clazzSourceView) throws IOException, ClazzException {
        HashMap params = new HashMap();
        params.put(ClazzSourceView.WITH_LINE_NUMBERS, "yes");
//            params.put(ClazzSourceView.SUPPRESS_EXCESSED_THIS); //Lo tiene puesta para que se añada siempre
        //TODO: Se podría añadir el tipo de identación o intentar poner las lineas en el sitio correcto...
        clazzSourceView.setDecompileParameters(params);
        String sourceText = clazzSourceView.getSource();

//        sourceText = sourceText.replaceAll(" ", "&nbsp;");
//        sourceText = sourceText.replaceAll("<", "&lt;");
//        sourceText = sourceText.replaceAll(">", "&gt;");
//        sourceText = sourceText.replaceAll("\n", "<BR>");
        return sourceText;
    }

//    class Decompiler extends JDSourceMapperLinuxX86 {
//        Decompiler() {
//            super(null, null, null, null);
//        }
//
//        public String decomp(String jarPath, String compiledPath) {
//            return decompile(jarPath, compiledPath);
//        }
//    }

    boolean loaded = false;

    protected void loadLibrary() throws IOException {
        if (!loaded) {
            System.load("/home/alberto/projects/java/jd-gui/lib/linux/x86/libjd-eclipse.so");
            loaded = true;
        }
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
            if (file.getName().endsWith("jar") || file.getName().endsWith("zip")) {
                buffer.append("Contents: Archivo comprimido");
            } else {
                readContents(buffer, new FileInputStream(file));
            }
        } catch (FileNotFoundException e) {
        }
        return buffer.toString();
    }

    private void readContents(StringBuffer buffer, InputStream fileInputStream) {
        buffer.append("Contents:\n");
        try {
            InputStream input = fileInputStream;

            byte[] fileData = new byte[input.available()];

            input.read(fileData);
            input.close();
            buffer.append(new String(fileData, Charset.defaultCharset()));
        } catch (IOException e) {
            buffer.append("error reading: " + e.getMessage());
        }
    }
}