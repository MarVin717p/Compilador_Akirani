package Compilador;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Scanner {

    private BufferedReader entrada;
    private String lineaActual = null;
    private int posicionActualEnLinea = 0;
    private int numLinea = 1;
    private String lexemaActual = "";
    private int tokenID = 0;
    
    public Scanner(File file) throws IOException {
        this.entrada = new BufferedReader(new FileReader(file));
        this.lineaActual = entrada.readLine();
    }

    private void avanzaLinea() throws IOException {
        lineaActual = entrada.readLine();
        posicionActualEnLinea = 0;
        numLinea++;
    }

    private char verCaracter() {
        if (lineaActual == null) {
            return '\n';
        }
        if (posicionActualEnLinea < lineaActual.length()) {
            return lineaActual.charAt(posicionActualEnLinea);
        } else {
            return '\n';
        }
    }

    private char advance() {
        char charActual = verCaracter();
        if (lineaActual != null && posicionActualEnLinea < lineaActual.length()) {
            posicionActualEnLinea++;
        }
        return charActual;
    }

    private void ignorarEspacios() throws IOException {
        while (true) {
            if (lineaActual == null) {
                break;
            }

            while (posicionActualEnLinea < lineaActual.length() && Character.isWhitespace(verCaracter())) {
                posicionActualEnLinea++;
            }

            if (posicionActualEnLinea >= lineaActual.length()) {
                avanzaLinea();
            } else {
                break;
            }
        }
    }

    public tipoID getToken() throws IOException {
        ignorarEspacios();

        if (lineaActual == null) {
            if (entrada != null) {
                entrada.close();
                entrada = null;
            }
            lexemaActual = "";
            tokenID = 0;
            return tipoID.EOF;
        }

        tipoID tokenClave = encontrarTokenClave();
        if (tokenClave != null) {
            return tokenClave;
        }


        tipoID numeroEnteroToken = encontrarNumeroEntero();
        if (numeroEnteroToken != null) {
            return numeroEnteroToken;
        }

        tipoID porCaracter = getPorCaracter();
        if (porCaracter != null) {
            return porCaracter;
        }

        lexemaActual = String.valueOf(verCaracter());
        advance();
        throw new RuntimeException(
                "Error léxico en la línea " + numLinea + ": token no reconocido '" + lexemaActual + "'"
        );
    }

    private tipoID encontrarTokenClave() {

        StringBuilder palabra = new StringBuilder();
        
         if (!Character.isLetter(verCaracter())) {
            return null;
        }
         
        while (Character.isLetterOrDigit(verCaracter())) {
            palabra.append(verCaracter());
            advance();
        }

        String str = palabra.toString();
        lexemaActual = str;

        switch (str) {
            case "class":
                tokenID = 1;
                return tipoID.PR;
            case "boolean":
                tokenID = 2;
                return tipoID.PR;
            case "int":
                tokenID = 3;
                return tipoID.PR;
            case "while":
                tokenID = 4;
                return tipoID.PR;
            case "true":
                tokenID = 5;
                return tipoID.PR;
            case "false":
                tokenID = 6;
                return tipoID.PR;
            case "println":
                tokenID = 7;
                return tipoID.PR;
            default:
                tokenID = 8;
                return tipoID.IDENTIFICADOR;
        }
    }

    private tipoID encontrarNumeroEntero() {
        if (!Character.isDigit(verCaracter())) {
            return null;
        }

        StringBuilder numero = new StringBuilder();
        while (Character.isDigit(verCaracter())) {
            numero.append(verCaracter());
            advance();
        }

        lexemaActual = numero.toString();
        tokenID = 9;
        return tipoID.NUM_ENTERO;
    }

    private tipoID getPorCaracter() {
        switch (verCaracter()) {
            case '{':
                advance();
                lexemaActual = "{";
                tokenID = 10;
                return tipoID.LLAVE_ABRE;
            case '}':
                advance();
                lexemaActual = "}";
                tokenID = 11;
                return tipoID.LLAVE_CIERRA;
            case '(':
                advance();
                lexemaActual = "(";
                tokenID = 12;
                return tipoID.PARENTESIS_ABRE;
            case ')':
                advance();
                lexemaActual = ")";
                tokenID = 13;
                return tipoID.PARENTESIS_CIERRA;
            case '<':
                advance();
                lexemaActual = "<";
                tokenID = 14;
                return tipoID.MENOR_QUE;
            case '+':
                advance();
                lexemaActual = "+";
                tokenID = 15;
                return tipoID.MAS;
            case '-':
                advance();
                lexemaActual = "-";
                tokenID = 16;
                return tipoID.GUION;
            case '*':
                advance();
                lexemaActual = "*";
                tokenID = 17;
                return tipoID.ASTERISCO;
            case ';':
                advance();
                lexemaActual = ";";
                tokenID = 18;
                return tipoID.PUNTOYCOMA;
            case '=':
                advance();
                lexemaActual = "=";
                tokenID = 19;
                return tipoID.IGUAL;
            default:
                return null;
        }
    }

    public int getNumeroDeLinea() {
        return numLinea;
    }

    public String getLexema() {
        return lexemaActual;
    }

    public static String getNombreToken(tipoID tipo) {
        if (tipo != null) {
            return tipo.toString();
        } else {
            return "Token erroneo";
        }
    }

    public int getTokenID() {
        return tokenID;
    }
    
}
