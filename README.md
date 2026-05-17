# SGL-AIR — Sistema de Gestion Legislativa AIR

Sistema web para la gestion de la Asamblea Institucional Representativa (AIR) del Instituto Tecnologico de Costa Rica. Desarrollado como proyecto del curso de Bases de Datos, Sprint 2.

## Equipo

| Miembro | GitHub | Issues asignados |
|---|---|---|
| Frank Alvarez | @Frankyyy11 | #0 Seguridad y Roles, #9 Padron de Asambleistas |
| Josue Garro | @JosueGarroPochet | #10 Normativa Jerarquica |
| Arash Moazzami | @arashmz02 | #1 Generacion de Folios, #2 Trazabilidad y Auditoria, #14 Bitacora de Cambios de Identidad |

## Stack Tecnologico

| Componente | Tecnologia |
|---|---|
| Lenguaje | Java 17+ (probado con OpenJDK 21) |
| Servidor | Apache Tomcat 10.1 (Jakarta EE) |
| Base de datos | Azure SQL Database (Free tier) |
| Build | Apache Maven 3.9+ |
| CI/CD | GitHub Actions |
| Autenticacion | BCrypt (jBCrypt 0.4, factor de coste 12) |
| Frontend | JSP + HTML + CSS + JavaScript |

## Estructura del Proyecto

```
proyecto.grup/
├── .github/workflows/
│   ├── deploy-sql.yml          # Aplica SQL a Azure en push a develop
│   └── validate-sql.yml        # Valida SQL en PRs
├── docs/
│   └── diccionario-datos.md    # Diccionario de datos completo
├── src/main/java/
│   ├── config/
│   │   └── Conexion.java       # Conexion a Azure SQL (env vars)
│   ├── controllers/
│   │   ├── AuthController.java         # Login, logout, middlewares
│   │   ├── AsambleistaController.java  # CRUD asambleistas
│   │   ├── FolioServlet.java           # Generacion de folios
│   │   └── InicioController.java       # Panel post-login
│   ├── models/
│   │   ├── Asambleista.java    # Modelo asambleistas + nombramientos
│   │   ├── FolioDAO.java       # Generacion atomica de folios
│   │   └── Usuario.java        # Modelo usuario con BCrypt
│   └── com/itcr/air/
│       ├── dao/NormativaDAO.java       # CTE recursiva para normativa
│       ├── servlets/NormativaServlet.java  # API REST normativa
│       └── util/ConexionBD.java        # Conexion alternativa (env vars)
├── src/main/webapp/
│   ├── WEB-INF/web.xml
│   ├── index.jsp               # Redirige al login
│   └── views/
│       ├── auth/login.jsp              # Formulario de login
│       ├── asambleistas/
│       │   ├── asambleista-lista.jsp   # Listado + detalle
│       │   └── asambleista-registro.jsp # Formulario de alta
│       ├── normativa/
│       │   ├── arbol.jsp       # Tree view del Estatuto Organico
│       │   └── reforma.jsp     # Vista de reformas
│       └── folios.jsp          # Generacion de folios
├── tools/
│   └── GenerarHash.java        # Utilidad para generar hashes BCrypt
├── proyecto-air.sql            # Script completo de la BD (DROP & RECREATE)
├── pom.xml                     # Dependencias Maven
├── .env.example                # Plantilla de variables de entorno
├── .gitignore
├── README.md
└── REGLAS_GIT.md
```

## Requisitos Previos

- **Java JDK 17+** (recomendado: Microsoft OpenJDK 21)
- **Apache Maven 3.9+**
- **Apache Tomcat 10.1+** (NO Tomcat 9 — el proyecto usa `jakarta.servlet.*`)
- **sqlcmd** (Go version, para ejecutar scripts contra Azure)
- **Acceso a Azure SQL Database** (credenciales del equipo)

## Configuracion del Entorno

### 1. Clonar el repositorio

