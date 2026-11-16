package edu.dosw.styleradar.notificaciones.application.dto.response;

import edu.dosw.styleradar.notificaciones.domain.model.Pagina;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;

@Schema(description = "Una pagina de resultados con sus metadatos")
public record PaginaResponse<T>(
        @Schema(description = "Elementos de la pagina")
        List<T> content,

        @Schema(description = "Numero de pagina, empieza en 0", example = "0")
        int page,

        @Schema(description = "Tamano de pagina pedido", example = "20")
        int size,

        @Schema(description = "Total de elementos que cumplen el filtro", example = "42")
        long totalElements,

        @Schema(description = "Total de paginas", example = "3")
        int totalPages,

        @Schema(description = "Hay una pagina siguiente", example = "true")
        boolean hasNext,

        @Schema(description = "Hay una pagina anterior", example = "false")
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
