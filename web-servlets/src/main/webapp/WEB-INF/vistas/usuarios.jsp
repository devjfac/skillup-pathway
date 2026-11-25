<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="co.edu.sena.skillup.dto.UsuarioDTO" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%!
    public String formatearEstadoCuenta(boolean activo) {
        return activo ? "ACTIVO" : "INACTIVO";
    }
%>
<%
    List<?> listaRenderizada = (List<?>) request.getAttribute("usuarios");
    int totalMostrados = (listaRenderizada != null) ? listaRenderizada.size() : 0;
%>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>SkillUp Pathway — Directorio y Gestión de Usuarios (RF-03 / RF-09)</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
  <%@ include file="cabecera.jspf" %>

  <div class="app-shell">
    <aside class="app-sidebar" aria-label="Resumen de métricas del módulo">
      <h2 style="margin-top:0; font-size:1.05rem;">Métricas del Directorio</h2>
      <p style="margin:0.3rem 0;"><strong>Registros listados:</strong> <code><%= totalMostrados %></code></p>
      <p style="margin:0.3rem 0;"><strong>Total en sistema:</strong> <code><c:out value="${requestScope.totalRegistrados}" default="0"/></code></p>
      <p style="margin:0.3rem 0;"><strong>Filtro búsqueda (GET):</strong> <code><c:out value="${param.q}" default="(ninguno)"/></code></p>
      <p style="margin:0.3rem 0;"><strong>Filtro rol (GET):</strong> <code><c:out value="${param.rol}" default="(todos)"/></code></p>
      <div class="btn-group">
        <a href="${pageContext.request.contextPath}/usuarios?accion=nuevo" class="btn btn-primary" style="width:100%;">
          + Registrar Usuario (RF-01)
        </a>
      </div>
    </aside>

    <main id="contenido-principal" class="main-workspace">
      <section class="card-panel" aria-labelledby="titulo-filtro-jsp">
        <h1 id="titulo-filtro-jsp" style="margin-top:0; font-size:1.25rem;">
          Gestión de Cuentas de Usuario (RF-01, RF-03, RF-09 — Historia HU-07)
        </h1>

        <c:if test="${not empty requestScope.mensajeExito or not empty param.mensaje}">
          <div class="alert-banner" role="status">
            <c:out value="${not empty requestScope.mensajeExito ? requestScope.mensajeExito : param.mensaje}"/>
          </div>
        </c:if>

        <c:if test="${not empty requestScope.mensajeError}">
          <div class="alert-banner alert-error" role="alert">
            <c:out value="${requestScope.mensajeError}"/>
          </div>
        </c:if>

        <!-- FORMULARIO JSP CON MÉTODO GET PARA FILTRADO DE PARÁMETROS -->
        <form action="${pageContext.request.contextPath}/usuarios" method="get" style="margin-bottom:1.25rem;">
          <input type="hidden" name="accion" value="listar">
          <div class="form-grid">
            <div class="field">
              <label for="q">Buscar por Nombre, Correo o Documento (GET)</label>
              <input type="text" id="q" name="q" value="<c:out value='${requestScope.filtroBusqueda}'/>" placeholder="Ej.: Araujo, 3855467...">
            </div>
            <div class="field">
              <label for="rol">Filtrar por Rol (GET)</label>
              <select id="rol" name="rol">
                <option value="">Todos los roles</option>
                <c:forEach var="rolItem" items="${requestScope.rolesDisponibles}">
                  <option value="${rolItem.name()}" <c:if test="${requestScope.filtroRol eq rolItem.name()}">selected</c:if>>
                    <c:out value="${rolItem.etiqueta}"/>
                  </option>
                </c:forEach>
              </select>
            </div>
            <div class="field">
              <label for="estado">Estado (GET)</label>
              <select id="estado" name="estado">
                <option value="">Todos</option>
                <option value="ACTIVO" <c:if test="${requestScope.filtroEstado eq 'ACTIVO'}">selected</c:if>>Activos</option>
                <option value="INACTIVO" <c:if test="${requestScope.filtroEstado eq 'INACTIVO'}">selected</c:if>>Inactivos</option>
              </select>
            </div>
          </div>
          <div class="btn-group">
            <button type="submit" class="btn btn-primary">Aplicar Filtros (HTTP GET)</button>
            <a href="${pageContext.request.contextPath}/usuarios" class="btn btn-secondary">Restablecer</a>
          </div>
        </form>

        <%-- TABLA DINÁMICA RENDERIZADA CON JSTL c:forEach Y EXPRESIONES EL --%>
        <div class="table-responsive">
          <table class="admin-table">
            <thead>
              <tr>
                <th scope="col">ID</th>
                <th scope="col">Documento</th>
                <th scope="col">Nombre Completo</th>
                <th scope="col">Correo Electrónico</th>
                <th scope="col">Roles Asignados</th>
                <th scope="col">Estado</th>
                <th scope="col">Acciones (GET y POST)</th>
              </tr>
            </thead>
            <tbody>
              <c:choose>
                <c:when test="${empty requestScope.usuarios}">
                  <tr>
                    <td colspan="7">No se encontraron usuarios que coincidan con los criterios de consulta.</td>
                  </tr>
                </c:when>
                <c:otherwise>
                  <c:forEach var="u" items="${requestScope.usuarios}">
                    <tr>
                      <td><code>#<c:out value="${u.idUsuario()}"/></code></td>
                      <td><c:out value="${u.tipoDocumento()}"/> <c:out value="${u.numeroDocumento()}"/></td>
                      <td><strong><c:out value="${u.nombreCompleto()}"/></strong></td>
                      <td><c:out value="${u.correoElectronico()}"/></td>
                      <td>
                        <c:forEach var="r" items="${u.roles()}">
                          <span class="badge"><c:out value="${r.etiqueta}"/></span>
                        </c:forEach>
                      </td>
                      <td>
                        <span class="badge">
                          <c:out value="${u.activo() ? 'ACTIVO' : 'INACTIVO'}"/>
                        </span>
                      </td>
                      <td>
                        <div style="display:flex; gap:0.35rem; flex-wrap:wrap;">
                          <!-- Acción por método GET: cargar formulario de edición -->
                          <a href="${pageContext.request.contextPath}/usuarios?accion=editar&amp;id=${u.idUsuario()}"
                             class="btn btn-secondary" style="padding:0.25rem 0.6rem; min-height:32px;">
                            Editar (GET)
                          </a>

                          <!-- Acción por método POST: activar o desactivar cuenta -->
                          <form action="${pageContext.request.contextPath}/usuarios" method="post" style="margin:0;">
                            <input type="hidden" name="accion" value="cambiarEstado">
                            <input type="hidden" name="idUsuario" value="${u.idUsuario()}">
                            <input type="hidden" name="activo" value="${not u.activo()}">
                            <button type="submit" class="btn btn-secondary" style="padding:0.25rem 0.6rem; min-height:32px;">
                              <c:out value="${u.activo() ? 'Desactivar (POST)' : 'Activar (POST)'}"/>
                            </button>
                          </form>

                          <!-- Acción por método POST: eliminar registro -->
                          <form action="${pageContext.request.contextPath}/usuarios" method="post" style="margin:0;">
                            <input type="hidden" name="accion" value="eliminar">
                            <input type="hidden" name="idUsuario" value="${u.idUsuario()}">
                            <button type="submit" class="btn btn-danger" style="padding:0.25rem 0.6rem; min-height:32px;">
                              Eliminar (POST)
                            </button>
                          </form>
                        </div>
                      </td>
                    </tr>
                  </c:forEach>
                </c:otherwise>
              </c:choose>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </div>

  <%@ include file="pie.jspf" %>
</body>
</html>