```bash
git clone https://github.com/arashmz02/proyecto.grup.git
cd proyecto.grup
```

### 2. Configurar las credenciales de la base de datos

Copiar la plantilla y rellenar con las credenciales reales:

```bash
cp .env.example .env
```

Editar `.env` con los valores reales:

```
DB_SERVER=sglairtec.database.windows.net
DB_NAME=sgl_air
DB_USER=<usuario_sql>
DB_PASSWORD=<contrasena_sql>
```

**IMPORTANTE:** el archivo `.env` esta en `.gitignore` y NUNCA se sube al repositorio.

### 3. Compilar el proyecto

```bash
mvn clean package
```

Genera `target/sgl-air.war`.

### 4. Desplegar en Tomcat

Copiar el `.war` a la carpeta `webapps` de Tomcat:

```bash
cp target/sgl-air.war $CATALINA_HOME/webapps/sgl-air.war
```

### 5. Arrancar Tomcat con las variables de entorno

En Windows (PowerShell):

```powershell
$env:CATALINA_HOME="C:\tomcat10"
$env:DB_SERVER="sglairtec.database.windows.net"
$env:DB_NAME="sgl_air"
$env:DB_USER="<usuario_sql>"
$env:DB_PASSWORD="<contrasena_sql>"
C:\tomcat10\bin\catalina.bat run
```

### 6. Acceder a la aplicacion

Abrir en el navegador: `http://localhost:8080/sgl-air/`

## Usuarios del Sistema

El script `proyecto-air.sql` crea 4 usuarios, uno por cada rol:

| Usuario | Contrasena | Rol | Permisos |
|---|---|---|---|
| admin | Admin2026SglAir | Administrador | Todos (GESTIONAR_USUARIOS, REGISTRAR_ASAMBLEISTAS, EMITIR_CERTIFICACION, CONSULTAR_NORMATIVA) |
| secretaria | Secretaria2026 | Secretaria AIR | REGISTRAR_ASAMBLEISTAS, EMITIR_CERTIFICACION, CONSULTAR_NORMATIVA |
| consulta | Consulta2026 | Consulta | CONSULTAR_NORMATIVA |
| asambleista | Asambleista2026 | Asambleista | CONSULTAR_NORMATIVA |

Los hashes BCrypt fueron generados con `tools/GenerarHash.java` (factor de coste 12).

## Base de Datos

### Aplicar el script SQL

El archivo `proyecto-air.sql` es un script DROP & RECREATE que crea toda la BD desde cero:

```bash
sqlcmd -S sglairtec.database.windows.net -d sgl_air -U <usuario> -P "<contrasena>" -i proyecto-air.sql -l 60 -b
```

**Nota:** Azure SQL Free tier tiene auto-pause. Si da error "Database not currently available", esperar 60 segundos y reintentar.

### Tablas (16)

| Modulo | Tablas |
|---|---|
| Seguridad | sys_rol, sys_permiso, sys_usuario, sys_usuario_rol, sys_rol_permiso, sys_log_auditoria |
| Asambleistas | asambleista, nombramiento, bitacora_asambleistas |
| Normativa | catalogo_nivel_reglamento, catalogo_estado_vigencia, reglamento, elemento_normativo |
| Folios | catalogo_maestro, control_folio, certificacion_emitida |

### Triggers (6)

| Trigger | Tabla | Funcion |
|---|---|---|
| tg_auditoria_asambleista | asambleista | Registra INSERT/UPDATE/DELETE en sys_log_auditoria |
| tg_auditoria_nombramiento | nombramiento | Registra INSERT/UPDATE/DELETE en sys_log_auditoria |
| tg_traslape_sector | nombramiento | Rechaza nombramientos con fechas solapadas (error 50001) |
| tg_vigencia_normativa | elemento_normativo | Archiva version anterior como "Historico" al insertar nueva |
| tg_folio_secuencial | certificacion_emitida | Genera folio unico DAIR-NNN-AAAA con UPDLOCK/HOLDLOCK |
| tg_cambio_identidad | asambleista | Registra cambios de cedula/nombre en bitacora_asambleistas |

