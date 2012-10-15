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
import ru.andrew.jclazz.decompiler.FileInputStreamBuilder;
import ru.andrew.jclazz.decompiler.JarInputStreamBuilder;
import ru.andrew.jclazz.gui.ClazzTreeNode;
import ru.andrew.jclazz.gui.ClazzTreeNodeCellRenderer;
import ru.andrew.jclazz.gui.ClazzTreeUI;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
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
import java.util.Vector;
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
    private JTree treeInternals;

    private JTextArea fileDetailsTextArea;
    private JTable tableFileProperties;
    private JTable tableSandBoxProperties;

    public FileVisor(String directory) {
        super(Config.getInstance().getMessage("fileVisor.title"));
        buildGUI(directory);
    }

    public JTree getFileTree() {
        return fileTree;
    }

    private void buildGUI(String directory) {
        fileTree = buildFileTree(directory);

        fileDetailsTextArea = new JTextArea();
        fileDetailsTextArea.setEditable(false);
        tableFileProperties = new JTable(null, new Vector(Arrays.asList(new String[]{"Prop", "Value"})));
        tableFileProperties.getColumn("Value").setPreferredWidth(350);
        JSplitPane splitFileDetails = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, new JScrollPane(tableFileProperties), new JScrollPane(fileDetailsTextArea));

        final JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        treeInternals = new JTree();
        treeInternals.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent tse) {
                ClazzTreeNode node = (ClazzTreeNode) tse.getPath().getLastPathComponent();
                textPane.setText(node.getDescription());
            }
        });
        treeInternals.setCellRenderer(new ClazzTreeNodeCellRenderer());
        setEmptyTreeInternalModel();


        JSplitPane splitTreeInternals = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(treeInternals), new JScrollPane(textPane));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("File Details", splitFileDetails);
        tabbedPane.addTab("File Internals", splitTreeInternals);
        JSplitPane splitTreeDetails = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(fileTree), tabbedPane);

        Component comp;
        if (Boolean.parseBoolean(System.getProperty("debug.sandbox"))) {
            JScrollPane scrollSandBox = new JScrollPane(buildSandBoxArea());
            JTabbedPane tabbedPaneSandBox = new JTabbedPane();
            tabbedPaneSandBox.addTab("SandBox Editor", scrollSandBox);
            DefaultTableModel dm = new DefaultTableModel(
                    new Object[][]{
                            new Object[]{"compilation path", ""} //TODO: Usar el path actual de fileTree
                            , new Object[]{"class name", ""} //TODO: Usar el path actual de fileTree
                    }
                    , new Object[]{"Prop", "Value"}
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return super.isCellEditable(row, column) && column != 0;
                }
            };
            tableSandBoxProperties = new JTable(dm);
            tableSandBoxProperties.getColumn("Value").setPreferredWidth(400);
            tabbedPaneSandBox.addTab("SandBox Properties", new JScrollPane(tableSandBoxProperties));
            JSplitPane splitMainSandBox = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, splitTreeDetails
                    , tabbedPaneSandBox);
            splitMainSandBox.setOneTouchExpandable(true);
            comp = splitMainSandBox;
        } else {
            comp = splitTreeDetails;
        }

        getContentPane().add(comp);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setSize(640, 480);

        pack();
        setVisible(true);
    }

    private JTextArea buildSandBoxArea() {
        final JTextArea sandBoxArea = new JTextArea();
        sandBoxArea.setEditable(true);
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
                                String fileContents = tableSandBoxProperties.getModel().getValueAt(0, 1).toString();
                                String fileName = tableSandBoxProperties.getModel().getValueAt(1, 1).toString();
                                compile(fileName, sandBoxArea.getText(), fileContents);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    }));
                    popupMenu.add(new JMenuItem(new AbstractAction("Compare with decompiled") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                File jclazzTmpSand = File.createTempFile("jclazz", null);
                                File jclazzTmpDecompiled = File.createTempFile("jclazz", null);
                                PrintWriter pwSand = new PrintWriter(jclazzTmpSand);
                                pwSand.print(sandBoxArea.getText());
                                pwSand.close();
                                PrintWriter pwDecompiled = new PrintWriter(jclazzTmpDecompiled);
                                pwDecompiled.print(fileDetailsTextArea.getText());
                                pwDecompiled.close();
                                Process process = Runtime.getRuntime().exec(new String[]{"/usr/bin/meld"
                                        , jclazzTmpSand.getAbsolutePath()
                                        , jclazzTmpDecompiled.getAbsolutePath()});
                                process.waitFor();
                                jclazzTmpSand.delete();
                                jclazzTmpDecompiled.delete();
                            } catch (IOException e1) {
                                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                        }
                    }));
                    popupMenu.show(sandBoxArea, e.getX() + 3, e.getY() + 3);
                }
            }
        });
        return sandBoxArea;
    }


    public static void compile(String className, String javaFileContents, String outputPath) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector diagnosticsCollector = new DiagnosticCollector();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticsCollector, null, null);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, new Vector<File>(Arrays.asList(new File(outputPath))));
        Iterable fileObjects = Arrays.asList(new JavaObjectFromString(className, javaFileContents));
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
                    setFileDetails(file);
                } else {
                    CompressedNode compNode = (CompressedNode) selectedNode;
                    setCompressedDetails(compNode);
                }
            }
        });
        fileTree.addMouseListener(new TreeFileMouseListener(this));
        return fileTree;
    }

    private void setCompressedDetails(CompressedNode compNode) {
        String compressedFile = compNode.toString();
        CompressedNode node = compNode;
        String filePath = node.getFilePath();
        String fileComp = node.getCompressPath();

        StringBuffer sb = new StringBuffer();
        setTableProperties(compressedFile, filePath, Config.getInstance().getMessage("fileVisor.compressedFile"), 0);
        try {
            JarFile jarFile = new JarFile(filePath);
            byte[] buffer = new byte[4096];
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.equals(fileComp)) {
                    InputStream inputStream = jarFile.getInputStream(entry);
                    if (fileComp.endsWith(".class")) {
                        Clazz clazz = new Clazz(entryName, new JarInputStreamBuilder(jarFile));
                        String sourceText = decompile(clazz);
                        setClassTreeInternalModel(clazz);
                        if (sourceText != null) {
                            sb.append(sourceText);
                        }
                    } else {
                        setEmptyTreeInternalModel();
                        // TODO: entry.getSize() para parar la lectura de un archivo muy grande
                        // ojo a veces no funciona
                        //TODO: Detectar otros tipos de archivos (ejemplo AndroidManifest.xml compilado (usar apktool)
                        //Tendrá que ser un condicionante que diga (un archivo dentro de un apk que se llame *.xml leer con apktool)
                        System.out.println(entry.getSize());
                        //Si es una imagen o es muy grande no habría que volcarlo
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
        fileDetailsTextArea.setText(sb.toString());
        fileDetailsTextArea.setCaretPosition(0);
    }

    private void setEmptyTreeInternalModel() {
        treeInternals.setModel(new DefaultTreeModel(null));
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

    private void setFileDetails(File file) {
        if (file == null) {
            return;
        }
        StringBuffer buffer = new StringBuffer();
        try {
            String prop;
            if (JarUtils.isCompressFile(file.getName())) {
                prop = "Archivo comprimido";
            } else {
                FileInputStream fileInputStream = new FileInputStream(file);
                if (file.getName().endsWith(".class")) {
                    prop = "Archivo compilado";
                    try {
                        Clazz clazz = new Clazz(file.getAbsolutePath(), new FileInputStreamBuilder());
                        String sourceText = decompile(clazz);
                        setClassTreeInternalModel(clazz);
                        if (sourceText != null) {
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
                    prop = "Archivo normal";
                    readContents(buffer, fileInputStream);
                    setEmptyTreeInternalModel();
                }
            }
            setTableProperties(file.getName(), file.getPath(), prop, file.length());
        } catch (FileNotFoundException e) {
        }
        fileDetailsTextArea.setText(buffer.toString());
        fileDetailsTextArea.setCaretPosition(0);
    }

    private DefaultTableModel setTableProperties(String name, String path, String prop, long length ) {
        DefaultTableModel tableModel = (DefaultTableModel) tableFileProperties.getModel();
        while(tableFileProperties.getModel().getRowCount() > 0) {
            tableModel.removeRow(0);
        }
        tableModel.addRow(new Object[]{"Name", name});
        tableModel.addRow(new Object[]{"Path", path});
        tableModel.addRow(new Object[]{"Prop", prop});
        tableModel.addRow(new Object[]{"Size", length});
        return tableModel;
    }

    private void setClassTreeInternalModel(Clazz clazz) {
        treeInternals.setModel(new ClazzTreeUI(clazz).getTreeModel());
        treeInternals.setSelectionRow(0);
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