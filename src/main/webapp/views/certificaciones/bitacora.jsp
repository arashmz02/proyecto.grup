<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="true" %>
<% String ctx = request.getContextPath(); %>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Bitacora de Auditoria - SGL-AIR</title>
<style>
*{box-sizing:border-box;margin:0;padding:0;}
body{font-family:'Segoe UI',sans-serif;background:#ecf0f1;color:#2c3e50;}
.barra{background:#1f2d3d;color:#fff;padding:1rem 2rem;display:flex;justify-content:space-between;align-items:center;}
.barra a{color:#fff;background:#2980b9;padding:0.5rem 1rem;text-decoration:none;border-radius:4px;font-size:0.85rem;}
.contenido{padding:2rem;max-width:1100px;margin:0 auto;}
.tarjeta{background:#fff;padding:1.5rem;border-radius:6px;box-shadow:0 2px 8px rgba(0,0,0,0.05);}
h2{color:#1f2d3d;font-size:1.2rem;margin-bottom:0.75rem;}
.resumen{color:#7f8c8d;margin-bottom:1rem;font-size:0.9rem;}
table{border-collapse:collapse;width:100%;font-size:0.85rem;}
th{background:#1f2d3d;color:#fff;text-align:left;padding:8px 10px;}
td{padding:7px 10px;border-bottom:1px solid #e5e8e8;}
tr:nth-child(even) td{background:#f7f9f9;}
.vacio{color:#7f8c8d;font-style:italic;}
</style>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/air-unificado.css">
</head>
<body>
<div class="barra">
<span>Sistema de Gestion Legislativa AIR</span>
<a href="<%= ctx %>/inicio">Volver al inicio</a>
</div>
<div class="contenido">
<div class="tarjeta">
<h2>Bitacora de Auditoria de Certificaciones (Issue #13)</h2>
<p id="resumen" class="resumen">Cargando registros...</p>
<div id="tabla"></div>
</div>
</div>
<script>
var ctx = '<%= ctx %>';
fetch(ctx + '/bitacora/certificaciones').then(function(r){return r.json();}).then(function(data){
  var eventos = data.eventos || [];
  document.getElementById('resumen').textContent = 'Total de registros: ' + (data.total != null ? data.total : eventos.length);
  var cont = document.getElementById('tabla');
  if (eventos.length === 0){ cont.innerHTML = '<p class="vacio">No hay registros de auditoria todavia.</p>'; return; }
  var cols = Object.keys(eventos[0]);
  var html = '<table><thead><tr>';
  cols.forEach(function(c){ html += '<th>' + c + '</th>'; });
  html += '</tr></thead><tbody>';
  eventos.forEach(function(ev){
    html += '<tr>';
    cols.forEach(function(c){ var v = ev[c]; if(v===null||v===undefined) v=''; html += '<td>' + String(v) + '</td>'; });
    html += '</tr>';
  });
  html += '</tbody></table>';
  cont.innerHTML = html;
}).catch(function(e){
  document.getElementById('resumen').textContent = '';
  document.getElementById('tabla').innerHTML = '<p class="vacio">No se pudo cargar la bitacora: ' + e.message + '</p>';
});
</script>
</body>
</html>