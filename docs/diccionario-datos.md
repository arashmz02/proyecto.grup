# Diccionario de Datos - Proyecto AIR

## Tabla: catalogo_maestro

Esta tabla guarda catálogos generales reutilizables dentro del sistema.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_catalogo | SERIAL | ID del catálogo |
| tipo_catalogo | VARCHAR(50) | Tipo del catálogo |
| codigo | VARCHAR(30) | Código interno |
| nombre | VARCHAR(100) | Nombre |
| descripcion | VARCHAR(200) | Descripción opcional |
| activo | BOOLEAN | Estado del registro |

---

## Tabla: consecutivo_institucional

Se utiliza para manejar consecutivos y folios institucionales.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_consecutivo | SERIAL | ID del consecutivo |
| tipo_documento | VARCHAR(30) | Tipo de documento |
| anio | INT | Año correspondiente |
| ultimo_numero | INT | Último consecutivo usado |
| prefijo | VARCHAR(20) | Prefijo institucional |
| activo | BOOLEAN | Estado activo |

---

## Tabla: folio_documento

Guarda los folios generados por el sistema.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_folio | SERIAL | ID del folio |
| tipo_documento | VARCHAR(30) | Tipo documental |
| anio | INT | Año |
| numero | INT | Número consecutivo |
| folio | VARCHAR(30) | Folio completo |
| fecha_generacion | TIMESTAMP | Fecha de generación |
| id_usuario_genero | INT | Usuario que generó el folio |
| observacion | TEXT | Observaciones |

---

## Tabla: bitacora_cedula

Permite registrar cambios históricos de cédula.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_bitacora_cedula | SERIAL | ID de la bitácora |
| tabla_origen | VARCHAR(60) | Tabla modificada |
| registro_id | INT | Registro afectado |
| cedula_anterior | VARCHAR(20) | Valor anterior |
| cedula_nueva | VARCHAR(20) | Nuevo valor |
| motivo_cambio | VARCHAR(200) | Motivo del cambio |
| id_usuario_cambio | INT | Usuario responsable |
| fecha_cambio | TIMESTAMP | Fecha del cambio |

---

## Tabla: trazabilidad_accion

Registra acciones importantes realizadas en el sistema.

| Campo | Tipo | Descripción |
|--------|-----|-------------|
| id_trazabilidad | SERIAL | ID |
| entidad_afectada | VARCHAR(60) | Tabla afectada |
| registro_id | INT | Registro afectado |
| accion | VARCHAR(20) | Acción realizada |
| valor_anterior | TEXT | Información anterior |
| valor_nuevo | TEXT | Información nueva |
| id_usuario | INT | Usuario |
| fecha_accion | TIMESTAMP | Fecha |
| ip_origen | VARCHAR(45) | Dirección IP |

---

# Funciones y Triggers

## fn_generar_folio
Genera folios consecutivos automáticos.

## fn_bitacora_cedula_asambleista
Registra cambios de cédula en asambleistas.

## fn_trazabilidad_general
Permite guardar auditoría básica del sistema.