package models;

import config.Conexion;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de la capa MODELO (MVC) para la entidad usuario.
 * Encapsula toda la logica de acceso a datos relacionada con
 * autenticacion: creacion de usuarios, validacion de
 * contrasenas con BCrypt y consulta de roles y permisos.
 *
 * Corresponde al Issue #0 (Gestion de Seguridad y Roles).
 *
 * Tablas involucradas: sys_usuario, sys_usuario_rol, sys_rol,
 * sys_rol_permiso, sys_permiso.
 */
public class Usuario {

    private int idUsuario;
    private String username;
    private String email;
    private boolean activo;

    // El hash de la contrasena se mantiene privado y NUNCA se
    // expone en getters. Solo se usa internamente para validar.
    private String passwordHash;

    // ----- Getters basicos -----
    public int getIdUsuario()  { return idUsuario; }
    public String getUsername() { return username; }
    public String getEmail()    { return email; }
    public boolean isActivo()   { return activo; }


    /**
     * Crea un nuevo usuario en la base de datos.
     * La contrasena se recibe en texto plano y se almacena
     * SIEMPRE como hash BCrypt; el texto plano nunca toca la BD.
     *
     * @param username       nombre de usuario unico
     * @param passwordPlano  contrasena en texto plano
     * @param email          correo unico
     * @return el id generado para el nuevo usuario
     * @throws SQLException si el username o email ya existen, o
     *                      ante cualquier error de BD
     */
    public static int crear(String username, String passwordPlano, String email)
            throws SQLException {

        // BCrypt.gensalt(12) -> factor de coste 12. Cuanto mayor,
        // mas lento de calcular y mas resistente a fuerza bruta.
        String hash = BCrypt.hashpw(passwordPlano, BCrypt.gensalt(12));

        String sql = "INSERT INTO sys_usuario (username, password_hash, email, activo) " +
                     "VALUES (?, ?, ?, 1)";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(
                     sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, email);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            throw new SQLException("No se pudo obtener el id del usuario creado.");
        }
    }


    /**
     * Busca un usuario por su username.
     *
     * @param username nombre de usuario a buscar
     * @return el objeto Usuario, o null si no existe
     */
    public static Usuario obtenerPorUsername(String username) throws SQLException {
        String sql = "SELECT id_usuario, username, password_hash, email, activo " +
                     "FROM sys_usuario WHERE username = ?";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.idUsuario    = rs.getInt("id_usuario");
                    u.username     = rs.getString("username");
                    u.passwordHash = rs.getString("password_hash");
                    u.email        = rs.getString("email");
                    u.activo       = rs.getBoolean("activo");
                    return u;
                }
            }
        }
        return null; // no encontrado
    }


    /**
     * Valida una contrasena en texto plano contra el hash BCrypt
     * almacenado para este usuario.
     *
     * BCrypt.checkpw vuelve a hashear el texto plano con el mismo
     * salt embebido en el hash y compara. Nunca se "descifra" el
     * hash: BCrypt es unidireccional.
     *
     * @param passwordPlano contrasena ingresada por el usuario
     * @return true si coincide; false si no
     */
    public boolean validarPassword(String passwordPlano) {
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(passwordPlano, passwordHash);
    }


    /**
     * Devuelve los nombres de los roles asignados a este usuario.
     * Ej: ["Administrador"] o ["Secretaria AIR"].
     */
    public List<String> obtenerRoles() throws SQLException {
        String sql = "SELECT r.nombre_rol " +
                     "FROM sys_usuario_rol ur " +
                     "JOIN sys_rol r ON ur.id_rol = r.id_rol " +
                     "WHERE ur.id_usuario = ?";

        List<String> roles = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, this.idUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roles.add(rs.getString("nombre_rol"));
                }
            }
        }
        return roles;
    }


    /**
     * Devuelve los nombres de los permisos efectivos de este
     * usuario, resolviendo la cadena usuario -> rol -> permiso.
     * Se usa SELECT DISTINCT porque si el usuario tuviera varios
     * roles, un mismo permiso podria aparecer repetido.
     *
     * Ej: ["GESTIONAR_USUARIOS", "REGISTRAR_ASAMBLEISTAS", ...]
     */
    public List<String> obtenerPermisos() throws SQLException {
        String sql = "SELECT DISTINCT p.nombre_permiso " +
                     "FROM sys_usuario_rol ur " +
                     "JOIN sys_rol_permiso rp ON ur.id_rol = rp.id_rol " +
                     "JOIN sys_permiso p ON rp.id_permiso = p.id_permiso " +
                     "WHERE ur.id_usuario = ?";

        List<String> permisos = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, this.idUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    permisos.add(rs.getString("nombre_permiso"));
                }
            }
        }
        return permisos;
    }
}
