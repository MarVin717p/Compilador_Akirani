package Compilador;

import java.io.File;
import java.io.IOException;

public class Parser {

    private Scanner scanner;
    private tipoID sigToken;
    private int tokenNum;

    public Parser(File file) throws IOException {
        scanner = new Scanner(file);
        avanzar();
    }

    // Avanza al siguiente tokenNum
    private void avanzar() throws IOException {
        sigToken = scanner.getToken();
        tokenNum = scanner.getTokenID();
    }

    private void eat(int numTokenEsperado) throws IOException {
        if (tokenNum == numTokenEsperado) {
            avanzar();
        } else {
            throw new RuntimeException("Error de sintaxis en línea "
                    + scanner.getNumeroDeLinea() + ": se esperaba el token "
                    + "'"+ getNombreToken(numTokenEsperado) + "'" + " pero se encontró " + "'" 
                    + getNombreToken(tokenNum) + "'" );
        }
    }

    // ----------------------- Gramatica -----------------------
    /*
    Program ::= "class" Identifier (VarDeclaration)* "{" (Statement)* "}" <EOF>
    VarDeclaration ::= Type Identifier ";"
    Type ::= "boolean" | "int"
    Statement ::= "{" (Statement)* "}"
              | "while" "(" Expression ")" Statement
              | "System.out.println" "(" Expression ")" ";"
              | Identifier "=" Expression ";"
    Expression ::= Identifier (("<" | "+" | "-" | "*") Identifier)
             | "true" | "false" | Identifier | Integer
    Identifier ::= letter (letter | digit)*
    Integer ::= digit+
    
     */
    // -----------------------------------------------------------

    // ----------------------- Tokens -----------------------
    public static String getNombreToken(int tokenNum) {
    switch (tokenNum) {
        case 0:  return "EOF";
        case 1:  return "class";
        case 2:  return "boolean";
        case 3:  return "int";
        case 4:  return "while";
        case 5:  return "true";
        case 6:  return "false";
        case 7:  return "println";
        case 8:  return "IDENTIFICADOR";
        case 9:  return "NUM_ENTERO";
        case 10: return "{";
        case 11: return "}";
        case 12: return "(";
        case 13: return ")";
        case 14: return "<";
        case 15: return "+";
        case 16: return "-";
        case 17: return "*";
        case 18: return ";";
        case 19: return "=";
        default: return "TOKEN_DESCONOCIDO";
        }
    }
    // ------------------------------------------------------
    
    // Program ::= class Identifier [ VarDeclaration ]* { Statement } <EOF>
    public void parseProgram() throws IOException {
        eat(1); 
        eat(8); 
        while (tokenNum == 2 || tokenNum == 3) { 
            parseVarDeclaration();
        }
        
        if (tokenNum != 10) { // {
        throw new RuntimeException("Se esperaba 'VarDeclaration' o '{'"
                + " después de class e identificador, pero se encontró " 
                + "'" +scanner.getLexema()+ "'");
    }   
        eat(10); 
        while (tokenNum != 11) { 
            parseStatement();
        }
        eat(11); 
        eat(0); 
    }

    // VarDeclaration ::= Type Identifier ;
    private void parseVarDeclaration() throws IOException {
        parseType();
        eat(8); 
        eat(18);
    }

    // Type ::= boolean | int
    private void parseType() throws IOException {
        if (tokenNum == 2 || tokenNum == 3) {
            eat(tokenNum);
        } else {
            throw new RuntimeException("Se esperaba un tipo (boolean/int) en línea "
                    + scanner.getNumeroDeLinea());
        }
    }

    // Statement ::= { [ Statement ]* }
    //             | while ( Expression ) Statement
    //             | println ( Expression ) ;
    //             | Identifier = Expression ;
    private void parseStatement() throws IOException {
        switch (tokenNum) {
            case 10:
                //statement
                eat(10);
                while (tokenNum != 11) { 
                    parseStatement();
                }   eat(11);
                break;
            case 4:
                //while
                eat(4);
                eat(12);
                parseExpression();
                eat(13);
                parseStatement();
                break;
            case 7:
                // println
                eat(7);
                eat(12); 
                parseExpression();
                eat(13); 
                eat(18); 
                break;
            case 8:
                // Asignacion "="
                eat(8);
                if (tokenNum != 19) { // =
                throw new RuntimeException("Se esperaba '=' después de identificador, se encontró "
                        + scanner.getLexema());
            }
                eat(19); 
                parseExpression();
                eat(18); 
                break;
            default:
                throw new RuntimeException("Error de sintaxis en Statement, token inesperado: "
                        + sigToken + " lexema: " + scanner.getLexema());
        }
    }

    // Expression ::= Identifier (< | + | - | * ) Identifier
    //              | true | false | Identifier | Integer
    private void parseExpression() throws IOException {
        if (tokenNum == 8) { 
            eat(8);
            if (tokenNum == 14 || tokenNum == 15 || tokenNum == 16 || tokenNum == 17) { // < + - *
                eat(tokenNum); 
                eat(8); // segundo IDENTIFICADOR
            }
        } else if (tokenNum == 5 || tokenNum == 6 || tokenNum == 8 || tokenNum == 9) {// true,false,identificador,numEntero
            eat(tokenNum);
        } else {
            throw new RuntimeException("Error de sintaxis en Expression, token inesperado: "
                    + sigToken + " lexema: " + scanner.getLexema());
        }
    }
}
