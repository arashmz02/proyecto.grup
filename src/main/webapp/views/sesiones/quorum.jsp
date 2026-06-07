<%@ page import="java.sql.*, config.Conexion, java.util.*" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="true" %>
<%
    List<String[]> sesiones = new ArrayList<>();
    try (Connection con = Conexion.obtener();
         PreparedStatement ps = con.prepareStatement(
             "SELECT id_sesion, numero_sesion FROM sesion ORDER BY id_sesion");
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            sesiones.add(new String[]{
                String.valueOf(rs.getInt("id_sesion")),
                rs.getString("numero_sesion")
            });
        }
    } catch (Exception e) { }
    String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Control de Quorum - SGL-AIR</title>
    <style>
        *{box-sizing:border-box;margin:0;padding:0;}
        body{font-family:'Segoe UI',sans-serif;background:#ecf0f1;color:#2c3e50;}
        .barra{background:#1f2d3d;color:#fff;padding:1rem 2rem;display:flex;
               justify-content:space-between;align-items:center;}
        .barra a{color:#fff;background:#2980b9;padding:0.5rem 1rem;text-decoration:none;
                 border-radius:4px;font-size:0.85rem;}
        .contenido{padding:2rem;max-width:760px;margin:0 auto;}
        .tarjeta{background:#fff;padding:1.5rem;border-radius:6px;margin-bottom:1.25rem;
                 box-shadow:0 2px 8px rgba(0,0,0,0.05);}
        h2{color:#1f2d3d;font-size:1.2rem;margin-bottom:0.75rem;}
        label{display:block;font-weight:600;margin-bottom:0.4rem;color:#34495e;}
        select{width:100%;padding:0.6rem;border:1px solid #ccd1d9;border-radius:4px;font-size:0.95rem;}
        .badge{display:inline-block;padding:0.45rem 0.9rem;border-radius:20px;color:#fff;
               font-weight:600;font-size:0.9rem;margin:0.5rem 0 1rem;}
        .badge.ok{background:#27ae60;}
        .badge.no{background:#c0392b;}
        .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(120px,1fr));gap:1rem;}
        .dato{background:#f4f6f7;border-radius:6px;padding:1rem;text-align:center;}
        .dato .num{display:block;font-size:1.7rem;font-weight:700;color:#2980b9;}
        .dato .lbl{display:block;font-size:0.8rem;color:#7f8c8d;margin-top:0.2rem;}
        .desglose{margin-top:1rem;color:#34495e;font-size:0.9rem;}
        .estado{margin-top:0.4rem;color:#7f8c8d;font-size:0.85rem;}
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
            <h2>Control de Quorum (Issue #11)</h2>
            <label for="sel">Selecciona una sesion:</label>
            <select id="sel">
                <option value="">-- Elegi una sesion --</option>
                <% for (String[] s : sesiones) { %>
                    <option value="<%= s[0] %>"><%= s[1] %></option>
                <% } %>
            </select>
        </div>
        <div id="resultado"></div>
    </div>
    <script>
        const ctx = '<%= ctx %>';
        document.getElementById('sel').addEventListener('change', async function () {
            const id = this.value;
            const cont = document.getElementById('resultado');
            if (!id) { cont.innerHTML = ''; return; }
            cont.innerHTML = '<div class="tarjeta vacio">Cargando...</div>';
            try {
                const [q, r] = await Promise.all([
                    fetch(ctx + '/sesiones/quorum?id=' + id).then(x => x.json()),
                    fetch(ctx + '/sesiones/resumen?id=' + id).then(x => x.json())
                ]);
                if (q.error) { throw new Error(q.error); }
                const ok = q.tieneQuorum === true;
                cont.innerHTML =
                    '<div class="tarjeta">' +
                    '<h2>' + q.numeroSesion + '</h2>' +
                    '<span class="badge ' + (ok ? 'ok' : 'no') + '">' +
                    (ok ? 'Quorum alcanzado' : 'Quorum NO alcanzado') + '</span>' +
                    '<div class="grid">' +
                    '<div class="dato"><span class="num">' + q.presentes + '</span><span class="lbl">Presentes</span></div>' +
                    '<div class="dato"><span class="num">' + q.quorumRequerido + '</span><span class="lbl">Requeridos</span></div>' +
                    '<div class="dato"><span class="num">' + q.totalConvocados + '</span><span class="lbl">Convocados</span></div>' +
                    '<div class="dato"><span class="num">' + q.porcentajeAsistencia + '%</span><span class="lbl">Asistencia</span></div>' +
                    '</div>' +
                    '<p class="desglose">Ausentes: ' + (r.ausentes || 0) + ' / Justificados: ' + (r.justificados || 0) + ' / Sin registrar: ' + (r.sinRegistrar || 0) + '</p>' +
                    '<p class="estado">Estado de la sesion: ' + (q.cerrada ? 'Cerrada' : 'Abierta') + '</p>' +
                    '</div>';
            } catch (e) {
                cont.innerHTML = '<div class="tarjeta vacio">No se pudo consultar el quorum: ' + e.message + '</div>';
            }
        });
    </script>
</body>
</html>