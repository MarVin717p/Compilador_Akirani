package Compilador;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeneradorCodigoIntermedio {

    private final Map<String, Semantico.InfoVariable> tablaSimbolos;
    private final List<Triple> triples = new ArrayList<>();
    private final StringBuilder codigoFinal = new StringBuilder();
    private int etiquetaWhile = 0;

    public GeneradorCodigoIntermedio(Map<String, Semantico.InfoVariable> tablaSimbolos) {
        this.tablaSimbolos = tablaSimbolos;
    }

    public void agregarTriple(Triple t) {
        triples.add(t);
    }

    public void generar() {
        generarEncabezado();
        generarSegmentoDatos();
        generarSegmentoCodigo();
    }

    private void generarEncabezado() {
        codigoFinal.append("\t\tTITLE Programa\n");
        codigoFinal.append("\t\t.MODEL SMALL\n");
        codigoFinal.append("\t\t.STACK 100h\n");
    }

    private void generarSegmentoDatos() {
        codigoFinal.append("\t\t.DATA\n");

        for (Map.Entry<String, Semantico.InfoVariable> entry : tablaSimbolos.entrySet()) {
            String id = entry.getKey();
            Semantico.InfoVariable info = entry.getValue();
            if (info.tipo.equals("int")) {
                codigoFinal.append(String.format("%-8s DW\t? \t; Variable %s\n", id, id));
            } else { // boolean
                codigoFinal.append(String.format("%-8s DB\t ? \t; Variable boolean %s\n", id, id));
            }
        }
        codigoFinal.append("\n");
    }

    private void generarSegmentoCodigo() {
        codigoFinal.append("\t\t.CODE\n");
        codigoFinal.append("MAIN\tPROC\tFAR\n");
        codigoFinal.append("\t\t.STARTUP\n\n");

        for (int i = 0; i < triples.size(); i++) {
            Triple t = triples.get(i);
            switch (t.operador) {
                case "while":
                    String etiqueta = "WHILE_" + etiquetaWhile;
                    codigoFinal.append(String.format("%s_START:\n", etiqueta));
                    // condicion 
                    codigoFinal.append("\tMOV AX, " + t.arg1 + "\n");
                    codigoFinal.append("\tCMP AX, " + t.arg2 + "\n");
                    codigoFinal.append(String.format("\tJGE %s_END\n\n", etiqueta));
                    i++;
                    while (i < triples.size() && !triples.get(i).operador.equals("while")) {
                        Triple tCuerpo = triples.get(i);
                        switch (tCuerpo.operador) {
                            case "=":
                                generarAsignacion(tCuerpo);
                                break;
                            case "+":
                                generarOperacion(tCuerpo);
                                break;
                            case "println":
                                codigoFinal.append("\t; println(" + tCuerpo.arg1 + ")\n");
                                if (esVariableBooleana(tCuerpo.arg1)) {
                                    codigoFinal.append("\tMOV AL, " + tCuerpo.arg1 + "\n");
                                    codigoFinal.append("\tMOV AH, 0\n");  
                                    codigoFinal.append("\tCALL PrintNumber\n");
                                } else {
                                    codigoFinal.append("\tMOV AX, " + tCuerpo.arg1 + "\n");
                                    codigoFinal.append("\tCALL PrintNumber\n");
                                }
                                // retorno de carro 13
                                codigoFinal.append("\tMOV DL, 13\n");
                                codigoFinal.append("\tMOV AH, 02h\n");
                                codigoFinal.append("\tINT 21h\n");
                                // salto de línea 10
                                codigoFinal.append("\tMOV DL, 10\n");
                                codigoFinal.append("\tMOV AH, 02h\n");
                                codigoFinal.append("\tINT 21h\n");
                                codigoFinal.append("\n");
                                break;
                        }
                        i++;
                    }
                    i--; // Retroceder uno porque el for lo avanza
                    // Volver al inicio del while
                    codigoFinal.append(String.format("\tJMP %s_START\n", etiqueta));
                    codigoFinal.append(String.format("%s_END:\n\n", etiqueta));
                    etiquetaWhile++;
                    break;

                case "=":
                    generarAsignacion(t);
                    break;
                case "+":
                case "-":
                case "*":
                case "<":
                    generarOperacion(t);
                    break;
                case "println":
                    //comentario
                    codigoFinal.append("\t; println(" + t.arg1 + ")\n");
                    
                    if (esVariableBooleana(t.arg1)) {
                        codigoFinal.append("\tMOV AL, " + t.arg1 + "\n");
                        codigoFinal.append("\tMOV AH, 0\n");  
                        codigoFinal.append("\tCALL PrintNumber\n");
                    } else {
                        codigoFinal.append("\tMOV AX, " + t.arg1 + "\n");
                        codigoFinal.append("\tCALL PrintNumber\n");
                    }
                    // retorno de carro 13
                    codigoFinal.append("\tMOV DL, 13\n");
                    codigoFinal.append("\tMOV AH, 02h\n");
                    codigoFinal.append("\tINT 21h\n");
                    // salto de línea 10
                    codigoFinal.append("\tMOV DL, 10\n");
                    codigoFinal.append("\tMOV AH, 02h\n");
                    codigoFinal.append("\tINT 21h\n");
                    codigoFinal.append("\n");
                    break;
            }
        }

        // Terminar programa
        codigoFinal.append("\t; Terminar el programa\n");
        codigoFinal.append("\tMOV AH, 4CH\n");
        codigoFinal.append("\tINT 21H\n");
        codigoFinal.append("\t.EXIT\n");
        codigoFinal.append("MAIN ENDP\n");

        // Agregar procedimientos si hay println
        if (triples.stream().anyMatch(t -> t.operador.equals("println"))) {
            generarProcedimientosDePrint();
        }

        codigoFinal.append("END MAIN\n");
    }

    private void generarAsignacion(Triple t) {
        //comentario
        codigoFinal.append(String.format("\t; %s = %s\n", t.arg1, t.arg2));
        
        if (esVariableBooleana(t.arg1)) {
            if (t.arg2.equals("true") || t.arg2.equals("false")) {
                String valor;
                if (t.arg2.equals("true")) {
                    valor = "1";
                } else {
                    valor = "0";
                }
                codigoFinal.append(String.format("\tMOV %s, %s\n", t.arg1, valor));
            } else {
                codigoFinal.append(String.format("\tMOV AL, %s\n", t.arg2));
                codigoFinal.append(String.format("\tMOV %s, AL\n", t.arg1));
            }
        } else {
            if (esNumero(t.arg2)) {
                codigoFinal.append(String.format("\tMOV %s, %s\n", t.arg1, t.arg2));
            } else {
                codigoFinal.append(String.format("\tMOV AX, %s\n", t.arg2));
                codigoFinal.append(String.format("\tMOV %s, AX\n", t.arg1));
            }
        }
        codigoFinal.append("\n");
    }

    private void generarOperacion(Triple t) {
        //comentario
        codigoFinal.append(String.format("\t; %s %s %s\n", t.arg1, t.operador, t.arg2));
        switch (t.operador) {
            case "+":
                codigoFinal.append(String.format("\tMOV AX, %s\n", t.arg1));
                codigoFinal.append(String.format("\tADD AX, %s\n", t.arg2));
                codigoFinal.append(String.format("\tMOV %s, AX\n", t.arg1));
                break;
            case "-":
                codigoFinal.append(String.format("\tMOV AX, %s\n", t.arg1));
                codigoFinal.append(String.format("\tSUB AX, %s\n", t.arg2));
                codigoFinal.append(String.format("\tMOV %s, AX\n", t.arg1));
                break;
            case "*":
                codigoFinal.append(String.format("\tMOV AX, %s\n", t.arg1));
                codigoFinal.append(String.format("\tMOV BX, %s\n", t.arg2));
                codigoFinal.append("\tMUL BX\n");
                codigoFinal.append(String.format("\tMOV %s, AX\n", t.arg1));
                break;
            case "<":
                codigoFinal.append(String.format("\tMOV AX, %s\n", t.arg1));
                codigoFinal.append(String.format("\tCMP AX, %s\n", t.arg2));
                codigoFinal.append("\tMOV AL, 0\n");  // Falso por defecto
                codigoFinal.append("\tJGE COMPARACION_FIN\n");
                codigoFinal.append("\tMOV AL, 1\n");  // Verdadero si es menor
                codigoFinal.append("COMPARACION_FIN:\n");
                codigoFinal.append(String.format("\tMOV %s, AL\n", t.arg1));
                break;
        }
        codigoFinal.append("\n");
    }

    private void generarProcedimientosDePrint() {
        codigoFinal.append("PrintNumber PROC\n");
        codigoFinal.append("\tPUSH AX\n");
        codigoFinal.append("\tPUSH BX\n");
        codigoFinal.append("\tPUSH CX\n");
        codigoFinal.append("\tPUSH DX\n");
        codigoFinal.append("\tMOV CX, 0\n");
        codigoFinal.append("\tMOV BX, 10\n");
        codigoFinal.append("CONVERT:\n");
        codigoFinal.append("\tXOR DX, DX\n");
        codigoFinal.append("\tDIV BX\n");
        codigoFinal.append("\tPUSH DX\n");
        codigoFinal.append("\tINC CX\n");
        codigoFinal.append("\tTEST AX, AX\n");
        codigoFinal.append("\tJNZ CONVERT\n");
        codigoFinal.append("PRINT_LOOP:\n");
        codigoFinal.append("\tPOP DX\n");
        codigoFinal.append("\tADD DL, '0'\n");
        codigoFinal.append("\tMOV AH, 02h\n");
        codigoFinal.append("\tINT 21h\n");
        codigoFinal.append("\tLOOP PRINT_LOOP\n");
        codigoFinal.append("\tPOP DX\n");
        codigoFinal.append("\tPOP CX\n");
        codigoFinal.append("\tPOP BX\n");
        codigoFinal.append("\tPOP AX\n");
        codigoFinal.append("\tRET\n");
        codigoFinal.append("PrintNumber ENDP\n\n");
    }

    private boolean esNumero(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean esVariableBooleana(String nombreVariable) {
        Semantico.InfoVariable info = tablaSimbolos.get(nombreVariable);
        return info != null && info.tipo.equals("boolean");
    }

    public String getCodigo() {
        return codigoFinal.toString();
    }
}