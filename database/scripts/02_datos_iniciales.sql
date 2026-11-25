-- =============================================================================
-- SkillUp Pathway - Datos iniciales del catálogo de roles
-- Los valores de nombre_rol coinciden con la enumeración Java NombreRol.
-- No se insertan usuarios: la cuenta de administrador inicial se crea desde la
-- aplicación la primera vez que se ejecuta, para no dejar contraseñas en el
-- repositorio.
-- =============================================================================

USE skillup;

INSERT INTO rol (nombre_rol, descripcion)
VALUES
    ('ESTUDIANTE', 'Aprendiz que se inscribe en rutas de aprendizaje y presenta evaluaciones.'),
    ('INSTRUCTOR', 'Crea y administra módulos, contenidos y evaluaciones.'),
    ('MENTOR', 'Acompaña el avance de los estudiantes asignados.'),
    ('EMPRESA', 'Organización que vincula colaboradores a planes de capacitación.'),
    ('ADMINISTRADOR', 'Gestiona cuentas de usuario, roles y parámetros de la plataforma.')
AS nuevo
ON DUPLICATE KEY UPDATE
    descripcion = nuevo.descripcion;
