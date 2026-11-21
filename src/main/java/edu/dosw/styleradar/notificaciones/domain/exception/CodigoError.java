package edu.dosw.styleradar.notificaciones.domain.exception;

/**
 * Codigo estable de cada error. El texto de {@code detail} es libre y puede cambiar; el front decide que
 * mensaje mostrar a partir de este codigo.
 */
public enum CodigoError {
    // 400
    VALIDATION_ERROR,
    INVALID_PARAMETER,
    MALFORMED_REQUEST,
    // 401
    UNAUTHENTICATED,
    // 403
    ACCESS_DENIED,
    // 404
    ALERT_NOT_FOUND,
    NOTIFICATION_NOT_FOUND,
    NOT_FOUND,
    ROUTE_NOT_FOUND,
    // 409
    ALERT_ALREADY_ACTIVE,
    CONFLICT,
    // 422
    ITEM_HAS_STOCK,
    ITEM_NOT_FOUND,
    ACCOUNT_NOT_ALLOWED,
    SAVED_SEARCH_NOT_AVAILABLE,
    INVALID_ALERT,
    BUSINESS_RULE_VIOLATION,
    // 429
    RATE_LIMITED,
    // 5xx
    CATALOG_UNAVAILABLE,
    EXTERNAL_SERVICE_UNAVAILABLE,
    INTERNAL_ERROR
}
