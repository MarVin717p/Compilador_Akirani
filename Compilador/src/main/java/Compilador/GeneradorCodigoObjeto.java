package Compilador;

import java.util.Map;
import java.util.HashMap;

public class GeneradorCodigoObjeto {

    private final Map<String, Semantico.InfoVariable> tablaSimbolos;
    private final String codigoEnsamblador;
    private final Map<String, Integer> offsetsEtiquetas = new HashMap<>(); 

    // Mapeo para registros de 1 byte
    private static final Map<String, String> REG_8_BITS = Map.of(
        "AL", "000", "CL", "001", "DL", "010", "BL", "011",
        "AH", "100", "CH", "101", "DH", "110", "BH", "111"
    );
    // Mapeo para registros de 2 bytes
    private static final Map<String, String> REG_16_BITS = Map.of(
        "AX", "000", "CX", "001", "DX", "010", "BX", "011",
        "SP", "100", "BP", "101", "SI", "110", "DI", "111"
    );

    public GeneradorCodigoObjeto(Map<String, Semantico.InfoVariable> ts, String codEnsamblador) {
        this.tablaSimbolos = ts;
        this.codigoEnsamblador = codEnsamblador;
    }

    public String generarCodigoObjeto() {
        calcularOffsetsEtiquetas();
        
        StringBuilder CodigoObjeto = new StringBuilder();
        CodigoObjeto.append("RESULTADO CODIGO OBJETO :D :\n\n");
        CodigoObjeto.append("Datos\n");
        CodigoObjeto.append(String.format("%-11s %-19s %s\n", "INSTRUCCION", "OFFSET", "CONTENIDO"));
        
        for (Map.Entry<String, Semantico.InfoVariable> entry : tablaSimbolos.entrySet()) {
            Semantico.InfoVariable info = entry.getValue();
            String nombre = entry.getKey();
            
            String tipo;
            if (info.tipo.equals("int")) {
                tipo = "DW ?";
            } else {
                tipo = "DB ?";
            }
            
            String offsetBinario = aBinario16(String.valueOf(info.direccion)); 
            String contenidoInicial;
            if (info.tipo.equals("int")) {
                contenidoInicial = "0000 0000 0000 0000";
            } else {
                contenidoInicial = "0000 0000";
            }
            
            CodigoObjeto.append(
                String.format("%-11s %-19s %s\n",
                    nombre + " " + tipo,
                    transformaOffsetANibble(offsetBinario),
                    contenidoInicial
                )
            );
        }
        CodigoObjeto.append("\nCodigo\n");
        CodigoObjeto.append(String.format("%-11s %-19s %s\n", "INSTRUCCION", "OFFSET", "CONTENIDO"));
        
        String[] lineas = codigoEnsamblador.split("\n");
        int offsetActual = 0x0000; 
        
        for (String linea : lineas) {
            String lineaDepurada = linea.trim();
            
            if (lineaDepurada.startsWith(";") || lineaDepurada.isEmpty() || 
                lineaDepurada.contains(":") || lineaDepurada.contains("PROC") || 
                lineaDepurada.contains(".DATA") || lineaDepurada.contains(".CODE") ||
                lineaDepurada.contains("END MAIN") || lineaDepurada.contains(".MODEL") ||
                lineaDepurada.contains(".STACK") || lineaDepurada.contains(".STARTUP") ||
                lineaDepurada.contains(".EXIT") || lineaDepurada.contains("ENDP") || lineaDepurada.contains("TITLE")) {
                continue;
            }
            
            int comentarioIndice = lineaDepurada.indexOf(';');
            if (comentarioIndice != -1) {
                lineaDepurada = lineaDepurada.substring(0, comentarioIndice).trim();
            }
            
            String instruccion = obtenerInstruccionYOperandos(lineaDepurada);
            String opCodeBinario = ObtenerOpCode(lineaDepurada, offsetActual);
            
            if (!opCodeBinario.isEmpty()) {
                String binarioFormateado = opCodeBinario.replaceAll(" ", "");
                int longitudBytes = binarioFormateado.length() / 8;
                
                String offsetBinario = aBinario16(String.valueOf(offsetActual));
                
                CodigoObjeto.append(
                    String.format("%-11s %-19s %s\n",
                        instruccion,
                        transformaOffsetANibble(offsetBinario),
                        contenidoANibble(opCodeBinario)
                           
                    )
                );
                offsetActual += longitudBytes;
            }
        }
        return CodigoObjeto.toString();
    }
    
