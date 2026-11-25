package co.edu.sena.skillup.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Cifra y verifica contraseñas con PBKDF2-HMAC-SHA256 y una sal aleatoria por usuario.
 *
 * <p>El resultado se guarda en la columna {@code usuario.contrasena_hash} con el formato
 * {@code pbkdf2_sha256$iteraciones$sal$hash}, donde la sal y el hash van en Base64.
 * Solo utiliza clases del JDK, sin dependencias externas.</p>
 */
public class CifradorContrasena {

    /** Iteraciones recomendadas por OWASP para PBKDF2-HMAC-SHA256. */
    public static final int ITERACIONES_POR_DEFECTO = 600_000;

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO = "pbkdf2_sha256";
    private static final String SEPARADOR = "$";
    private static final String SEPARADOR_REGEX = "\\$";
    private static final int PARTES_HASH = 4;
    private static final int LONGITUD_SAL_BYTES = 16;
    private static final int LONGITUD_HASH_BITS = 256;

    private final int iteraciones;
    private final SecureRandom generadorAleatorio = new SecureRandom();

    /**
     * Crea el cifrador con las iteraciones recomendadas.
     */
    public CifradorContrasena() {
        this(ITERACIONES_POR_DEFECTO);
    }

    /**
     * Crea el cifrador con un número de iteraciones específico.
     *
     * @param iteraciones número de iteraciones de PBKDF2; debe ser positivo
     */
    public CifradorContrasena(int iteraciones) {
        if (iteraciones < 1) {
            throw new IllegalArgumentException("Las iteraciones deben ser positivas");
        }
        this.iteraciones = iteraciones;
    }

    /**
     * Genera el hash de una contraseña con una sal nueva.
     *
     * @param contrasena contraseña en texto plano
     * @return hash en formato {@code pbkdf2_sha256$iteraciones$sal$hash}
     */
    public String cifrar(String contrasena) {
        Objects.requireNonNull(contrasena, "La contraseña es obligatoria");
        byte[] sal = new byte[LONGITUD_SAL_BYTES];
        generadorAleatorio.nextBytes(sal);
        byte[] hash = derivarClave(contrasena, sal, iteraciones);
        Base64.Encoder codificador = Base64.getEncoder();
        return String.join(SEPARADOR, PREFIJO, String.valueOf(iteraciones),
                codificador.encodeToString(sal), codificador.encodeToString(hash));
    }

    /**
     * Comprueba si una contraseña corresponde al hash almacenado.
     *
     * @param contrasena contraseña en texto plano digitada por el usuario
     * @param hashAlmacenado valor de la columna {@code contrasena_hash}
     * @return {@code true} si la contraseña es correcta
     */
    public boolean verificar(String contrasena, String hashAlmacenado) {
        if (contrasena == null || hashAlmacenado == null) {
            return false;
        }
        String[] partes = hashAlmacenado.split(SEPARADOR_REGEX);
        if (partes.length != PARTES_HASH || !PREFIJO.equals(partes[0])) {
            return false;
        }
        try {
            int iteracionesAlmacenadas = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] hashEsperado = Base64.getDecoder().decode(partes[3]);
            byte[] hashCalculado = derivarClave(contrasena, sal, iteracionesAlmacenadas);
            return MessageDigest.isEqual(hashEsperado, hashCalculado);
        } catch (IllegalArgumentException e) {
            // Hash con formato dañado: se trata como contraseña incorrecta.
            return false;
        }
    }

    private static byte[] derivarClave(String contrasena, byte[] sal, int iteraciones) {
        PBEKeySpec especificacion = new PBEKeySpec(
                contrasena.toCharArray(), sal, iteraciones, LONGITUD_HASH_BITS);
        try {
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance(ALGORITMO);
            return fabrica.generateSecret(especificacion).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("El algoritmo " + ALGORITMO + " no está disponible", e);
        } finally {
            especificacion.clearPassword();
        }
    }
}