### CI/CD

Dos workflows de GitHub Actions:

- **validate-sql.yml:** se ejecuta en PRs que tocan `proyecto-air.sql`. Valida que el script ejecute sin errores contra Azure.
- **deploy-sql.yml:** se ejecuta en push a `develop`. Aplica el script a Azure automaticamente.

## Modulos Funcionales

### Issue #0 — Seguridad y Roles (Frank)

- Login con validacion BCrypt
- Sesion con roles y permisos
- Middlewares reutilizables (`middlewareAuth`, `middlewarePermiso`)
- Logout que destruye la sesion
- Auditoria automatica via SESSION_CONTEXT + triggers

### Issue #9 — Padron de Asambleistas (Frank)

- Listado de asambleistas con estado Vigente/Inactivo
- Registro con validaciones (cedula X-XXXX-XXXX, correo institucional)
- Detalle con historial de nombramientos (Maestro-Detalle)
- Selector de sectores desde catalogo_maestro
- Captura del error 50001 del trigger de traslape

### Issue #10 — Normativa Jerarquica (Josue)

- API REST que devuelve el arbol del Estatuto Organico en JSON
- CTE recursiva en T-SQL para resolver la jerarquia
- Vista con tree view dinamico (expand/collapse via JavaScript)
- Badges de estado "Vigente" en cada elemento
- Trigger de versionamiento automatico (INSTEAD OF INSERT)

### Issue #1 — Generacion de Folios (Arash)

- Formulario para generar folios unicos
- Numeracion atomica via trigger con UPDLOCK/HOLDLOCK
- Formato DAIR-NNN-AAAA (ej. DAIR-001-2026)
- Integrado con autenticacion (requiere permiso EMITIR_CERTIFICACION)

### Issue #2 — Trazabilidad y Auditoria (Arash)

- Tabla `sys_log_auditoria` registra toda operacion de escritura
- Triggers `tg_auditoria_asambleista` y `tg_auditoria_nombramiento` se disparan en INSERT/UPDATE/DELETE
- `Conexion.establecerContexto()` inyecta el `id_usuario` de la sesion activa via `SESSION_CONTEXT`
- Cada registro incluye: tabla afectada, operacion, datos anteriores/nuevos, usuario responsable y fecha

### Issue #14 — Bitacora de Cambios de Identidad (Arash)

- Tabla `bitacora_asambleistas` almacena el historial de cambios de cedula y nombre
- Trigger `tg_cambio_identidad` (AFTER UPDATE en asambleista) detecta cambios en campos de identidad
- Registra automaticamente: valor anterior, valor nuevo, fecha del cambio y usuario responsable
- Garantiza que ningun cambio de identidad se pierda para efectos legales

## Flujo de Trabajo Git

El equipo sigue el flujo definido en `REGLAS_GIT.md`:

1. Crear rama feature desde `develop` (`feature/issue-X-descripcion`)
2. Desarrollar y commitear con Conventional Commits (`feat:`, `fix:`, `refactor:`)
3. Abrir Pull Request hacia `develop`
4. Code review por al menos un miembro del equipo
5. Merge a `develop` tras aprobacion

## Utilidades

### Generar un hash BCrypt

Para crear o cambiar la contrasena de un usuario:

```bash
# Editar la contrasena en tools/GenerarHash.java
# Compilar y ejecutar:
javac -cp "target/sgl-air/WEB-INF/lib/*" tools/GenerarHash.java -d .
java -cp ".:target/sgl-air/WEB-INF/lib/*" GenerarHash

# Copiar el hash generado y actualizar en la BD:
# UPDATE sys_usuario SET password_hash = '<hash>' WHERE username = '<usuario>';
```

En Windows, usar `;` en vez de `:` para el classpath.
