package com.example.demo.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    SOLICITUD_INVALIDA(HttpStatus.BAD_REQUEST, "Solicitud inválida", "La solicitud no es válida"),
    VALIDACION_FALLIDA(HttpStatus.BAD_REQUEST, "Validación fallida", "Uno o más campos no son válidos"),
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", "Usuario o contraseña incorrectos"),
    NO_AUTENTICADO(HttpStatus.UNAUTHORIZED, "No autenticado", "Debe iniciar sesión para acceder a este recurso"),
    TOKEN_INVALIDO(HttpStatus.UNAUTHORIZED, "Token inválido", "El token de acceso no es válido"),
    TOKEN_EXPIRADO(HttpStatus.UNAUTHORIZED, "Token expirado", "La sesión expiró, inicie sesión nuevamente"),
    USUARIO_INACTIVO(HttpStatus.UNAUTHORIZED, "Usuario inactivo", "El usuario está desactivado"),
    SESION_INVALIDA(HttpStatus.UNAUTHORIZED, "Sesión inválida", "La sesión expiró o no es válida, inicie sesión nuevamente"),
    ACCESO_DENEGADO(HttpStatus.FORBIDDEN, "Acceso denegado", "No tiene permisos para realizar esta acción"),
    RECURSO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "Recurso no encontrado", "El recurso solicitado no existe"),
    METODO_NO_PERMITIDO(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido", "El método HTTP no está permitido para este recurso"),
    TIPO_DE_CONTENIDO_NO_SOPORTADO(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado", "El tipo de contenido de la solicitud no está soportado"),
    RECURSO_DUPLICADO(HttpStatus.CONFLICT, "Recurso duplicado", "El recurso ya existe"),
    STOCK_INSUFICIENTE(HttpStatus.CONFLICT, "Stock insuficiente", "No hay stock suficiente para completar la operación"),
    CONFLICTO_DE_DATOS(HttpStatus.CONFLICT, "Conflicto de datos", "La operación entra en conflicto con datos existentes"),
    SOLICITUD_DUPLICADA(HttpStatus.CONFLICT, "Solicitud duplicada", "Ya hay una venta en proceso con la misma Idempotency-Key, intente de nuevo en unos segundos"),
    MODIFICACION_CONCURRENTE(HttpStatus.CONFLICT, "Modificación concurrente", "El recurso fue modificado por otro usuario, recargue e intente de nuevo"),
    OPERACION_NO_PERMITIDA(HttpStatus.UNPROCESSABLE_ENTITY, "Operación no permitida", "La operación no está permitida"),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ocurrió un error inesperado, intente más tarde");

    private final HttpStatus status;
    private final String titulo;
    private final String mensaje;

    public static ErrorCode fromStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 401 -> NO_AUTENTICADO;
            case 403 -> ACCESO_DENEGADO;
            case 404 -> RECURSO_NO_ENCONTRADO;
            case 405 -> METODO_NO_PERMITIDO;
            case 409 -> CONFLICTO_DE_DATOS;
            case 415 -> TIPO_DE_CONTENIDO_NO_SOPORTADO;
            default -> status.is5xxServerError() ? ERROR_INTERNO : SOLICITUD_INVALIDA;
        };
    }
}
