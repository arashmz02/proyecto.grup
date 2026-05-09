Jerarquía Normativa Recursiva - Issue #10
Contexto: Este documento describe la implementación de la estructura recursiva de reglamentos con versionamiento histórico automático.

Niveles:
1. Título (nivel 1) - Sin padre
2. Capítulo (nivel 2) - Padre: Título
3. Artículo (nivel 3) - Padre: Capítulo
4. Inciso (nivel 4) - Padre: Artículo o Inciso
5. Sub-inciso (nivel 5) - Padre: Inciso

Caracteristicas del CTE Recursiva
1. Obtiene el árbol completo de reglamentos
2. Ordena por ruta jerárquica
3. Filtra solo elementos vigentes

Versionamiento automático
1. Trigger `tg_vigencia_normativa` marca versiones anteriores como "Histórico"
2. Partial Unique Index evita duplicados vigentes
3. Soporte de fechas de inicio y fin de vigencia

Validación jerárquica
1. El controlador valida que el nivel del padre sea menor que el del hijo
2. Solo Títulos pueden no tener padre

# Los archivos principales: `src/models/Normativa.js` - Modelo con métodos de CTE y versionamiento, 
`src/controllers/NormativaController.js` - API REST con validación jerárquica y  `src/views/normativa/` - Vistas HTML para árbol y compilador

Ejemplo de uso en java:
```javascript
const normativa = new Normativa(bd);
const arbol = await normativa.obtenerArbolReglamento(1);
// Devuelve toda la jerarquía de un reglamento
```
Prueba del caso Artículo 18:
Al insertar una reforma al Artículo 18, el trigger automáticamente:
1. Marca la versión anterior como "Histórico"
2. Establece su fecha de fin de vigencia
3. Inserta la nueva versión como "Vigente"