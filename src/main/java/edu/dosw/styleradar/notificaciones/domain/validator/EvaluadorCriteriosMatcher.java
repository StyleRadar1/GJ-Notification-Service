package edu.dosw.styleradar.notificaciones.domain.validator;

import edu.dosw.styleradar.notificaciones.domain.model.CriterioBusquedaVO;
import edu.dosw.styleradar.notificaciones.domain.model.DatosPrendaVO;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class EvaluadorCriteriosMatcher {

    public boolean coincideConBusqueda(CriterioBusquedaVO criterios, DatosPrendaVO prenda) {
        if (criterios == null || prenda == null) {
            return false;
        }
        if (prenda.stockDisponible() == null || prenda.stockDisponible() <= 0) {
            return false;
        }

        if (!coincideTextoLibre(criterios.textoLibre(), prenda.nombrePrenda(), prenda.marca(), prenda.categoria())) {
            return false;
        }
        if (!coincideIgualdadIgnorandoCaso(criterios.categoria(), prenda.categoria())) {
            return false;
        }
        if (!coincideIgualdadIgnorandoCaso(criterios.color(), prenda.color())) {
            return false;
        }
        if (!coincideTalla(criterios.talla(), prenda.tallasDisponibles())) {
            return false;
        }
        if (!coincideIgualdadIgnorandoCaso(criterios.marca(), prenda.marca())) {
            return false;
        }
        if (!coincideIgualdadIgnorandoCaso(criterios.estilo(), prenda.estilo())) {
            return false;
        }
        if (!coincideIgualdadIgnorandoCaso(criterios.localidad(), prenda.localidad())) {
            return false;
        }
        if (criterios.precioMin() != null && (prenda.precio() == null || prenda.precio().compareTo(criterios.precioMin()) < 0)) {
            return false;
        }
        if (criterios.precioMax() != null && (prenda.precio() == null || prenda.precio().compareTo(criterios.precioMax()) > 0)) {
            return false;
        }

        return true;
    }

    public boolean coincideItemAgotado(Long alertaItemId, String tallaObjetivo, DatosPrendaVO prenda) {
        if (alertaItemId == null || prenda == null || !alertaItemId.equals(prenda.itemId())) {
            return false;
        }
        if (prenda.stockDisponible() == null || prenda.stockDisponible() <= 0) {
            return false;
        }
        return coincideTalla(tallaObjetivo, prenda.tallasDisponibles());
    }

    private boolean coincideTextoLibre(String textoFiltro, String nombrePrenda, String marca, String categoria) {
        if (esVacio(textoFiltro)) {
            return true;
        }
        String filtroNorm = normalizar(textoFiltro);
        String compuesto = normalizar((nombrePrenda != null ? nombrePrenda : "") + " "
                + (marca != null ? marca : "") + " "
                + (categoria != null ? categoria : ""));
        String[] terminos = filtroNorm.split("\\s+");
        for (String termino : terminos) {
            if (!termino.isBlank() && !compuesto.contains(termino)) {
                return false;
            }
        }
        return true;
    }

    private boolean coincideIgualdadIgnorandoCaso(String esperado, String actual) {
        if (esVacio(esperado)) {
            return true;
        }
        if (esVacio(actual)) {
            return false;
        }
        return normalizar(esperado).equals(normalizar(actual));
    }

    private boolean coincideTalla(String tallaEsperada, List<String> tallasDisponibles) {
        if (esVacio(tallaEsperada)) {
            return true;
        }
        if (tallasDisponibles == null || tallasDisponibles.isEmpty()) {
            return false;
        }
        String esperadaNorm = normalizar(tallaEsperada);
        return tallasDisponibles.stream()
                .filter(t -> !esVacio(t))
                .map(this::normalizar)
                .anyMatch(esperadaNorm::equals);
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String normalizar(String texto) {
        String nfd = Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
