import java.util.Map;

/** Serialización de respuestas; los formularios se reciben como URL encoded. */
public final class Json {
    private Json() { }
    public static String escribir(Object v) {
        if(v==null) { return "null"; }
        if(v instanceof Number || v instanceof Boolean) { return v.toString(); }
        if(v instanceof Map<?,?> m) {
            StringBuilder b=new StringBuilder("{");boolean primero=true;
            for(var e:m.entrySet()) { if(!primero) { b.append(','); }primero=false;b.append(escribir(e.getKey().toString())).append(':').append(escribir(e.getValue())); }return b.append('}').toString();
        }
        if(v instanceof Iterable<?> l) {
            StringBuilder b=new StringBuilder("[");boolean primero=true;
            for(Object item:l) { if(!primero) { b.append(','); }primero=false;b.append(escribir(item)); }return b.append(']').toString();
        }
        StringBuilder b=new StringBuilder("\"");
        for(char c:v.toString().toCharArray()) {
            switch(c) {
                case '\\' -> b.append("\\\\");case '"' -> b.append("\\\"");case '\n' -> b.append("\\n");case '\r' -> b.append("\\r");case '\t' -> b.append("\\t");
                default -> { if(c<32) { b.append(String.format("\\u%04x",(int)c)); }else { b.append(c); } }
            }
        }return b.append('"').toString();
    }
}
