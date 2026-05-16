package gestiones_tunnel;

public class IpEncryptor {

    // Alfabeto de 26 letras mayúsculas
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    // Una clave simple para que el resultado no sea una conversión numérica directa
    private static final long SALT = 0x5A5A5A5AL;

    /**
     * Encripta una IP (String) a un código de 8 letras.
     */
    public static String encrypt(String ip) {
        long ipLong = ipToLong(ip);
        // Aplicamos un XOR simple para "ofuscar" el número
        long obfuscated = ipLong ^ SALT;
        return longToBase26(obfuscated);
    }

    /**
     * Desencripta un código de 8 letras a una IP (String).
     */
    public static String decrypt(String code) {
        long obfuscated = base26ToLong(code);
        long ipLong = obfuscated ^ SALT;
        return longToIp(ipLong);
    }

    // Convierte IP "192.168.1.1" a un long de 32 bits
    private static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (Long.parseLong(parts[i]) << (24 - (8 * i)));
        }
        return result & 0xFFFFFFFFL;
    }

    // Convierte el número a una cadena de 8 letras (Base 26)
    private static String longToBase26(long value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET.charAt((int) (value % 26)));
            value /= 26;
        }
        return sb.reverse().toString();
    }

    // Revierte la cadena de 8 letras a un número long
    private static long base26ToLong(String code) {
        long result = 0;
        for (int i = 0; i < code.length(); i++) {
            result = result * 26 + ALPHABET.indexOf(code.charAt(i));
        }
        return result;
    }

    // Convierte el long de vuelta al formato "0.0.0.0"
    private static String longToIp(long value) {
        return ((value >> 24) & 0xFF) + "." +
                ((value >> 16) & 0xFF) + "." +
                ((value >> 8) & 0xFF) + "." +
                (value & 0xFF);
    }

    public static void main(String[] args) {
        String originalIp = "192.168.15.240";
        String encrypted = encrypt(originalIp);
        String decrypted = decrypt(encrypted);

        System.out.println("IP Original:  " + originalIp);
        System.out.println("Código (8):   " + encrypted);
        System.out.println("IP Recuperada: " + decrypted);
    }
}