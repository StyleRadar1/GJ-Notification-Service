package edu.dosw.styleradar.notificaciones.application.dto.response;

import edu.dosw.styleradar.notificaciones.domain.model.Pagina;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;

@Schema(description = "A page of results with its metadata")
public record PaginaResponse<T>(
        @Schema(description = "Items of the page")
        List<T> content,

        @Schema(description = "Page number, starting at 0", example = "0")
        int page,

        @Schema(description = "Requested page size", example = "20")
        int size,

        @Schema(description = "Total elements matching the filter", example = "42")
        long totalElements,

        @Schema(description = "Total pages", example = "3")
        int totalPages,

        @Schema(description = "There is a next page", example = "true")
        boolean hasNext,

        @Schema(description = "There is a previous page", example = "false")
        boolean hasPrevious
) {

    public static <D, T> PaginaResponse<T> de(Pagina<D> pagina, Function<D, T> convertir) {
        return new PaginaResponse<>(
                pagina.contenido().stream().map(convertir).toList(),
                pagina.page(),
                pagina.size(),
                pagina.totalElements(),
                pagina.totalPages(),
                pagina.hasNext(),
                pagina.hasPrevious());
    }
}