    private void calcularOffsetsEtiquetas() {
        String[] lineas = codigoEnsamblador.split("\n");
        int offsetActual = 0x0000;

        for (String linea : lineas) {
            String lineaDepurada = linea.trim();
            
            if (lineaDepurada.endsWith(":")) {
                String etiqueta = lineaDepurada.substring(0, lineaDepurada.length() - 1);
                offsetsEtiquetas.put(etiqueta, offsetActual);
                continue; 
            }
            
            if (lineaDepurada.startsWith(";") || lineaDepurada.isEmpty() || 
                lineaDepurada.contains("PROC") || lineaDepurada.contains(".DATA") || 
                lineaDepurada.contains(".CODE") || lineaDepurada.contains(".STARTUP") ||
                lineaDepurada.contains(".EXIT") || lineaDepurada.contains("ENDP") || lineaDepurada.contains("TITLE")) {
                continue;
            }
            
            int comentarioIndice = lineaDepurada.indexOf(';');
            if (comentarioIndice != -1) {
                lineaDepurada = lineaDepurada.substring(0, comentarioIndice).trim();
            }
            if (lineaDepurada.isEmpty()) continue;
            
            String opCodeBinario = ObtenerOpCode(lineaDepurada, offsetActual);
            
            if (!opCodeBinario.isEmpty()) {
                String binarioLimpio = opCodeBinario.replaceAll(" ", "");
                int longitudBytes = binarioLimpio.length() / 8;
                offsetActual += longitudBytes;
            }
        }
    }
    
    private String ObtenerOpCode(String linea, int currentOffset) {
        if (linea.startsWith(";") || linea.isEmpty()) return "";
        
        if (linea.contains("MOV AH, 4CH")) return generarControl(linea);
        
        String[] partes = linea.split("\\s+", 2);
        if (partes.length == 0) return "";
        String instruccion = partes[0];
        String operandos = (partes.length > 1) ? partes[1] : "";

        switch(instruccion) {
            case "MOV": return generarMOV(linea);
            case "ADD": 
                if (operandos.contains("DL, '0'")) {
                    return generarADD_DL(linea); 
                }
                return generarADD(linea); 
            case "SUB": return generarSUB(linea);
            case "CMP": return generarCMP(linea);
            case "MUL": return generarMUL(linea);
            case "DIV": return generarDIV(linea);
            case "PUSH": return generarPUSH(linea);
            case "POP": return generarPOP(linea);
            case "XOR": 
                if (operandos.contains("DX, DX") || operandos.contains("DH, DH") || operandos.contains("DL, DL")) {
                    return generarXOR(linea);
                }
                return "";
            case "INC": return generarINC(linea);
            case "TEST": 
                if (operandos.contains("AX, AX") || operandos.contains("AL, AL") || operandos.contains("AH, AH")) {
                    return generarTEST(linea);
                }
                return "";
            case "JGE": 
            case "JMP": 
            case "CALL": 
            case "JNZ": 
            case "LOOP": 
                return generarJUMP(linea, currentOffset); 
            case "INT": 
            case "RET": 
                return generarControl(linea); 
            default: return "";
        }
    }
    
    private String getOpCodeDW(String opCodeBase, String direction, String width) {
        return opCodeBase + direction + width;
    }
    
    //Instrucciones
    
