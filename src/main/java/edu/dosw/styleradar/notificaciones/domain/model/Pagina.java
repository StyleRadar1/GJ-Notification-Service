package edu.dosw.styleradar.notificaciones.domain.model;

import java.util.List;

/** Una pagina de resultados, sin depender de Spring Data. */
public record Pagina<T>(List<T> contenido, int page, int size, long totalElements) {

    public int totalPages() {
        return size <= 0 ? 0 : (int) ((totalElements + size - 1) / size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public boolean hasPrevious() {
        return page > 0 && totalPages() > 0;
    }
}
