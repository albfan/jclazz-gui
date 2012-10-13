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
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.tools.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Visor de archivos normales y comprimidos
 * User: nenopera
 * Date: 24/09/11
 * Time: 1:27
 */
public class FileVisor extends JFrame {
    private JTree fileTree;

    private JTextArea fileDetailsTextArea;

    public FileVisor(String directory) {
        super(Config.getInstance().getMessage("fileVisor.title"));
        buildGUI(directory);
    }

    public JTree getFileTree() {
        return fileTree;
    }

    private void buildGUI(String directory) {
        fileDetailsTextArea = new JTextArea();
        fileDetailsTextArea.setEditable(false);
        fileTree = buildFileTree(directory);
        JSplitPane splitTreeDetails = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(
                fileTree), new JScrollPane(fileDetailsTextArea));

        Component comp;
        if (Boolean.parseBoolean(System.getProperty("debug.sandbox"))) {
            final JTextArea sandBoxArea = new JTextArea();
            sandBoxArea.setEditable(true);
            JSplitPane splitMainSandBox = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, new JScrollPane(
                    splitTreeDetails), new JScrollPane(sandBoxArea));
            comp = splitMainSandBox;

            sandBoxArea.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    showPopUp(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    showPopUp(e);
                }

                private void showPopUp(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        JPopupMenu popupMenu = new JPopupMenu("Opciones");
                        popupMenu.add(new JMenuItem(new AbstractAction("Compilar texto") {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    compile(sandBoxArea.getText());
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }));
                        popupMenu.show(sandBoxArea, e.getX() + 3, e.getY() + 3);
                    }
                }
            });

        } else {
            comp = splitTreeDetails;
        }

        getContentPane().add(comp);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setSize(640, 480);

        setVisible(true);
    }


    public static void compile(String javaFileContents) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector diagnosticsCollector = new DiagnosticCollector();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticsCollector, null, null);
        Iterable fileObjects = Arrays.asList(new JavaObjectFromString("TestClass", javaFileContents));
        //TODO: Configurar donde quedan los sources grabados, los compilados, hacer comparaciones...
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnosticsCollector, null, null, fileObjects);
        Boolean result = task.call();

        for (Object d : diagnosticsCollector.getDiagnostics()) {
            // Diagnosticos de la compilación
            System.out.println(d.toString());
        }
        if (result) {
            System.out.println("Compilation has succeeded");
        } else {
            System.out.println("Compilation fails.");
        }
    }

    /**
     * Clase que representa a un objeto compilable
     */
    static class JavaObjectFromString extends SimpleJavaFileObject {
        private String contents = null;

        public JavaObjectFromString(String className, String contents) throws Exception {
            super(new URI(className), Kind.SOURCE);
            this.contents = contents;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return contents;
        }
    }

    private JTree buildFileTree(String directory) {
        final JTree fileTree = new JTree(buildTreeModel(directory));
        fileTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (JarUtils.isCompressFile(value.toString())) {
                    setIcon(new ImageIcon("src/main/resources/jclazz/compress.png"));
                }
                return component;
            }
        });
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
        fileTree.addMouseListener(new TreeFileMouseListener(this));
        return fileTree;
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

    public String decompile(ClazzSourceView clazzSourceView) throws IOException, ClazzException {
        HashMap params = new HashMap();
        params.put(ClazzSourceView.WITH_LINE_NUMBERS, "yes");
        params.put(ClazzSourceView.SUPPRESS_EXCESSED_THIS, "yes");

        //TODO: Se podría añadir el tipo de identación o intentar poner las lineas en el sitio correcto...
        clazzSourceView.setDecompileParameters(params);
        String sourceText = clazzSourceView.getSource();

        return sourceText;
    }

    public TreeModel buildTreeModel(String directory) {
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
}