    private String generarMOV(String linea) {
        String[] partes = linea.substring(4).split(",\\s*");
        if (partes.length != 2) return "";

        String destino = partes[0].trim();
        String fuente = partes[1].trim();
        String opCode = "";
        
        String wFlag;
        if (esRegistro16(destino) || esRegistro16(fuente) || (esVariable(destino) && tablaSimbolos.get(destino).tipo.equals("int"))) {
            wFlag = "1";
        } else {
            wFlag = "0";
        }

        // caso 1: MOV Memoria, Valor Inmediato 
        if (esVariable(destino) && esNumero(fuente)) {
            opCode += "1100011" + wFlag; 
            opCode += generarModRmRegMem("00", "000", "110"); 
            
            opCode += aBinario16(getOffset(destino)); 
            
            if (wFlag.equals("1")) {
                opCode += codificarDesplazamiento16(fuente); 
            } else {
                opCode += aBinario8(fuente);
            }
            return opCode;
        }
        
        // sacamos el valor de los reg
        String regDest;
        if (esRegistro16(destino)) {
            regDest = REG_16_BITS.get(destino);
        } else {
            regDest = REG_8_BITS.get(destino);
        }
        String regFuente;
        if (esRegistro16(fuente)) {
            regFuente = REG_16_BITS.get(fuente);
        } else {
            regFuente = REG_8_BITS.get(fuente);
        }
        
        // caso 2: MOV Reg, Memoria. Reg es destino, D=1
        if (esRegistro(destino) && esVariable(fuente)) {
            String dirFlag = "1";
            opCode += getOpCodeDW("100010", dirFlag, wFlag); 
            opCode += generarModRmRegMem("00", regDest, "110"); 
            opCode += aBinario16(getOffset(fuente));
            return opCode;
        }
        
        // caso 3: MOV Memoria, Reg. Reg es fuente, D=0
        if (esVariable(destino) && esRegistro(fuente)) {
            String dirFlag = "0";
            opCode += getOpCodeDW("100010", dirFlag, wFlag); 
            opCode += generarModRmRegMem("00", regFuente, "110");
            opCode += aBinario16(getOffset(destino)); 
            return opCode;
        }
        
        // caso 4: MOV Reg, Inmediato
        if (esRegistro(destino) && esNumero(fuente)) {
            // OpCode: 1011 W RRR
            opCode += "1011" + wFlag + regDest; 
            
            if (wFlag.equals("1")) {
                opCode += codificarDesplazamiento16(fuente); 
            } else {
                opCode += aBinario8(fuente);
            }
            return opCode;
        }
        
        return "";
    }
    
    private String generarADD(String linea) {
        String[] partes = linea.substring(4).split(",\\s*");
        if (partes.length != 2) return "";
        String destino = partes[0].trim();
        String fuente = partes[1].trim();
        
        String wFlag;
        if (esRegistro16(destino) || (esVariable(fuente) && tablaSimbolos.get(fuente).tipo.equals("int"))) {
            wFlag = "1";
        } else {
            wFlag = "0";
        }
        
        String regCodeDest;
        if (esRegistro16(destino)) {
            regCodeDest = REG_16_BITS.get(destino);
        } else {
            regCodeDest = REG_8_BITS.get(destino);
        }
        
        // ADD Reg, Memoria. Reg es destino, D=1
        if (esRegistro(destino) && esVariable(fuente)) {
            String dirFlag = "1";
            String opCode = getOpCodeDW("000000", dirFlag, wFlag); // 000000dw
            opCode += generarModRmRegMem("00", regCodeDest, "110");
            opCode += aBinario16(getOffset(fuente)); 
            return opCode;
        }
        
        return "";
    }
    
    private String generarADD_DL(String linea) {
        return "10000000" + generarModRmRegMem("11", "000", "010") + "00110000";
    }

