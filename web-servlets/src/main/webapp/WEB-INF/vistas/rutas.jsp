<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%!
    public int calcularHorasTotalesCatalogo(java.util.List<co.edu.sena.skillup.dto.RutaAprendizajeDTO> rutas) {
        if (rutas == null) {
            return 0;
        }
        int suma = 0;
        for (co.edu.sena.skillup.dto.RutaAprendizajeDTO ruta : rutas) {
            suma += ruta.duracionHoras();
        }
        return suma;
    }
%>
<%
    @SuppressWarnings("unchecked")
    java.util.List<co.edu.sena.skillup.dto.RutaAprendizajeDTO> listaRutas =
            (java.util.List<co.edu.sena.skillup.dto.RutaAprendizajeDTO>) request.getAttribute("rutas");
    int totalHorasCatalogo = calcularHorasTotalesCatalogo(listaRutas);
%>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>SkillUp Pathway — Catálogo e Inscripción a Rutas de Aprendizaje (RF-04 / RF-05)</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
  <%@ include file="cabecera.jspf" %>

  <div class="app-shell">
    <aside class="app-sidebar" aria-label="Resumen de catálogo formativo">
      <h2 style="margin-top:0; font-size:1.05rem;">Catálogo SkillUp</h2>
      <p style="margin:0.3rem 0;"><strong>Horas ofertadas:</strong> <code><%= totalHorasCatalogo %> h</code></p>
      <p style="margin:0.3rem 0;"><strong>Área filtrada (GET):</strong> <code><c:out value="${param.area}" default="Todas"/></code></p>
      <p style="margin:0.3rem 0;"><strong>Inscripciones activas:</strong> <code><c:out value="${requestScope.totalInscripciones}" default="0"/></code></p>
    </aside>

    <main id="contenido-principal" class="main-workspace">
      <section class="card-panel" aria-labelledby="titulo-catalogo-jsp">
        <h1 id="titulo-catalogo-jsp" style="margin-top:0; font-size:1.25rem;">
          Catálogo de Rutas Formativas e Inscripción (RF-04, RF-05, RF-06)
        </h1>

        <c:if test="${not empty requestScope.mensajeExito or not empty param.mensaje}">
          <div class="alert-banner" role="status">
            <c:out value="${not empty requestScope.mensajeExito ? requestScope.mensajeExito : param.mensaje}"/>
          </div>
        </c:if>

        <!-- FORMULARIO GET: FILTRO POR ÁREA DE CONOCIMIENTO -->
        <form action="${pageContext.request.contextPath}/rutas" method="get" style="margin-bottom:1.25rem;">
          <div class="form-grid">
            <div class="field">
              <label for="area">Filtrar por Área de Conocimiento (GET)</label>
              <select id="area" name="area">
                <option value="">Todas las áreas</option>
                <option value="Programación" <c:if test="${requestScope.areaSeleccionada eq 'Programación'}">selected</c:if>>Programación y Backend Java</option>
                <option value="Desarrollo Web" <c:if test="${requestScope.areaSeleccionada eq 'Desarrollo Web'}">selected</c:if>>Desarrollo Web y Frontend</option>
                <option value="Bases de Datos" <c:if test="${requestScope.areaSeleccionada eq 'Bases de Datos'}">selected</c:if>>Bases de Datos Relacionales</option>
                <option value="Ciberseguridad" <c:if test="${requestScope.areaSeleccionada eq 'Ciberseguridad'}">selected</c:if>>Ciberseguridad y APIs REST</option>
              </select>
            </div>
          </div>
          <div class="btn-group">
            <button type="submit" class="btn btn-primary">Filtrar Catálogo (HTTP GET)</button>
            <a href="${pageContext.request.contextPath}/rutas" class="btn btn-secondary">Ver Todas</a>
          </div>
        </form>

        <!-- TABLA DE RUTAS E INSCRIPCIÓN VÍA MÉTODO POST -->
        <div class="table-responsive">
          <table class="admin-table">
            <thead>
              <tr>
                <th scope="col">Código</th>
                <th scope="col">Ruta de Aprendizaje</th>
                <th scope="col">Área de Conocimiento</th>
                <th scope="col">Duración</th>
                <th scope="col">Estado / Avance</th>
                <th scope="col">Inscripción (HTTP POST)</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="ruta" items="${requestScope.rutas}">
                <tr>
                  <td><code>RUTA-<c:out value="${ruta.idRuta()}"/></code></td>
                  <td>
                    <strong><c:out value="${ruta.titulo()}"/></strong><br>
                    <span style="font-size:0.82rem; color:#555;"><c:out value="${ruta.descripcion()}"/></span>
                  </td>
                  <td><span class="badge"><c:out value="${ruta.areaConocimiento()}"/></span></td>
                  <td><c:out value="${ruta.duracionHoras()}"/> horas</td>
                  <td>
                    <c:choose>
                      <c:when test="${ruta.inscrito()}">
                        <strong>Inscrito (<c:out value="${ruta.porcentajeAvance()}"/>%)</strong>
                      </c:when>
                      <c:otherwise>Disponible</c:otherwise>
                    </c:choose>
                  </td>
                  <td>
                    <form action="${pageContext.request.contextPath}/rutas" method="post" style="margin:0;">
                      <input type="hidden" name="idRuta" value="${ruta.idRuta()}">
                      <input type="hidden" name="correoAprendiz" value="jfac2420@gmail.com">
                      <button type="submit" class="btn btn-primary" style="padding:0.3rem 0.7rem; min-height:34px;">
                        <c:out value="${ruta.inscrito() ? 'Registrar Avance +25% (POST)' : 'Inscribirse en Ruta (POST)'}"/>
                      </button>
                    </form>
                  </td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </div>

  <%@ include file="pie.jspf" %>
</body>
</html>
