package Compilador;

import static Compilador.Parser.getNombreToken;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Semantico {

    private Scanner scanner;
    private tipoID sigToken;
    private int tokenNum;

    public static class InfoVariable {

        public String tipo;
        public String valor;
        public int direccion;

        public InfoVariable(String tipo, String valor, int direccion) {
            this.tipo = tipo;
            this.valor = valor;
            this.direccion = direccion;
        }
    }

    public static class ValorTipo {

        public String valor;
        public String tipo;
        public boolean esExpresion; // bandera

        public ValorTipo(String valor, String tipo, boolean esExpresion) {
            this.valor = valor;
            this.tipo = tipo;
            this.esExpresion = esExpresion;
        }
    }

    private Map<String, InfoVariable> tablaSimbolos = new LinkedHashMap<>();
    private int siguienteDireccion = 0;

    //MISMA ESTRUCTURA DEL PARSER
    public Semantico(File file) throws IOException {
        scanner = new Scanner(file);
        avanzar();
    }

    private void avanzar() throws IOException {
        sigToken = scanner.getToken();
        tokenNum = scanner.getTokenID();
    }

    private void eat(int numTokenEsperado) throws IOException {
        if (tokenNum == numTokenEsperado) {
            avanzar();
        } else {
            error("Se esperaba token '" + Parser.getNombreToken(numTokenEsperado)
                    + "' pero se encontró '" + Parser.getNombreToken(tokenNum) + "'");
        }
    }

    private void error(String msg) throws IOException {
        throw new IOException(" " + msg + " en línea " + scanner.getNumeroDeLinea());
    }

    public void analizarPrograma() throws IOException {
        eat(1); // CLASS
        eat(8); // IDENTIFICADOR

        while (tokenNum == 2 || tokenNum == 3) { // BOOLEAN O INT
            declaracionVariable();
        }

        eat(10); // {

        while (tokenNum != 11 && tokenNum != 0) { // MIENTRAS NO HAYA "}"
            analizarStatement();
        }

        if (tokenNum == 11) {
            eat(11);
        } else {
            error("Bloque no cerrado con '}'");
        }

        if (tokenNum != 0) {
            error("Se esperaba fin de archivo <EOF>");
        }
    }

    private void declaracionVariable() throws IOException {
        int tipoToken = tokenNum;
        eat(tipoToken);

        if (tokenNum == 8) { // IDENTIFICADOR
            String nombreVar = scanner.getLexema();
            String tipoStr;

            switch (tipoToken) {
                case 2:
                    tipoStr = "boolean";
                    break;
                case 3:
                    tipoStr = "int";
                    break;
                default:
                    error("Tipo desconocido");
                    tipoStr = "desconocido";
                    break;
            }

            if (tablaSimbolos.containsKey(nombreVar)) {
                error("Variable '" + nombreVar + "' ya declarada");
            } else {
                String valorInicial;
                int bits;
                if (tipoStr.equals("int")) {
                    valorInicial = "0";
                    bits = 2;
                } else {
                    valorInicial = "false";
                    bits = 1;
                }
                tablaSimbolos.put(nombreVar, new InfoVariable(tipoStr,valorInicial, siguienteDireccion));
                siguienteDireccion += bits;
            }

            eat(8);
        } else {
            error("Se esperaba un identificador después del tipo");
        }

        eat(18); // ;
    }

    private void analizarStatement() throws IOException {
        switch (tokenNum) {
            case 10: // {
                eat(10);
                while (tokenNum != 11 && tokenNum != 0) {
                    analizarStatement();
                }
                eat(11);
                break;

            case 4: // WHILE VERIFICACION
                eat(4);
                eat(12); // (
                ValorTipo tipoExpWhile = leerValorTipo();
                if (!tipoExpWhile.tipo.equals("boolean")) {
                    error("Expresión de while debe de dar un resultado boolean, "
                            + "se encontró " + tipoExpWhile.tipo);
                }
                eat(13); // )
                analizarStatement();
                break;

            case 7: // PRINTLN
                eat(7);
                eat(12); // (
                leerValorTipo();
                eat(13); // )
                eat(18); // ;
                break;

            //SEGUNDA VERIFICACION VARIABLES NO DECLARADAS
            case 8: // ASIGNACION
                String nombreVar = scanner.getLexema();
                if (!tablaSimbolos.containsKey(nombreVar)) {
                    error("Variable '" + nombreVar + "' no declarada");
                }
                String tipoVar = tablaSimbolos.get(nombreVar).tipo;
                eat(8); // identificador
                eat(19); // =
                ValorTipo valorExp = leerValorTipo();

                if (!tipoVar.equals(valorExp.tipo)) {
                    error("Error de tipo, se esperaba una operacion con " + tipoVar + " pero se se encuentra " + valorExp.tipo);
                }

                // Actualizar valor
                //if (!valorExp.esExpresion) {
                //    tablaSimbolos.get(nombreVar).valor = valorExp.valor;
                //}
                eat(18); // ;
                break;

            default:
                error("Token inesperado: " + tokenNum + " (lexema: " + scanner.getLexema() + ")");
        }
    }

    private ValorTipo leerValorTipo() throws IOException {
        String tipo, valor;
        boolean esExpresion = false;

        if (tokenNum == 8) { // IDENTIFICADOR
            String lexema = scanner.getLexema();
            if (!tablaSimbolos.containsKey(lexema)) {
                error("Variable '" + lexema + "' no declarada");
            }
            tipo = tablaSimbolos.get(lexema).tipo;
            valor = tablaSimbolos.get(lexema).valor;
            eat(8);

        } else if (tokenNum == 9) { // NUM_ENTERO
            tipo = "int";
            valor = scanner.getLexema();
            eat(9);

        } else if (tokenNum == 5) { // TRUE
            tipo = "boolean";
            valor = "true";
            eat(5);

        } else if (tokenNum == 6) { // FALSE
            tipo = "boolean";
            valor = "false";
            eat(6);

        } else {
            error("Expresión inválida");
            tipo = "desconocido";
            valor = "";
        }

        if (tokenNum == 14) {
            int op = tokenNum;
            eat(op);
            ValorTipo expresionDerecha = leerValorTipo();

            if (!tipo.equals("int") || !expresionDerecha.tipo.equals("int")) {
                error("Type mismatch: ambos operandos de la expresión relacional '<' deben ser de tipo int");
            }

            // El resultado de una expresión relacional es BOOLEAN
            tipo = "boolean";
            valor = "";
            esExpresion = true;
        }

        // MANEJO DE OPERADORES ARITMÉTICOS (+, -, *) 
        while (tokenNum >= 15 && tokenNum <= 17) {
            int op = tokenNum; // 15: +, 16: -, 17: *
            eat(op);
            ValorTipo expresionDerecha = leerValorTipo();

            // Verificación Semántica: Operandos deben ser int para la aritmética
            if (!tipo.equals("int") || !expresionDerecha.tipo.equals("int")) {
                error("Type mismatch: ambos operandos de la expresión aritmética deben ser de tipo int");
            }

            // El resultado de una expresión aritmética es INT
            tipo = "int";
            valor = "";
            esExpresion = true;
        }

        return new ValorTipo(valor, tipo, esExpresion);
    }

    public Map<String, InfoVariable> getTablaSimbolos() {
        return tablaSimbolos;
    }
}
