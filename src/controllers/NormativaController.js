/*CONTROLADOR: NormativaController.js
Maneja las solicitudes HTTP del usuario y llama al Modelo*/
const Normativa = require('../models/Normativa');

class NormativaController {
    constructor(conexionBD) {
        this.normativa = new Normativa(conexionBD);
    }

//Devuelve el arbol completo de un reglamento
    async verArbol(req, res) {
        try {
            const { id } = req.params;

            if (!id || id < 1) {
                return res.status(400).json({
                    success: false,
                    error: 'ID del reglamento inválido'
                });
            }

            const arbol = await this.normativa.obtenerArbolReglamento(id);

            res.json({
                success: true,
                datos: arbol
            });

        } catch (error) {
            console.error("Error en verArbol:", error);
            res.status(500).json({
                success: false,
                error: error.message
            });
        }
    }

//Devuelve un elemento especifico con su historial
    async verArticulo(req, res) {
        try {
            const { id } = req.params;

            if (!id || id < 1) {
                return res.status(400).json({
                    success: false,
                    error: 'ID del elemento inválido'
                });
            }

            const elemento = await this.normativa.obtenerElemento(id);
            
            if (!elemento) {
                return res.status(404).json({
                    success: false,
                    error: 'Elemento no encontrado'
                });
            }

            const historial = await this.normativa.obtenerHistorial(id);

            res.json({
                success: true,
                elemento,
                historial
            });

        } catch (error) {
            console.error("Error en verArticulo:", error);
            res.status(500).json({
                success: false,
                error: error.message
            });
        }
    }
//Crea un nuevo elemento(reforma, cambio, etc.)
    async crearReforma(req, res) {
        try {
            const {
                id_reglamento,
                id_elemento_padre,
                id_nivel_reglamento,
                numero_etiqueta,
                contenido_texto,
                orden
            } = req.body;

//Validaciones básicas
            if (!id_reglamento || !id_nivel_reglamento || !numero_etiqueta || !contenido_texto) {
                return res.status(400).json({
                    success: false,
                    error: 'Faltan campos obligatorios'
                });
            }

            if (!orden || orden < 1) {
                return res.status(400).json({
                    success: false,
                    error: 'Orden debe ser mayor a 0'
                });
            }

//Validacion de jerarquia 
            const sql_nivel = `
                SELECT orden FROM catalogo_nivel_reglamento 
                WHERE id_nivel_reglamento = $1
            `;
            
            let nivel_nuevo;
            try {
                nivel_nuevo = await this.normativa.bd.queryOne(sql_nivel, [id_nivel_reglamento]);
            } catch (e) {
                return res.status(400).json({
                    success: false,
                    error: 'Nivel de reglamento no válido'
                });
            }

            if (!nivel_nuevo) {
                return res.status(400).json({
                    success: false,
                    error: 'Nivel de reglamento no válido'
                });
            }

            if (id_elemento_padre) {
                const sql_padre = `
                    SELECT e.*, n.orden AS nivel_padre
                    FROM elemento_normativo e
                    JOIN catalogo_nivel_reglamento n 
                        ON e.id_nivel_reglamento = n.id_nivel_reglamento
                    WHERE e.id_elemento = $1
                `;
                
                let padre;
                try {
                    padre = await this.normativa.bd.queryOne(sql_padre, [id_elemento_padre]);
                } catch (e) {
                    padre = null;
                }

                if (!padre) {
                    return res.status(400).json({
                        success: false,
                        error: 'El elemento padre no existe'
                    });
                }

                if (padre.nivel_padre >= nivel_nuevo.orden) {
                    return res.status(400).json({
                        success: false,
                        error: `El padre no puede ser de un nivel igual o inferior`
                    });
                }
            } else {
                if (nivel_nuevo.orden !== 1) {
                    return res.status(400).json({
                        success: false,
                        error: 'Solo los Títulos pueden no tener padre'
                    });
                }
            }

            try {
                const resultado = await this.normativa.crearElemento({
                    id_reglamento,
                    id_elemento_padre: id_elemento_padre || null,
                    id_nivel_reglamento,
                    numero_etiqueta,
                    contenido_texto,
                    orden
                });

                res.status(201).json({
                    success: true,
                    mensaje: 'Elemento creado exitosamente. La versión anterior fue marcada como histórica.',
                    elemento: resultado
                });

            } catch (error) {
                if (error.tipo === "DUPLICADO_VIGENTE") {
                    return res.status(400).json({
                        success: false,
                        error: error.message
                    });
                }
                throw error;
            }

        } catch (error) {
            console.error("Error en crearReforma:", error);
            res.status(500).json({
                success: false,
                error: error.message
            });
        }
    }
}

module.exports = NormativaController;