#MODELO:normativa.py
#Esto basicamente maneja toda la lógica de lectura y escritura de reglamentos,artículos, incisos, etc.

class Normativa:
#Clase que gestiona los elementos normativos (artículos, incisos, etc.)
    def __init__(self, conexion_bd):
        self.bd = conexion_bd
    
#1:Obtener el arbol completo de un reglamento,retorna la lista de elementos ordenados jerárquicamente
    
    def obtener_arbol_reglamento(self, id_reglamento):
        sql = """
            WITH RECURSIVE arbol AS (
                -- CASO BASE: Los Títulos (sin padre)
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
                WHERE e.id_reglamento = %s
                  AND e.id_elemento_padre IS NULL
                  AND v.nombre = 'Vigente'

                UNION ALL

                -- CASO RECURSIVO: Los hijos
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
        """
        
        try:
            resultado = self.bd.query(sql, [id_reglamento])
            return resultado
        except Exception as error:
            print(f"Error en obtener_arbol_reglamento: {error}")
            raise
#2:Obtener los hijos directos de un elemento, retorna la lista de elementos hijos
    def obtener_hijos(self, id_elemento_padre):    
        sql = """
            SELECT *
            FROM elemento_normativo
            WHERE id_elemento_padre = %s
              AND id_estado_vigencia = (
                  SELECT id_estado_vigencia 
                  FROM catalogo_estado_vigencia 
                  WHERE nombre = 'Vigente'
              )
            ORDER BY orden ASC;
        """
        try:
            resultado = self.bd.query(sql, [id_elemento_padre])
            return resultado
        except Exception as error:
            print(f"Error en obtener_hijos: {error}")
            raise

#3: Obtén que versión estaba vigente en cierta fecha
    
    def obtener_articulo_en_fecha(self, id_elemento, fecha_consulta):    
        sql = """
            SELECT *
            FROM elemento_normativo
            WHERE id_elemento = %s
              AND fecha_inicio_vigencia <= %s
              AND (fecha_fin_vigencia IS NULL OR fecha_fin_vigencia > %s)
            LIMIT 1;
        """
        
        try:
            resultado = self.bd.query_one(sql, [id_elemento, fecha_consulta, fecha_consulta])
            return resultado
        except Exception as error:
            print(f"Error en obtener_articulo_en_fecha: {error}")
            raise

#4:crear un nuevo elemento
    
    def crear_elemento(self, datos):
        id_reglamento = datos.get('id_reglamento')
        id_elemento_padre = datos.get('id_elemento_padre')
        id_nivel_reglamento = datos.get('id_nivel_reglamento')
        numero_etiqueta = datos.get('numero_etiqueta')
        contenido_texto = datos.get('contenido_texto')
        orden = datos.get('orden')
        
        sql = """
            INSERT INTO elemento_normativo (
                id_reglamento,
                id_elemento_padre,
                id_nivel_reglamento,
                numero_etiqueta,
                contenido_texto,
                orden,
                fecha_inicio_vigencia,
                id_estado_vigencia
            ) VALUES (%s, %s, %s, %s, %s, %s, CURRENT_DATE, 1)
            RETURNING *;
        """
        
        try:
            resultado = self.bd.query(sql, [
                id_reglamento,
                id_elemento_padre,
                id_nivel_reglamento,
                numero_etiqueta,
                contenido_texto,
                orden
            ])
            return resultado
        except Exception as error: #si el error es por el índice especial
            if "uq_etiqueta_vigente" in str(error):
                error_personalizado = Exception(
                    f'Ya existe un elemento "{numero_etiqueta}" vigente en este nivel'
                )
                error_personalizado.tipo = "DUPLICADO_VIGENTE"
                raise error_personalizado
            
            print(f"Error en crear_elemento: {error}")
            raise
    
#5:Obtén el historial de un elemento
    def obtener_historial(self, id_elemento):
        sql = """
            SELECT *
            FROM elemento_normativo
            WHERE id_elemento = %s
            ORDER BY fecha_inicio_vigencia DESC;
        """
        
        try:
            resultado = self.bd.query(sql, [id_elemento])
            return resultado
        except Exception as error:
            print(f"Error en obtener_historial: {error}")
            raise
    
#6:Obtener información de un elemento específico
    
    def obtener_elemento(self, id_elemento):  
        sql = """
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
            WHERE e.id_elemento = %s;
        """
        
        try:
            resultado = self.bd.query_one(sql, [id_elemento])
            return resultado
        except Exception as error:
            print(f"Error en obtener_elemento: {error}")
            raise