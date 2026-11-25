<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    boolean modoEdicion = Boolean.TRUE.equals(request.getAttribute("modoEdicion"));
    String tituloFormulario = modoEdicion
            ? "Actualización de Perfil y Cuenta de Usuario (RF-03 / RF-09)"
            : "Registro de Nueva Cuenta de Usuario (RF-01 — Historia HU-01)";
%>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>SkillUp Pathway — <%= tituloFormulario %></title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
  <%@ include file="cabecera.jspf" %>

  <div class="app-shell">
    <aside class="app-sidebar" aria-label="Reglas de negocio del formulario">
      <h2 style="margin-top:0; font-size:1.05rem;">Criterios de Validación</h2>
      <ul style="padding-left:1.1rem; margin:0.4rem 0; font-size:0.86rem;">
        <li><strong>CA-01 (HU-01):</strong> Correo electrónico con formato válido y único en la base de datos.</li>
        <li><strong>CA-02 (HU-01):</strong> Contraseña de mínimo 8 caracteres con mayúscula, minúscula, dígito y símbolo especial.</li>
        <li><strong>Documento:</strong> Tipo de documento admitido (<code>CC</code>, <code>CE</code>, <code>TI</code>, <code>PASAPORTE</code>) y número único.</li>
      </ul>
    </aside>

    <main id="contenido-principal" class="main-workspace">
      <section class="card-panel" aria-labelledby="titulo-form-usuario">
        <h1 id="titulo-form-usuario" style="margin-top:0; font-size:1.25rem;">
          <%= tituloFormulario %>
        </h1>

        <c:if test="${not empty requestScope.erroresValidacion}">
          <div class="alert-banner alert-error" role="alert">
            <strong>Verifique los siguientes errores de validación antes de continuar:</strong>
            <ul style="margin:0.4rem 0 0 0; padding-left:1.2rem;">
              <c:forEach var="err" items="${requestScope.erroresValidacion}">
                <li><c:out value="${err}"/></li>
              </c:forEach>
            </ul>
          </div>
        </c:if>

        <c:if test="${not empty requestScope.mensajeError}">
          <div class="alert-banner alert-error" role="alert">
            <c:out value="${requestScope.mensajeError}"/>
          </div>
        </c:if>

        <form action="${pageContext.request.contextPath}/usuarios" method="post">
          <input type="hidden" name="accion" value="${requestScope.modoEdicion ? 'actualizar' : 'registrar'}">
          <c:if test="${requestScope.modoEdicion}">
            <input type="hidden" name="idUsuario" value="${requestScope.usuarioFormulario.idUsuario()}">
          </c:if>

          <div class="form-grid">
            <div class="field">
              <label for="tipoDocumento">Tipo de Documento *</label>
              <select id="tipoDocumento" name="tipoDocumento" required>
                <c:forEach var="td" items="${requestScope.tiposDocumento}">
                  <option value="${td.name()}"
                    <c:if test="${requestScope.usuarioFormulario.tipoDocumento() eq td}">selected</c:if>>
                    <c:out value="${td.name()} — ${td.descripcion}"/>
                  </option>
                </c:forEach>
              </select>
            </div>

            <div class="field">
              <label for="numeroDocumento">Número de Documento *</label>
              <span class="hint">De 5 a 20 caracteres alfanuméricos sin puntos</span>
              <input
                type="text"
                id="numeroDocumento"
                name="numeroDocumento"
                value="<c:out value='${requestScope.usuarioFormulario.numeroDocumento()}'/>"
                required
              >
            </div>

            <div class="field">
              <label for="nombres">Nombres *</label>
              <input
                type="text"
                id="nombres"
                name="nombres"
                value="<c:out value='${requestScope.usuarioFormulario.nombres()}'/>"
                required
              >
            </div>

            <div class="field">
              <label for="apellidos">Apellidos *</label>
              <input
                type="text"
                id="apellidos"
                name="apellidos"
                value="<c:out value='${requestScope.usuarioFormulario.apellidos()}'/>"
                required
              >
            </div>

            <div class="field">
              <label for="correoElectronico">Correo Electrónico *</label>
              <input
                type="email"
                id="correoElectronico"
                name="correoElectronico"
                value="<c:out value='${requestScope.usuarioFormulario.correoElectronico()}'/>"
                required
              >
            </div>

            <c:if test="${not requestScope.modoEdicion}">
              <div class="field">
                <label for="contrasena">Contraseña Inicial *</label>
                <span class="hint">Mínimo 8 caracteres, mayúscula, minúscula, número y carácter especial</span>
                <input
                  type="password"
                  id="contrasena"
                  name="contrasena"
                  required
                  minlength="8"
                >
              </div>
            </c:if>

            <div class="field">
              <label for="rol">Rol Asignado *</label>
              <select id="rol" name="rol" required>
                <c:forEach var="rolDisp" items="${requestScope.rolesDisponibles}">
                  <option value="${rolDisp.name()}"
                    <c:if test="${requestScope.rolSeleccionado eq rolDisp.name()}">selected</c:if>>
                    <c:out value="${rolDisp.etiqueta}"/>
                  </option>
                </c:forEach>
              </select>
            </div>
          </div>

          <div class="btn-group">
            <button type="submit" class="btn btn-primary">
              <c:out value="${requestScope.modoEdicion ? 'Guardar Cambios (HTTP POST)' : 'Registrar Usuario (HTTP POST)'}"/>
            </button>
            <a href="${pageContext.request.contextPath}/usuarios" class="btn btn-secondary">
              Cancelar y Volver al Listado (HTTP GET)
            </a>
          </div>
        </form>
      </section>
    </main>
  </div>

  <%@ include file="pie.jspf" %>
</body>
</html>
