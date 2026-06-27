package GUICompilador;

import Compilador.Parser;
import Compilador.Scanner;
import Compilador.tipoID;
import Compilador.Semantico;
import Compilador.Triple;
import Compilador.GeneradorCodigoObjeto; 

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class Windowgui extends JFrame implements ActionListener, WindowListener {

    private static final long serialVersionUID = 1L;

    private JMenuBar jmbMenu;
    private JMenu jmArchivo;
    private JMenuItem jmiAbrir, jmiGuardar, jmiGuardarComo, jmiSalir;

    private JTextArea textAreaCodigo;
    private JTable tableTokens;
    private DefaultTableModel modelTokens;
    private DefaultTableModel modelSemantico;
    private JTextArea textAreaErrores;
    private JTextArea textAreaIntermedio;
    private JTextArea textAreaObjeto;

    private JButton btnTokens;
    private JButton btnParser;
    private JButton btnSemantico;
    private JButton btnCI;
    private JButton btnCO; // Declaración del botón CO

    private File archivoActual = null;
    private int fontSizeCodigo = 16;
    private int fontSizeErrores = 16;
    private int fontSizeTablas = 14;

    private Semantico semanticoActual;

    private void actualizarFuentes() {
        textAreaCodigo.setFont(new Font("Monospaced", Font.PLAIN, fontSizeCodigo));
        textAreaErrores.setFont(new Font("Monospaced", Font.PLAIN, fontSizeErrores));
        tableTokens.setFont(new Font("Monospaced", Font.PLAIN, fontSizeTablas));
        tableTokens.setRowHeight(fontSizeTablas + 6);
        textAreaIntermedio.setFont(new Font("Monospaced", Font.PLAIN, fontSizeCodigo));
        textAreaObjeto.setFont(new Font("Monospaced", Font.PLAIN, fontSizeCodigo));
    }

    public Windowgui() {
        super("Akirani");
        setLayout(new BorderLayout());
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(this);
        initComponents();
        setLocationRelativeTo(null);
        setVisible(true);
        actualizarFuentes();
    }

    private void initComponents() {
        Menu();
        PanelPrincipal();
    }

    private void Menu() {
        jmbMenu = new JMenuBar();
        jmbMenu.setAlignmentX(LEFT_ALIGNMENT);
        jmbMenu.setBackground(new Color(200, 220, 255));
        jmbMenu.setOpaque(true);

        jmArchivo = new JMenu("Archivo");
        jmArchivo.setMnemonic('A');

        jmiAbrir = new JMenuItem("Abrir...");
        jmiAbrir.addActionListener(this);
        jmArchivo.add(jmiAbrir);

        jmiGuardar = new JMenuItem("Guardar");
        jmiGuardar.addActionListener(this);
        jmArchivo.add(jmiGuardar);

        jmiGuardarComo = new JMenuItem("Guardar como...");
        jmiGuardarComo.addActionListener(this);
        jmArchivo.add(jmiGuardarComo);

        jmArchivo.addSeparator();

        jmiSalir = new JMenuItem("Salir");
        jmiSalir.addActionListener(this);
        jmArchivo.add(jmiSalir);

        jmbMenu.add(jmArchivo);
        setJMenuBar(jmbMenu);
    }

    private void PanelPrincipal() {
        textAreaCodigo = new JTextArea();
        textAreaCodigo.setFont(new Font("Monospaced", Font.PLAIN, fontSizeCodigo));

        textAreaCodigo.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown()) {
                    if (e.getKeyCode() == KeyEvent.VK_PLUS || e.getKeyCode() == KeyEvent.VK_EQUALS) {
                        fontSizeCodigo++;
                        fontSizeErrores++;
                        fontSizeTablas++;
                        actualizarFuentes();
                    } else if (e.getKeyCode() == KeyEvent.VK_MINUS) {
                        if (fontSizeCodigo > 8) {
                            fontSizeCodigo--;
                            fontSizeErrores--;
                            fontSizeTablas = Math.max(8, fontSizeTablas - 1);
                            actualizarFuentes();
                        }
                    }
                }
            }
        });

        textAreaCodigo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void limpiarTodo() {
                if (modelTokens != null) modelTokens.setRowCount(0);
                if (modelSemantico != null) modelSemantico.setRowCount(0);
                textAreaErrores.setText("");
                textAreaErrores.setBackground(Color.WHITE);
                textAreaIntermedio.setText("");
                textAreaObjeto.setText("");
                btnTokens.setEnabled(true);
                btnParser.setEnabled(false);
                btnSemantico.setEnabled(false);
                btnCI.setEnabled(false);
                btnCO.setEnabled(false); // Deshabilitar CO
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { limpiarTodo(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { limpiarTodo(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { limpiarTodo(); }
        });

        JScrollPane scrollCodigo = new JScrollPane(textAreaCodigo);
        scrollCodigo.setBorder(BorderFactory.createTitledBorder("Código"));

        btnTokens = new JButton("Tokens");
        btnTokens.addActionListener(this);
        btnTokens.setBackground(new Color(200, 220, 255));

        btnParser = new JButton("Parser");
        btnParser.addActionListener(this);
        btnParser.setBackground(new Color(200, 220, 255));

        btnSemantico = new JButton("Semántico");
        btnSemantico.addActionListener(this);
        btnSemantico.setBackground(new Color(200, 220, 255));

        btnCI = new JButton("CI");
        btnCI.addActionListener(this);
        btnCI.setBackground(new Color(200, 220, 255));
        btnCI.setEnabled(false);
        
        btnCO = new JButton("CO"); // Inicialización de btnCO
        btnCO.addActionListener(this);
        btnCO.setBackground(new Color(200, 220, 255));
        btnCO.setEnabled(false);


        btnTokens.setEnabled(true);
        btnParser.setEnabled(false);
        btnSemantico.setEnabled(false);

        JPanel panelBotones = new JPanel(new GridLayout(2, 1, 5, 5));
        panelBotones.add(btnParser);
        panelBotones.add(btnSemantico);

        textAreaErrores = new JTextArea();
        textAreaErrores.setEditable(false);
        textAreaErrores.setLineWrap(true);
        textAreaErrores.setWrapStyleWord(true);
        textAreaErrores.setForeground(Color.RED);
        textAreaErrores.setFont(new Font("Monospaced", Font.PLAIN, fontSizeErrores));

        JScrollPane scrollErrores = new JScrollPane(textAreaErrores);
        scrollErrores.setBorder(BorderFactory.createTitledBorder("Errores"));

        JPanel panelErrores = new JPanel(new BorderLayout());
        panelErrores.add(panelBotones, BorderLayout.NORTH);
        panelErrores.add(scrollErrores, BorderLayout.CENTER);

        modelTokens = new DefaultTableModel();
        modelTokens.addColumn("Token");
        modelTokens.addColumn("Lexema");

        modelSemantico = new DefaultTableModel();
        modelSemantico.addColumn("Nombre");
        modelSemantico.addColumn("Tipo");
        modelSemantico.addColumn("Valor");
        modelSemantico.addColumn("Dirección");

        tableTokens = new JTable(modelTokens);
        tableTokens.setFont(new Font("Monospaced", Font.PLAIN, fontSizeTablas));
        tableTokens.setRowHeight(fontSizeTablas + 6);

        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.add(btnTokens, BorderLayout.NORTH);
        panelTabla.add(new JScrollPane(tableTokens), BorderLayout.CENTER);

        JSplitPane splitIzqCentro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollCodigo, panelTabla);
        splitIzqCentro.setDividerLocation(350);

        JSplitPane splitSuperior = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitIzqCentro, panelErrores);
        splitSuperior.setDividerLocation(700);

        // --- Área de Código Intermedio (CI) ---
        textAreaIntermedio = new JTextArea();
        textAreaIntermedio.setFont(new Font("Monospaced", Font.PLAIN, fontSizeCodigo));
        textAreaIntermedio.setEditable(false);
        textAreaIntermedio.setLineWrap(true);
        textAreaIntermedio.setWrapStyleWord(true);
        JScrollPane scrollIntermedio = new JScrollPane(textAreaIntermedio);
        scrollIntermedio.setBorder(BorderFactory.createTitledBorder("Código Intermedio"));

        JPanel panelIntermedioConBoton = new JPanel(new BorderLayout());
        panelIntermedioConBoton.add(btnCI, BorderLayout.NORTH); // Botón CI arriba
        panelIntermedioConBoton.add(scrollIntermedio, BorderLayout.CENTER);

        // --- Área de Código Objeto (CO) ---
        textAreaObjeto = new JTextArea();
        textAreaObjeto.setFont(new Font("Monospaced", Font.PLAIN, fontSizeCodigo));
        textAreaObjeto.setEditable(false);
        textAreaObjeto.setLineWrap(true);
        textAreaObjeto.setWrapStyleWord(true);
        JScrollPane scrollObjeto = new JScrollPane(textAreaObjeto);
        scrollObjeto.setBorder(BorderFactory.createTitledBorder("Código Objeto"));
        
        JPanel panelObjetoConBoton = new JPanel(new BorderLayout()); // Contenedor para CO
        panelObjetoConBoton.add(btnCO, BorderLayout.NORTH); // Botón CO arriba
        panelObjetoConBoton.add(scrollObjeto, BorderLayout.CENTER);


        JSplitPane splitInferior = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelIntermedioConBoton, panelObjetoConBoton); // Usa los nuevos paneles
        splitInferior.setDividerLocation(500);

        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitSuperior, splitInferior);
        splitPrincipal.setDividerLocation(350);

        add(splitPrincipal, BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == jmiSalir) System.exit(0);
        else if (e.getSource() == jmiAbrir) abrirArchivo();
        else if (e.getSource() == jmiGuardar) guardarArchivo();
        else if (e.getSource() == jmiGuardarComo) guardarArchivoComo();
        else if (e.getSource() == btnTokens) mostrarTokens();
        else if (e.getSource() == btnParser) ejecutarParser();
        else if (e.getSource() == btnSemantico) mostrarSemantico();
        else if (e.getSource() == btnCI) ejecutarCI();
        else if (e.getSource() == btnCO) ejecutarCO(); // Llama al método ejecutarCO()
    }

    private void abrirArchivo() {
        FileDialog fd = new FileDialog(this, "Selecciona archivo", FileDialog.LOAD);
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir != null && file != null) {
            archivoActual = new File(dir, file);
            try (BufferedReader br = new BufferedReader(new FileReader(archivoActual))) {
                textAreaCodigo.setText("");
                String line;
                while ((line = br.readLine()) != null) textAreaCodigo.append(line + "\n");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al abrir archivo", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void guardarArchivo() {
        if (archivoActual != null) {
            try (PrintWriter pw = new PrintWriter(archivoActual)) {
                pw.print(textAreaCodigo.getText());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al guardar", JOptionPane.ERROR_MESSAGE);
            }
        } else guardarArchivoComo();
    }

    private void guardarArchivoComo() {
        FileDialog fd = new FileDialog(this, "Guardar archivo como", FileDialog.SAVE);
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir != null && file != null) {
            archivoActual = new File(dir, file);
            guardarArchivo();
        }
    }

    private void mostrarTokens() {
        if (textAreaCodigo.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay codigo para analizar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        modelTokens.setRowCount(0);
        textAreaErrores.setText("");

        try {
            File tmp = File.createTempFile("codigo", ".tmp");
            tmp.deleteOnExit();
            try (PrintWriter pw = new PrintWriter(tmp)) { pw.print(textAreaCodigo.getText()); }

            Scanner scanner = new Scanner(tmp);
            tipoID token;
            do {
                token = scanner.getToken();
                if (token != tipoID.EOF) {
                    modelTokens.addRow(new Object[]{
                        Scanner.getNombreToken(token),
                        scanner.getLexema()
                    });
                }
            } while (token != tipoID.EOF);

            tableTokens.setModel(modelTokens);
            actualizarFuentes();
            textAreaErrores.setForeground(new Color(0, 128, 0));
            textAreaErrores.setBackground(new Color(240, 255, 240));
            textAreaErrores.setText("ANALISIS LEXICO CORRECTO");

            btnParser.setEnabled(true);
            btnSemantico.setEnabled(false);
            btnCI.setEnabled(false);
            btnCO.setEnabled(false); // Deshabilitar CO
        } catch (Exception ex) {
            textAreaErrores.setForeground(Color.RED);
            textAreaErrores.setBackground(new Color(255, 240, 240));
            textAreaErrores.setText("ERROR LEXICO: " + ex.getMessage());
            btnParser.setEnabled(false);
            btnSemantico.setEnabled(false);
            btnCI.setEnabled(false);
            btnCO.setEnabled(false); // Deshabilitar CO
        }
    }

    private void ejecutarParser() {
        textAreaErrores.setText("");
        if (textAreaCodigo.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay codigo para parsear.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            File tmp = File.createTempFile("codigo", ".tmp");
            tmp.deleteOnExit();
            try (PrintWriter pw = new PrintWriter(tmp)) { pw.print(textAreaCodigo.getText()); }

            Parser parser = new Parser(tmp);
            parser.parseProgram();

            textAreaErrores.setForeground(new Color(0, 128, 0));
            textAreaErrores.setBackground(new Color(240, 255, 240));
            textAreaErrores.setText("PARSER CORRECTO");

            btnSemantico.setEnabled(true);
            btnCI.setEnabled(false);
            btnCO.setEnabled(false); // Deshabilitar CO
        } catch (RuntimeException ex) {
            textAreaErrores.setForeground(Color.RED);
            textAreaErrores.setBackground(new Color(255, 240, 240));
            textAreaErrores.setText("SYNTAX ERROR: " + ex.getMessage());
            btnSemantico.setEnabled(false);
            btnCI.setEnabled(false);
            btnCO.setEnabled(false); // Deshabilitar CO
        } catch (IOException ioEx) {
            textAreaErrores.setForeground(Color.RED);
            textAreaErrores.setBackground(new Color(255, 240, 240));
            textAreaErrores.setText("ERROR DE ARCHIVO: " + ioEx.getMessage());
            btnSemantico.setEnabled(false);
            btnCI.setEnabled(false);
            btnCO.setEnabled(false); // Deshabilitar CO
        }
    }

    private void mostrarSemantico() {
        if (textAreaCodigo.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay codigo para analizar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        modelSemantico.setRowCount(0);
        textAreaErrores.setText("");
        textAreaIntermedio.setText("");
        textAreaObjeto.setText("");

        try {
            File tmp = File.createTempFile("codigo", ".tmp");
            tmp.deleteOnExit();
            try (PrintWriter pw = new PrintWriter(tmp)) { pw.print(textAreaCodigo.getText()); }

            Semantico semantico = new Semantico(tmp);
            semantico.analizarPrograma();

            for (Map.Entry<String, Semantico.InfoVariable> entry : semantico.getTablaSimbolos().entrySet()) {
                Semantico.InfoVariable info = entry.getValue();
                modelSemantico.addRow(new Object[]{
                    entry.getKey(),
                    info.tipo,
                    info.valor,
                    info.direccion
                });
            }

            tableTokens.setModel(modelSemantico);
            actualizarFuentes();
            textAreaErrores.setForeground(new Color(0, 128, 0));
            textAreaErrores.setBackground(new Color(240, 255, 240));
            textAreaErrores.setText("ANÁLISIS SEMÁNTICO CORRECTO");

            semanticoActual = semantico; // Guardamos instancia para CI
            btnCI.setEnabled(true);
            btnCO.setEnabled(true); // <--- Habilitar CO
        } catch (Exception ex) {
            textAreaErrores.setForeground(Color.RED);
            textAreaErrores.setBackground(new Color(255, 240, 240));
            textAreaErrores.setText("ERROR SEMÁNTICO: " + ex.getMessage());
            btnCI.setEnabled(false);
            btnCO.setEnabled(false); // Deshabilitar CO
        }
    }

    /**
     * Reutiliza la lógica de escaneo para generar los triples (CI Lógico).
     * @param gci Instancia de GeneradorCodigoIntermedio para almacenar triples.
     * @throws IOException Si hay error de archivo o léxico.
     */
    private void generarTriples(Compilador.GeneradorCodigoIntermedio gci) throws IOException {
        File tmp = File.createTempFile("codigo", ".tmp");
        tmp.deleteOnExit();
        try (PrintWriter pw = new PrintWriter(tmp)) {
            pw.print(textAreaCodigo.getText());
        }

        Scanner scanner = new Scanner(tmp);
        tipoID token;
        String lexema;
        
        do {
            token = scanner.getToken();
            lexema = scanner.getLexema();
            
            if (token == tipoID.PR && lexema.equals("while")) {
                scanner.getToken(); // PARENTESIS_ABRE
                scanner.getToken(); // primer operando
                String var1 = scanner.getLexema();
                scanner.getToken(); // MENOR_QUE
                scanner.getToken(); // segundo operando
                String var2 = scanner.getLexema();
                gci.agregarTriple(Triple.crearWhile(var1, var2));
                
            } else if (token == tipoID.PR && lexema.equals("println")) {
                scanner.getToken(); // PARENTESIS_ABRE
                scanner.getToken(); // identificador
                gci.agregarTriple(Triple.crearPrintln(scanner.getLexema()));
                
            } else if (token == tipoID.IDENTIFICADOR) {
                String id = scanner.getLexema();
                token = scanner.getToken();
                
                if (token == tipoID.IGUAL) {
                    token = scanner.getToken();
                    
                    if (token == tipoID.NUM_ENTERO) {
                        gci.agregarTriple(Triple.crearAsignacion(id, scanner.getLexema()));
                    } else if (token == tipoID.PR && (scanner.getLexema().equals("true") || 
                                 scanner.getLexema().equals("false"))) {
                        gci.agregarTriple(Triple.crearAsignacionBooleana(id, scanner.getLexema()));
                    } else if (token == tipoID.IDENTIFICADOR) {
                        String primerOperando = scanner.getLexema();
                        token = scanner.getToken();
                        
                        if (token == tipoID.MAS || token == tipoID.GUION || 
                            token == tipoID.ASTERISCO || token == tipoID.MENOR_QUE) {
                            String operador = Triple.tokenAOperador(token);
                            token = scanner.getToken();
                            if (token == tipoID.IDENTIFICADOR || token == tipoID.NUM_ENTERO) {
                                String segundoOperando = scanner.getLexema();
                                gci.agregarTriple(Triple.crearAsignacion(id, primerOperando));
                                gci.agregarTriple(Triple.crearOperacion(operador, id, segundoOperando));
                            }
                        } else {
                            gci.agregarTriple(Triple.crearAsignacion(id, primerOperando));
                        }
                    }
                }
            }
        } while (token != tipoID.EOF);
    }

    private void ejecutarCI() {
        if (semanticoActual == null) return;

        try {
            Compilador.GeneradorCodigoIntermedio gci = 
                new Compilador.GeneradorCodigoIntermedio(semanticoActual.getTablaSimbolos());

            generarTriples(gci); // Genera los triples
            
            gci.generar(); // Genera el Ensamblador
            textAreaIntermedio.setText(gci.getCodigo());
            textAreaObjeto.setText(""); // Limpiar área de objeto

        } catch (Exception ex) {
            textAreaErrores.setForeground(Color.RED);
            textAreaErrores.setBackground(new Color(255, 240, 240));
            textAreaErrores.setText("ERROR EN GENERACION DE CÓDIGO INTERMEDIO: " + ex.getMessage());
        }
    }
    
    private void ejecutarCO() {
        if (semanticoActual == null) {
            JOptionPane.showMessageDialog(this, "Debe ejecutar el analisis semántico (Semántico) primero.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Compilador.GeneradorCodigoIntermedio gci = 
                new Compilador.GeneradorCodigoIntermedio(semanticoActual.getTablaSimbolos());

            generarTriples(gci); 
            
            gci.generar();
            String codigoEnsamblador = gci.getCodigo();
            textAreaIntermedio.setText(codigoEnsamblador);

            // 2. GENERACIÓN DEL CÓDIGO OBJETO 
            Compilador.GeneradorCodigoObjeto gco = 
                new Compilador.GeneradorCodigoObjeto(semanticoActual.getTablaSimbolos(), codigoEnsamblador);
            String codigoObjetoBinario = gco.generarCodigoObjeto();
            textAreaObjeto.setText(codigoObjetoBinario);
            
            textAreaErrores.setForeground(new Color(0, 128, 0));
            textAreaErrores.setBackground(new Color(240, 255, 240));
            textAreaErrores.setText("GENERACION DE CODIGO OBJETO COMPLETA YIPEE!.");

            
        } catch (Exception ex) {
            textAreaErrores.setForeground(Color.RED);
            textAreaErrores.setBackground(new Color(255, 240, 240));
            textAreaErrores.setText("ERROR EN GENERACION DE CODIGO OBJETO: " + ex.getMessage());
        }
    }
/*
    + "\n\n" + 
                    "                      /^--^\\     /^--^\\     /^--^\\\n" +
"                      \\____/     \\____/     \\____/\n" +
"                     /      \\   /      \\   /      \\\n" +
"MV                  |        | |        | |        |\n" +
"                     \\__  __/   \\__  __/   \\__  __/\n" +
"|^|^|^|^|^|^|^|^|^|^|^|^\\ \\^|^|^|^/ /^|^|^|^|^\\ \\^|^|^|^|^|^|^|^|^|^|^|^|\n" +
"| | | | | | | | | | | | |\\ \\| | |/ /| | | | | | \\ \\ | | | | | | | | | | |\n" +
"########################/ /######\\ \\###########/ /#######################\n" +
"| | | | | | | | | | | | \\/| | | | \\/| | | | | |\\/ | | | | | | | | | | | |\n" +
"|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|_|"
    */

    @Override public void windowOpened(WindowEvent e) {}
    @Override public void windowClosing(WindowEvent e) {}
    @Override public void windowClosed(WindowEvent e) {}
    @Override public void windowIconified(WindowEvent e) {}
    @Override public void windowDeiconified(WindowEvent e) {}
    @Override public void windowActivated(WindowEvent e) {}
    @Override public void windowDeactivated(WindowEvent e) {}
}