/* MODELO: Normativa.js
 Maneja toda la lógica de lectura y escritura de reglamentos,artículos, incisos, etc.*/

class Normativa {
    constructor(conexionBD) {
        this.bd = conexionBD;
    }
// 1: Obtener el arbol completo de un reglamento 
    async obtenerArbolReglamento(id_reglamento) {
        const sql = `
            WITH RECURSIVE arbol AS (
                SELECT
                    e.id_elemento,
                    e.id_elemento_padre,
                    e.numero_etiqueta,
                    e.contenido_texto,
                    e.orden,
                    n.nombre AS nivel,
                    1 AS profundidad,
                    ARRAY[e.orden] AS ruta
                FROM elemento_normativo e
                JOIN catalogo_nivel_reglamento n 
                    ON e.id_nivel_reglamento = n.id_nivel_reglamento
                JOIN catalogo_estado_vigencia v 
                    ON e.id_estado_vigencia = v.id_estado_vigencia
                WHERE e.id_reglamento = $1
                  AND e.id_elemento_padre IS NULL
                  AND v.nombre = 'Vigente'

                UNION ALL

                SELECT
                    e.id_elemento,
                    e.id_elemento_padre,
                    e.numero_etiqueta,
                    e.contenido_texto,
                    e.orden,
                    n.nombre AS nivel,
                    a.profundidad + 1,
                    a.ruta || e.orden
                FROM elemento_normativo e
                JOIN catalogo_nivel_reglamento n 
                    ON e.id_nivel_reglamento = n.id_nivel_reglamento
                JOIN catalogo_estado_vigencia v 
                    ON e.id_estado_vigencia = v.id_estado_vigencia
                JOIN arbol a ON e.id_elemento_padre = a.id_elemento
                WHERE v.nombre = 'Vigente'
            )
            SELECT * FROM arbol ORDER BY ruta;
        `;
        
        try {
            const resultado = await this.bd.query(sql, [id_reglamento]);
            return resultado;
        } catch (error) {
            console.error("Error en obtenerArbolReglamento:", error);
            throw error;
        }
    }

// 2: Obtener los hijos directos de un elemento
    async obtenerHijos(id_elemento_padre) {
        const sql = `
            SELECT *
            FROM elemento_normativo
            WHERE id_elemento_padre = $1
              AND id_estado_vigencia = (
                  SELECT id_estado_vigencia 
                  FROM catalogo_estado_vigencia 
                  WHERE nombre = 'Vigente'
              )
            ORDER BY orden ASC;
        `;
        try {
            const resultado = await this.bd.query(sql, [id_elemento_padre]);
            return resultado;
        } catch (error) {
            console.error("Error en obtenerHijos:", error);
            throw error;
        }
    }

// 3: Obtén qué versión estaba vigente en cierta fecha
    async obtenerArticuloEnFecha(id_elemento, fecha_consulta) {
        const sql = `
            SELECT *
            FROM elemento_normativo
            WHERE id_elemento = $1
              AND fecha_inicio_vigencia <= $2
              AND (fecha_fin_vigencia IS NULL OR fecha_fin_vigencia > $2)
            LIMIT 1;
        `;
        try {
            const resultado = await this.bd.queryOne(sql, [id_elemento, fecha_consulta]);
            return resultado;
        } catch (error) {
            console.error("Error en obtenerArticuloEnFecha:", error);
            throw error;
        }
    }

// 4: CREAR UN NUEVO ELEMENTO
    async crearElemento(datos) {
        const {
            id_reglamento,
            id_elemento_padre,
            id_nivel_reglamento,
            numero_etiqueta,
            contenido_texto,
            orden
        } = datos;

        const sql = `
            INSERT INTO elemento_normativo (
                id_reglamento,
                id_elemento_padre,
                id_nivel_reglamento,
                numero_etiqueta,
                contenido_texto,
                orden,
                fecha_inicio_vigencia,
                id_estado_vigencia
            ) VALUES ($1, $2, $3, $4, $5, $6, CURRENT_DATE, 1)
            RETURNING *;
        `;

        try {
            const resultado = await this.bd.query(sql, [
                id_reglamento,
                id_elemento_padre || null,
                id_nivel_reglamento,
                numero_etiqueta,
                contenido_texto,
                orden
            ]);
            return resultado;
        } catch (error) {
            if (error.message.includes("uq_etiqueta_vigente")) {
                const error_personalizado = new Error(
                    `Ya existe un elemento "${numero_etiqueta}" vigente en este nivel`
                );
                error_personalizado.tipo = "DUPLICADO_VIGENTE";
                throw error_personalizado;
            }
            console.error("Error en crearElemento:", error);
            throw error;
        }
    }

// 5: Obten el historial de un elemento
    async obtenerHistorial(id_elemento) {
        const sql = `
            SELECT *
            FROM elemento_normativo
            WHERE id_elemento = $1
            ORDER BY fecha_inicio_vigencia DESC;
        `;
        try {
            const resultado = await this.bd.query(sql, [id_elemento]);
            return resultado;
        } catch (error) {
            console.error("Error en obtenerHistorial:", error);
            throw error;
        }
    }

// 6: Obtener información de un elemento específico
    async obtenerElemento(id_elemento) {
        const sql = `
            SELECT 
                e.*,
                r.nombre_normativa,
                r.sigla,
                n.nombre AS nivel_nombre,
                v.nombre AS estado_nombre
            FROM elemento_normativo e
            JOIN reglamento r ON e.id_reglamento = r.id_reglamento
            JOIN catalogo_nivel_reglamento n ON e.id_nivel_reglamento = n.id_nivel_reglamento
            JOIN catalogo_estado_vigencia v ON e.id_estado_vigencia = v.id_estado_vigencia
            WHERE e.id_elemento = $1;
        `;
        try {
            const resultado = await this.bd.queryOne(sql, [id_elemento]);
            return resultado;
        } catch (error) {
            console.error("Error en obtenerElemento:", error);
            throw error;
        }
    }
}

module.exports = Normativa;