import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilidad de un solo uso para generar un hash BCrypt.
 *
 * Uso:
 *   1. Compilar y ejecutar (ver instrucciones abajo).
 *   2. Copiar el hash que imprime.
 *   3. Pegarlo en la BD con un UPDATE sobre sys_usuario.
 *
 * Cambie la variable 'password' por la contrasena que quiera.
 */
public class GenerarHash {
    public static void main(String[] args) {
        // ---- CAMBIE ESTA CONTRASENA POR LA QUE QUIERA USAR ----
        String password = "Admin2026SglAir";
        // -------------------------------------------------------

        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        System.out.println("Contrasena : " + password);
        System.out.println("Hash BCrypt: " + hash);
        System.out.println();
        System.out.println("SQL para actualizar el usuario admin:");
        System.out.println("UPDATE sys_usuario SET password_hash = '" + hash +
                            "' WHERE username = 'admin';");
    }
}
