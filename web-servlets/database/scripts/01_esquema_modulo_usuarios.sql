-- =============================================================================
-- SkillUp Pathway - Script DDL del módulo de gestión de usuarios (Sprint 1)
-- Motor: MySQL 8.4 (InnoDB, utf8mb4)
-- Requisitos: RF-01 Registro, RF-02 Inicio de sesión, RF-03 Perfil,
--             RF-09 Gestión de usuarios (historias HU-01, HU-02 y HU-07)
-- Origen: modelo lógico GA4-220501095-AA1-EV01 (tablas usuario, rol y
--         usuario_rol) con las convenciones de GA7-220501096-AA1-EV02:
--         snake_case, tablas en singular, PK id_<tabla>, prefijos pk_, fk_,
--         uq_, idx_ y chk_, columnas de auditoría fecha_creacion y
--         fecha_actualizacion.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS skillup
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE skillup;

-- Catálogo de roles de la plataforma.
CREATE TABLE IF NOT EXISTS rol (
    id_rol BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre_rol VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255) NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_rol PRIMARY KEY (id_rol),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cuentas de usuario: datos de identificación y de autenticación.
-- intentos_fallidos y bloqueado_hasta implementan el criterio CA-03 de HU-02
-- (bloqueo temporal de 15 minutos después de 5 intentos fallidos).
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tipo_documento VARCHAR(30) NOT NULL,
    numero_documento VARCHAR(30) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo_electronico VARCHAR(150) NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    intentos_fallidos TINYINT UNSIGNED NOT NULL DEFAULT 0,
    bloqueado_hasta DATETIME NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_correo UNIQUE (correo_electronico),
    CONSTRAINT uq_usuario_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT chk_usuario_tipo_documento
        CHECK (tipo_documento IN ('CC', 'CE', 'TI', 'PASAPORTE')),
    CONSTRAINT chk_usuario_activo CHECK (activo IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Relación muchos a muchos entre usuario y rol.
CREATE TABLE IF NOT EXISTS usuario_rol (
    id_usuario_rol BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario BIGINT UNSIGNED NOT NULL,
    id_rol BIGINT UNSIGNED NOT NULL,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario_rol PRIMARY KEY (id_usuario_rol),
    CONSTRAINT uq_usuario_rol UNIQUE (id_usuario, id_rol),
    CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_usuario_rol_rol FOREIGN KEY (id_rol)
        REFERENCES rol (id_rol)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    INDEX idx_usuario_rol_rol (id_rol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
