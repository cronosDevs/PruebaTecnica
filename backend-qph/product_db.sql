/******************************
-  Nombre:      1.- Creación de base de datos
-  Descripción: Definición y creación del contenedor principal
               para el sistema de gestión de productos.
-  Tipo:        Estructura (Database)
-  Versión:     1.0
-  Autor:       Stalin Ladino
-  Fecha:       13-04-2026
*******************************/
CREATE DATABASE products_db;


/******************************
-  Nombre:      2.- Creación de tabla de productos
-  Descripción: Definición y creación de la tabla principal
               que almacena el catálogo de productos con
               control de precios y stock disponible.
-  Tipo:        Estructura (Table)
-  Versión:     1.0
-  Autor:       Stalin Ladino
-  Fecha:       13-04-2026
*******************************/
CREATE TABLE public.products (
    id      serial         NOT NULL,
    nombre  varchar(255)   NOT NULL,
    precio  numeric(38, 2) NOT NULL,
    stock   int4           NOT NULL,
    CONSTRAINT products_pkey       PRIMARY KEY (id),
    CONSTRAINT products_stock_check CHECK ((stock >= 0))
);


CREATE TABLE public.roles (
    id          serial        NOT NULL,
    nombre      varchar(30)   NOT NULL,
    descripcion varchar(150),
    CONSTRAINT roles_pkey      PRIMARY KEY (id),
    CONSTRAINT roles_nombre_uk UNIQUE (nombre)
);


CREATE TABLE public.users (
    id                  bigserial     NOT NULL,
    username            varchar(50)   NOT NULL,
    password_hash       varchar(100)  NOT NULL,
    nombre_completo     varchar(120)  NOT NULL,
    email               varchar(150)  NOT NULL,
    rol_id              int4          NOT NULL,
    activo              boolean       NOT NULL DEFAULT true,
    fecha_creacion      timestamptz   NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz,
    CONSTRAINT users_pkey        PRIMARY KEY (id),
    CONSTRAINT users_username_uk UNIQUE (username),
    CONSTRAINT users_email_uk    UNIQUE (email),
    CONSTRAINT users_rol_fk      FOREIGN KEY (rol_id) REFERENCES public.roles (id)
);


ALTER SEQUENCE public.products_id_seq AS int8;

ALTER TABLE public.products
    ALTER COLUMN id TYPE int8,
    ADD COLUMN activo              boolean     NOT NULL DEFAULT true,
    ADD COLUMN version             int8        NOT NULL DEFAULT 0,
    ADD COLUMN fecha_creacion      timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN fecha_actualizacion timestamptz,
    ADD CONSTRAINT products_precio_check CHECK ((precio > 0));


CREATE TABLE public.sales (
    id                 bigserial      NOT NULL,
    codigo             varchar(30)    NOT NULL,
    usuario_id         int8           NOT NULL,
    total              numeric(38, 2) NOT NULL DEFAULT 0,
    clave_idempotencia varchar(64),
    fecha_creacion     timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT sales_pkey           PRIMARY KEY (id),
    CONSTRAINT sales_codigo_uk      UNIQUE (codigo),
    CONSTRAINT sales_idempotencia_uk UNIQUE (usuario_id, clave_idempotencia),
    CONSTRAINT sales_usuario_fk     FOREIGN KEY (usuario_id) REFERENCES public.users (id),
    CONSTRAINT sales_total_check    CHECK ((total >= 0))
);

CREATE INDEX sales_usuario_fecha_idx ON public.sales (usuario_id, fecha_creacion DESC);


CREATE TABLE public.sale_items (
    id              bigserial      NOT NULL,
    venta_id        int8           NOT NULL,
    producto_id     int8           NOT NULL,
    cantidad        int4           NOT NULL,
    precio_unitario numeric(38, 2) NOT NULL,
    subtotal        numeric(38, 2) NOT NULL,
    CONSTRAINT sale_items_pkey             PRIMARY KEY (id),
    CONSTRAINT sale_items_venta_fk         FOREIGN KEY (venta_id) REFERENCES public.sales (id) ON DELETE CASCADE,
    CONSTRAINT sale_items_producto_fk      FOREIGN KEY (producto_id) REFERENCES public.products (id),
    CONSTRAINT sale_items_venta_producto_uk UNIQUE (venta_id, producto_id),
    CONSTRAINT sale_items_cantidad_check   CHECK ((cantidad > 0)),
    CONSTRAINT sale_items_precio_check     CHECK ((precio_unitario > 0)),
    CONSTRAINT sale_items_subtotal_check   CHECK ((subtotal = cantidad * precio_unitario))
);

CREATE INDEX sale_items_producto_idx ON public.sale_items (producto_id);


CREATE TABLE public.inventory_movements (
    id               bigserial    NOT NULL,
    producto_id      int8         NOT NULL,
    venta_id         int8,
    usuario_id       int8         NOT NULL,
    tipo             varchar(20)  NOT NULL,
    cantidad         int4         NOT NULL,
    stock_resultante int4         NOT NULL,
    fecha            timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT inventory_movements_pkey        PRIMARY KEY (id),
    CONSTRAINT inventory_movements_producto_fk FOREIGN KEY (producto_id) REFERENCES public.products (id),
    CONSTRAINT inventory_movements_venta_fk    FOREIGN KEY (venta_id) REFERENCES public.sales (id),
    CONSTRAINT inventory_movements_usuario_fk  FOREIGN KEY (usuario_id) REFERENCES public.users (id),
    CONSTRAINT inventory_movements_tipo_check  CHECK ((tipo IN ('ENTRADA', 'VENTA', 'AJUSTE'))),
    CONSTRAINT inventory_movements_cantidad_check CHECK ((cantidad <> 0)),
    CONSTRAINT inventory_movements_stock_check CHECK ((stock_resultante >= 0))
);

CREATE INDEX inventory_movements_producto_fecha_idx ON public.inventory_movements (producto_id, fecha DESC);


CREATE TABLE public.refresh_tokens (
    id               bigserial    NOT NULL,
    usuario_id       int8         NOT NULL,
    token_hash       varchar(64)  NOT NULL,
    fecha_creacion   timestamptz  NOT NULL DEFAULT now(),
    fecha_expiracion timestamptz  NOT NULL,
    fecha_revocacion timestamptz,
    CONSTRAINT refresh_tokens_pkey       PRIMARY KEY (id),
    CONSTRAINT refresh_tokens_hash_uk    UNIQUE (token_hash),
    CONSTRAINT refresh_tokens_usuario_fk FOREIGN KEY (usuario_id) REFERENCES public.users (id)
);

CREATE INDEX refresh_tokens_usuario_idx ON public.refresh_tokens (usuario_id);


INSERT INTO public.roles (nombre, descripcion) VALUES
    ('ADMIN',    'Acceso total, incluida la gestión de usuarios'),
    ('VENDEDOR', 'Registra ventas y consulta únicamente las propias');

INSERT INTO public.users (username, password_hash, nombre_completo, email, rol_id)
SELECT 'admin',
       '$2a$10$eMSPwJfxqXRuTXFmPRKy2.CvXAMpOQ1q3Wgvc4cOkli9RuOBK5Cge',
       'Administrador',
       'admin@producthub.local',
       r.id
FROM public.roles r
WHERE r.nombre = 'ADMIN';