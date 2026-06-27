package Compilador;

public class Triple {
    public String operador;
    public String arg1;
    public String arg2;

    public Triple(String operador, String arg1, String arg2) {
        this.operador = operador;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public static Triple crearAsignacion(String id, String valor) {
        return new Triple("=", id, valor);
    }

    public static Triple crearAsignacionBooleana(String id, String valor) {
        if (!valor.equals("true") && !valor.equals("false")) {
            throw new RuntimeException("Valor booleano inválido: " + valor);
        }
        return new Triple("=", id, valor);
    }

    public static Triple crearOperacion(String operador, String id, String segundoOperando) {
        return new Triple(operador, id, segundoOperando);
    }

    public static Triple crearWhile(String var1, String var2) {
        return new Triple("while", var1, var2);
    }

    public static Triple crearPrintln(String varPrint) {
        return new Triple("println", varPrint, "");
    }

    public static String tokenAOperador(tipoID token) {
        switch (token) {
            case MAS:
                return "+";
            case GUION:
                return "-";
            case ASTERISCO:
                return "*";
            case MENOR_QUE:
                return "<";
            default:
                throw new RuntimeException("Operador no encontrado");
        }
    }

    @Override
    public String toString() {
        return "(" + operador + ", " + (arg1 != null ? arg1 : "") + ", " + (arg2 != null ? arg2 : "") + ")";
    }
}