    private String generarSUB(String linea) {
        String[] partes = linea.substring(4).split(",\\s*");
        if (partes.length != 2) return "";
        String destino = partes[0].trim();
        String fuente = partes[1].trim();
        
        String wFlag;
        if (esRegistro16(destino) || (esVariable(fuente) && tablaSimbolos.get(fuente).tipo.equals("int"))) {
            wFlag = "1";
        } else {
            wFlag = "0";
        }

        String regCodeDest;
        if (esRegistro16(destino)) {
            regCodeDest = REG_16_BITS.get(destino);
        } else {
            regCodeDest = REG_8_BITS.get(destino);
        }
        
        // SUB Reg, Memoria. Reg es destino, D=1
        if (esRegistro(destino) && esVariable(fuente)) {
            String dirFlag = "1";
            String opCode = getOpCodeDW("001010", dirFlag, wFlag); // 001010dw
            opCode += generarModRmRegMem("00", regCodeDest, "110");
            opCode += aBinario16(getOffset(fuente)); 
            return opCode;
        }
        
        // SUB Reg, Reg. Reg es destino, D=1
        if (esRegistro(destino) && esRegistro(fuente)) {
            String regCodeFuente;
            if (esRegistro16(fuente)) {
                regCodeFuente = REG_16_BITS.get(fuente);
            } else {
                regCodeFuente = REG_8_BITS.get(fuente);
            }
            
            String dirFlag = "1";
            String opCode = getOpCodeDW("001010", dirFlag, wFlag); // 001010dw
            opCode += generarModRmRegMem("11", regCodeDest, regCodeFuente);
            return opCode;
        }
        return "";
    }

    private String generarMUL(String linea) {
        String[] partes = linea.substring(4).split("\\s+");
        if (partes.length < 1) return "";
        String operando = partes[0].trim().replace(",", "");

        String wFlag;
        if (esRegistro16(operando)) {
            wFlag = "1";
        } else {
            wFlag = "0";
        }

        String rrr;
        if (esRegistro16(operando)) {
            rrr = REG_16_BITS.get(operando);
        } else {
            rrr = REG_8_BITS.get(operando);
        }
        
        if (esRegistro(operando)) {
            // OpCode 1111011w
            String opCode = "1111011" + wFlag;
            // Mod=11, Reg=100, R/M=rrr
            return opCode + generarModRmRegMem("11", "100", rrr);
        }
        return "";
    }

    private String generarDIV(String linea) {
        String[] partes = linea.substring(4).split("\\s+");
        if (partes.length < 1) return "";
        String operando = partes[0].trim().replace(",", "");

        String wFlag;
        if (esRegistro16(operando)) {
            wFlag = "1";
        } else {
            wFlag = "0";
        }
        String rrr;
        if (esRegistro16(operando)) {
            rrr = REG_16_BITS.get(operando);
        } else {
            rrr = REG_8_BITS.get(operando);
        }
        
        if (esRegistro(operando)) {
            // OpCode 1111011w
            String opCode = "1111011" + wFlag;
            // Mod=11, Reg=110, R/M=rrr
            return opCode + generarModRmRegMem("11", "110", rrr);
        }
        return "";
    }

    private String generarCMP(String linea) {
        String[] partes = linea.substring(4).split(",\\s*");
        if (partes.length != 2) return "";
        String destino = partes[0].trim();
        String fuente = partes[1].trim();
        
        String wFlag;
        if (esRegistro16(destino) || (esVariable(fuente) && tablaSimbolos.get(fuente).tipo.equals("int"))) {
            wFlag = "1";
        } else {
            wFlag = "0";
        }

        String regCodeDest;
        if (esRegistro16(destino)) {
            regCodeDest = REG_16_BITS.get(destino);
        } else {
            regCodeDest = REG_8_BITS.get(destino);
        }
        
        // CMP Reg, Memoria. Reg es destino, D=1
        if (esRegistro(destino) && esVariable(fuente)) {
            String dirFlag = "1";
            String opCode = getOpCodeDW("001110", dirFlag, wFlag); // 001110dw
            opCode += generarModRmRegMem("00", regCodeDest, "110");
            opCode += aBinario16(getOffset(fuente));
            return opCode;
        }
        
        // CMP Reg, Reg. Reg es destino, D=1
        if (esRegistro(destino) && esRegistro(fuente)) {
            String regCodeFuente;
            if (esRegistro16(fuente)) {
                regCodeFuente = REG_16_BITS.get(fuente);
            } else {
                regCodeFuente = REG_8_BITS.get(fuente);
            }

            String dirFlag = "1";
            String opCode = getOpCodeDW("001110", dirFlag, wFlag); // 001110dw
            opCode += generarModRmRegMem("11", regCodeDest, regCodeFuente);
            return opCode;
        }
        return "";
    }
    
