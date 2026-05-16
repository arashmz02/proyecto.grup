feature/issue-10-jerarquia-normativa
# Diccionario de Datos - Módulo de Jerarquía Normativa (Issue #10)

## Tabla: catalogo_nivel_reglamento

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_nivel_reglamento | INT IDENTITY(1,1) | ID único del nivel (PK) |
| nombre | NVARCHAR(40) NOT NULL UNIQUE | Nombre del nivel: Título, Capítulo, Artículo, Inciso, Sub-inciso |
| orden | INT NOT NULL | Orden jerárquico del nivel |

---

## Tabla: catalogo_estado_vigencia

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_estado_vigencia | INT IDENTITY(1,1) | ID único del estado (PK) |
| nombre | NVARCHAR(20) NOT NULL UNIQUE | Estado: Vigente, Histórico, Derogado |

---

## Tabla: reglamento

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_reglamento | INT IDENTITY(1,1) | ID único del reglamento (PK) |
| nombre_normativa | NVARCHAR(150) NOT NULL | Nombre completo del reglamento |
| sigla | NVARCHAR(20) NOT NULL UNIQUE | Acrónimo único del reglamento |
| emisor | NVARCHAR(10) | Órgano emisor: AIR o CI |

---

## Tabla: elemento_normativo

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_elemento | INT IDENTITY(1,1) | ID único del elemento (PK) |
| id_reglamento | INT NOT NULL | FK a reglamento.id_reglamento |
| id_elemento_padre | INT | FK a elemento_normativo.id_elemento (NULL si es raíz) |
| id_nivel_reglamento | INT NOT NULL | FK a catalogo_nivel_reglamento.id_nivel_reglamento |
| numero_etiqueta | NVARCHAR(20) NOT NULL | Etiqueta visible: "18", "a)", "I.1.a" |
| contenido_texto | NVARCHAR(MAX) NOT NULL | Texto completo del elemento |
| orden | INT NOT NULL | Orden entre hermanos |
| fecha_inicio_vigencia | DATE NOT NULL | Fecha de inicio de vigencia |
| fecha_fin_vigencia | DATE | Fecha de fin de vigencia |
| id_estado_vigencia | INT NOT NULL | FK a catalogo_estado_vigencia.id_estado_vigencia |
| id_acuerdo_origen | INT | FK opcional a futura tabla resolucion |



