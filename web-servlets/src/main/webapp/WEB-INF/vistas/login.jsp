<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%!
    private static final DateTimeFormatter FORMATO_AUDITORIA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String obtenerMarcaTiempoServidor() {
        return LocalDateTime.now().format(FORMATO_AUDITORIA);
    }
%>
<%
    String marcaTiempo = obtenerMarcaTiempoServidor();
    String metodoRecibido = request.getMethod();
%>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>SkillUp Pathway — Inicio de Sesión (RF-02 / HU-02)</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
  <%@ include file="cabecera.jspf" %>

  <div class="app-shell">
    <aside class="app-sidebar" aria-label="Estado de sesión HTTP">
      <h2 style="margin-top:0; font-size:1.05rem;">Contexto JSP de Sesión</h2>
      <p style="margin:0.3rem 0;"><strong>Método HTTP actual:</strong> <code><%= metodoRecibido %></code></p>
      <p style="margin:0.3rem 0;"><strong>Hora servidor:</strong> <code><%= marcaTiempo %></code></p>
      <c:choose>
        <c:when test="${not empty sessionScope.usuarioAutenticado}">
          <p style="margin:0.3rem 0;"><strong>Sesión activa:</strong> <c:out value="${sessionScope.usuarioAutenticado.nombreCompleto()}"/></p>
          <p style="margin:0.3rem 0;"><strong>Documento:</strong> <c:out value="${sessionScope.usuarioAutenticado.tipoDocumento()}"/> <c:out value="${sessionScope.usuarioAutenticado.numeroDocumento()}"/></p>
          <form action="${pageContext.request.contextPath}/acceso" method="get">
            <input type="hidden" name="accion" value="salir">
            <button type="submit" class="btn btn-secondary" style="width:100%;">Cerrar Sesión (GET)</button>
          </form>
        </c:when>
        <c:otherwise>
          <p style="margin:0.3rem 0;">No hay sesión autenticada activa.</p>
        </c:otherwise>
      </c:choose>
    </aside>

    <main id="contenido-principal" class="main-workspace">
      <section class="card-panel" aria-labelledby="titulo-acceso-jsp">
        <h1 id="titulo-acceso-jsp" style="margin-top:0; font-size:1.25rem;">
          Autenticación de Usuarios en SkillUp Pathway (RF-02 — Historia HU-02)
        </h1>

        <c:if test="${not empty requestScope.mensajeExito}">
          <div class="alert-banner" role="status">
            <c:out value="${requestScope.mensajeExito}"/>
          </div>
        </c:if>

        <c:if test="${not empty requestScope.mensajeError}">
          <div class="alert-banner alert-error" role="alert">
            <c:out value="${requestScope.mensajeError}"/>
          </div>
        </c:if>

        <form action="${pageContext.request.contextPath}/acceso" method="post">
          <div class="form-grid">
            <div class="field">
              <label for="correoElectronico">Correo Electrónico Institucional *</label>
              <span class="hint">Ingrese el correo registrado en SkillUp Pathway</span>
              <input
                type="email"
                id="correoElectronico"
                name="correoElectronico"
                value="<c:out value='${requestScope.correoIngresado}'/>"
                required
                autocomplete="email"
              >
            </div>
            <div class="field">
              <label for="contrasena">Contraseña *</label>
              <span class="hint">Tras 5 intentos fallidos la cuenta se bloquea por 15 minutos (CA-03 HU-02)</span>
              <input
                type="password"
                id="contrasena"
                name="contrasena"
                required
                minlength="8"
                autocomplete="current-password"
              >
            </div>
          </div>
          <div class="btn-group">
            <button type="submit" class="btn btn-primary">Iniciar Sesión (POST /acceso)</button>
            <a href="${pageContext.request.contextPath}/usuarios?accion=nuevo" class="btn btn-secondary">
              Crear Cuenta de Aprendiz (GET /usuarios?accion=nuevo)
            </a>
          </div>
        </form>
      </section>
    </main>
  </div>

  <%@ include file="pie.jspf" %>
</body>
</html>