    private String generarPUSH(String linea) {
        String reg = linea.substring(5).trim();
        String rrr = REG_16_BITS.getOrDefault(reg, "000"); 
        if (!esRegistro16(reg)) return "";
        return "01010" + rrr;
    }

    private String generarPOP(String linea) {
        String reg = linea.substring(4).trim();
        String rrr = REG_16_BITS.getOrDefault(reg, "000"); 
        if (!esRegistro16(reg)) return "";
        return "01011" + rrr;
    }
    
    private String generarXOR(String linea) {
        String reg = linea.substring(4).split(",\\s*")[0].trim();
        
        String wFlag;
        if (esRegistro16(reg)) {
            wFlag = "1";
        } else {
            wFlag = "0";
        }

        String rrr;
        if (esRegistro16(reg)) {
            rrr = REG_16_BITS.get(reg);
        } else {
            rrr = REG_8_BITS.get(reg);
        }
        
        String opCode = getOpCodeDW("001100", "1", wFlag);
        return opCode + generarModRmRegMem("11", rrr, rrr);
    }
    
    private String generarINC(String linea) {
        String reg = linea.substring(4).trim();
        
        String wFlag;
        if (esRegistro16(reg)) {
            wFlag = "1";
        } else {
            wFlag = "0";
        }

        String rrr;
        if (esRegistro16(reg)) {
            rrr = REG_16_BITS.get(reg);
        } else {
            rrr = REG_8_BITS.get(reg);
        }
        
        if (esRegistro(reg)) {
            String opCode = "1111111" + wFlag;
            return opCode + generarModRmRegMem("11", "000", rrr);
        }
        return "";
    }
    
    private String generarTEST(String linea) {
        String reg = linea.substring(5).split(",\\s*")[0].trim();
        
        String wFlag;
        if (esRegistro16(reg)) {
            wFlag = "1";
        } else {
            wFlag = "0";
        }

        String rrr;
        if (esRegistro16(reg)) {
            rrr = REG_16_BITS.get(reg);
        } else {
            rrr = REG_8_BITS.get(reg);
        }
        
        String opCode = "1000011" + wFlag;
        return opCode + generarModRmRegMem("11", "000", rrr);
    }

    private String generarJUMP(String linea, int offsetActual) {
        String[] partes = linea.split("\\s+");
        if (partes.length != 2) return "";
        String instr = partes[0];
        String etiqueta = partes[1];
        String opCode; 

        if (!offsetsEtiquetas.containsKey(etiqueta)) {
            if (instr.equals("CALL") || instr.equals("JMP")) {
                opCode = "11101000"; 
            } else {
                opCode = "01110101"; 
            }
            return opCode + "0000000000000000"; 
        }

        int offsetDestino = offsetsEtiquetas.get(etiqueta);
        
        int longitudInstruccion;
        
        if (instr.equals("JGE") || instr.equals("JNZ") || instr.equals("LOOP")) {
            longitudInstruccion = 2; // Salto corto
        } else {
            longitudInstruccion = 3; // OpCode con desplazamiento de 16-bit 
        }

        int desplazamiento = offsetDestino - (offsetActual + longitudInstruccion);

        if (instr.equals("JGE") || instr.equals("JNZ") || instr.equals("LOOP")) {
            // Salto corto (8 bits)
            switch (instr) {
                case "JGE": opCode = "01111101"; break;
                case "JNZ": opCode = "01110101"; break;
                case "LOOP": opCode = "11100010"; break;
                default: opCode = "";
            }
            return opCode + aBinario8(String.valueOf(desplazamiento)); 
        } else {
            if (instr.equals("CALL")) {
                opCode = "11101000";
            } else { 
                opCode = "11101001";
            }
            
            return opCode + codificarDesplazamiento16(String.valueOf(desplazamiento));
        }
    }

    private String generarControl(String linea) {
        String instr = linea.split("\\s+")[0];

        if (linea.contains("MOV AH, 4CH")) {
            return "10110100" + "01001100";
        }

        switch(instr) {
            case "INT": 
                if (linea.contains("21H") || linea.contains("21h")) {
                    return "11001101" + "00100001"; //  opcode y 21H
                }
                break;
            case "RET": 
                return "11000011"; // opcode
            default: return "";
        }
        return "";
    }
    
    private String generarModRmRegMem(String mod, String reg, String r_m) {
        return mod + reg + r_m;
    }

    private String getOffset(String var) {
        Semantico.InfoVariable info = tablaSimbolos.get(var);
        if (info != null) {
            return String.valueOf(info.direccion);
        }
        return "0";
    }
    
    private String transformaOffsetANibble(String binario16) {
        if (binario16.length() != 16) return binario16;
        
        StringBuilder sb = new StringBuilder();
        
        // Byte Alto
        sb.append(binario16.substring(0, 4)).append(" ");
        sb.append(binario16.substring(4, 8)).append(" ");
        // Byte Bajo
        sb.append(binario16.substring(8, 12)).append(" ");
        sb.append(binario16.substring(12, 16));
        
        return sb.toString();
    }

    private String contenidoANibble(String binario) {
        StringBuilder sb = new StringBuilder();
        String binarioLimpio = binario.replaceAll("\\[disp_rel_16bit\\]", "").replaceAll("\\[disp_16bit\\]", "").replaceAll(" ", "");

        for (int i = 0; i < binarioLimpio.length(); i += 4) {
            String nibble = binarioLimpio.substring(i, Math.min(i + 4, binarioLimpio.length()));
            sb.append(nibble);
            
            if (i + 4 < binarioLimpio.length()) {
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String obtenerInstruccionYOperandos(String linea) {
        String[] partes = linea.split("\\s+", 2);
        if (partes.length == 2) {
            return partes[0] + " " + partes[1].replace(", ", ",");
        }
        return linea;
    }

    private boolean esNumero(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean esRegistro16(String str) {
        return REG_16_BITS.containsKey(str);
    }
    
    private boolean esRegistro8(String str) {
        return REG_8_BITS.containsKey(str);
    }
    
    private boolean esRegistro(String str) {
        return esRegistro16(str) || esRegistro8(str);
    }
    
    private boolean esVariable(String str) {
        return tablaSimbolos.containsKey(str);
    }

    private String aBinario16(String str) {
        try {
            int valor = Integer.parseInt(str);
            String binario16 = String.format("%16s", Integer.toBinaryString(valor & 0xFFFF)).replace(' ', '0');
            
            return binario16; 
        } catch (NumberFormatException e) {
            return "0000000000000000";
        }
    }
    
    private String codificarDesplazamiento16(String str) {
          try {
            String binario16 = aBinario16(str); //obtenemos big endian osea el normalito 
            
            // Convertir a little endian
            String msb = binario16.substring(0, 8);
            String lsb = binario16.substring(8);
            
            return lsb + msb;
        } catch (NumberFormatException e) {
            return "0000000000000000";
        }
    }
    
    private String aBinario8(String str) {
        try {
            int valor = Integer.parseInt(str);
            String binario8 = String.format("%8s", Integer.toBinaryString(valor & 0xFF)).replace(' ', '0');
            return binario8; 
        } catch (NumberFormatException e) {
            return "00000000"; 
        }
    }
}