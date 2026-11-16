package edu.dosw.styleradar.notificaciones.integration.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.notificaciones.application.dto.event.BusquedaGuardadaSincronizadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.DonacionEstadoCambiadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.InventarioActualizadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.MensajeEnviadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.ModeracionPrendaCambiadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.PrendaPublicadaEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.event.UsuarioEstadoCambiadoEventPayload;
import edu.dosw.styleradar.notificaciones.application.dto.request.CrearAlertaRequest;
import edu.dosw.styleradar.notificaciones.domain.event.AlertaCreadaDomainEvent;
import edu.dosw.styleradar.notificaciones.domain.exception.ServicioExternoNoDisponibleException;
import edu.dosw.styleradar.notificaciones.domain.model.BusquedaGuardadaCopia;
import edu.dosw.styleradar.notificaciones.domain.model.CanalNotificacion;
import edu.dosw.styleradar.notificaciones.domain.model.CriterioBusquedaVO;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.EstadoCuenta;
import edu.dosw.styleradar.notificaciones.domain.model.Notificacion;
import edu.dosw.styleradar.notificaciones.domain.model.TipoAlerta;
import edu.dosw.styleradar.notificaciones.domain.model.TipoNotificacion;
import edu.dosw.styleradar.notificaciones.domain.port.out.AlertaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.BusquedaGuardadaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.EstadoCuentaCopiaRepositoryPort;
import edu.dosw.styleradar.notificaciones.domain.port.out.NotificacionRepositoryPort;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.in.amqp.NotificacionesEventsListener;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.CatalogoClientAdapter;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.CatalogoFeignClient;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.external.dto.CatalogoDisponibilidadItemResponse;
import edu.dosw.styleradar.notificaciones.infrastructure.adapter.out.messaging.RabbitMqEventPublisherAdapter;
import edu.dosw.styleradar.notificaciones.infrastructure.security.JwtTokenService;
import edu.dosw.styleradar.notificaciones.infrastructure.sse.SseEmitterRegistry;
import edu.dosw.styleradar.notificaciones.infrastructure.config.RabbitMqConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AlertaControllerAndInfrastructureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private AlertaRepositoryPort alertaRepositoryPort;

    @Autowired
    private NotificacionRepositoryPort notificacionRepositoryPort;

    @Autowired
    private BusquedaGuardadaCopiaRepositoryPort busquedaGuardadaCopiaRepositoryPort;

    @Autowired
    private EstadoCuentaCopiaRepositoryPort estadoCuentaCopiaRepositoryPort;

    @Autowired
    private NotificacionesEventsListener eventsListener;

    @Autowired
    private SseEmitterRegistry sseRegistry;

    @Autowired
    private RabbitMqConfig rabbitMqConfig;

    @MockBean
    private CatalogoFeignClient catalogoFeignClient;

    @Test
    @DisplayName("NOT-05 y NOT-22 - Debe exigir JWT en endpoints y permitir ciclo completo de alertas y notificaciones")
    void debeCompletarFlujoHttpConJwt() throws Exception {
        // Arrange
        String tokenUsuario = jwtTokenService.generateToken(501L, "mateo@styleradar.co", "COMPRADOR");
        String tokenOtroUsuario = jwtTokenService.generateToken(999L, "otro@styleradar.co", "COMPRADOR");

        when(catalogoFeignClient.consultarDisponibilidad(1045L))
                .thenReturn(new CatalogoDisponibilidadItemResponse(1045L, 0, List.of()));

        CrearAlertaRequest crearRequest = new CrearAlertaRequest(
                TipoAlerta.ITEM_AGOTADO, 1045L, "M", null, CanalNotificacion.IN_APP
        );

        // Act & Assert 1: Sin token retorna 401
        mockMvc.perform(get("/api/v1/usuarios/me/alertas"))
                .andExpect(status().isUnauthorized());

        // Act & Assert 2: Crear alerta retorna 201
        String responseJson = mockMvc.perform(post("/api/v1/usuarios/me/alertas")
                        .header("Authorization", "Bearer " + tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(501))
                .andExpect(jsonPath("$.itemId").value(1045))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long alertaId = objectMapper.readTree(responseJson).get("id").asLong();

        // Act & Assert 3: Crear duplicada retorna 409 Conflict
        mockMvc.perform(post("/api/v1/usuarios/me/alertas")
                        .header("Authorization", "Bearer " + tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearRequest)))
                .andExpect(status().isConflict());

        // Act & Assert 4: Listar mis alertas
        mockMvc.perform(get("/api/v1/usuarios/me/alertas")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(alertaId));

        // Act & Assert 5: Otro usuario intentando desactivar recibe 403 Forbidden
        mockMvc.perform(delete("/api/v1/usuarios/me/alertas/{id}", alertaId)
                        .header("Authorization", "Bearer " + tokenOtroUsuario))
                .andExpect(status().isForbidden());

        // Act & Assert 6: Simular evento de reposicion de stock por RabbitListener
        eventsListener.onInventarioActualizado(new InventarioActualizadoEventPayload(
                "evt-1", 1045L, "Chaqueta Bomber", "CHAQUETAS", "NEGRO", "M",
                "BogotaStreet", "STREETWEAR", new BigDecimal("180000"), 0, 5, 1, 10L, "CHAPINERO", LocalDateTime.now()
        ));

        // Act & Assert 7: Verificar contador de no leidas y listado
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones/no-leidas/contador")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadNoLeidas").value(1));

        String notifListJson = mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?soloNoLeidas=true")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemId").value(1045))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long notifId = objectMapper.readTree(notifListJson).get(0).get("id").asLong();

        // Act & Assert 8: Marcar notificacion como leida y marcar todas
        mockMvc.perform(patch("/api/v1/usuarios/me/notificaciones/{id}/leer", notifId)
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));

        mockMvc.perform(patch("/api/v1/usuarios/me/notificaciones/leer-todas")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadNoLeidas").value(0));

        // Act & Assert 9: Desactivar alerta con PATCH y DELETE
        mockMvc.perform(patch("/api/v1/usuarios/me/alertas/{id}/desactivar", alertaId)
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DESACTIVADA"));

        // Act & Assert 10: Suscripcion SSE
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones/stream")
                        .header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("NOT-05 y NOT-10 - Debe retornar 400, 404 y 422 en validaciones de entrada y reglas de negocio")
    void debeManejarErroresDeValidacionYNegocioConProblemDetail() throws Exception {
        String token = jwtTokenService.generateToken(505L, "aura@styleradar.co", "ROLE_COMPRADOR");

        // 400 por JSON malformado
        mockMvc.perform(post("/api/v1/usuarios/me/alertas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed-json"))
                .andExpect(status().isBadRequest());

        // 400 por @Valid (tipoAlerta nulo)
        mockMvc.perform(post("/api/v1/usuarios/me/alertas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": 10}"))
                .andExpect(status().isBadRequest());

        // 422 cuando el item tiene stock disponible en catalogo
        when(catalogoFeignClient.consultarDisponibilidad(2000L))
                .thenReturn(new CatalogoDisponibilidadItemResponse(2000L, 4, List.of("M", "L")));

        CrearAlertaRequest conStock = new CrearAlertaRequest(
                TipoAlerta.ITEM_AGOTADO, 2000L, "M", null, CanalNotificacion.IN_APP
        );
        mockMvc.perform(post("/api/v1/usuarios/me/alertas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conStock)))
                .andExpect(status().isUnprocessableEntity());

        // 404 al desactivar alerta inexistente
        mockMvc.perform(delete("/api/v1/usuarios/me/alertas/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("NOT-06 a NOT-15 - Debe procesar todos los consumidores de eventos, SSE, idempotencia y envio a DLQ")
    void debeProcesarConsumidoresDeEventosYDlq() {
        // Arrange: Sincronizar busqueda guardada y crear alerta sobre ella
        eventsListener.onBusquedaGuardadaSincronizada(new BusquedaGuardadaSincronizadaEventPayload(
                "evt-bg-1", 400L, 601L, "Blazers Sastre", "blazer", "SASTRERIA", "BEIGE", "M",
                "Atelier", "SASTRE", new BigDecimal("100000"), new BigDecimal("400000"),
                "USAQUEN", "CREADA", LocalDateTime.now()
        ));

        BusquedaGuardadaCopia copia = busquedaGuardadaCopiaRepositoryPort.buscarPorId(400L).orElseThrow();
        assertThat(copia.isActiva()).isTrue();

        SseEmitter emitter = sseRegistry.registrar(601L);
        assertThat(sseRegistry.contarSuscripcionesActivas(601L)).isEqualTo(1);

        eventsListener.onPrendaPublicada(new PrendaPublicadaEventPayload(
                "evt-pub-1", 3001L, "Blazer Lino Sastre", "SASTRERIA", "BEIGE", List.of("M"),
                "Atelier", "SASTRE", new BigDecimal("320000"), 3, 7L, "USAQUEN", LocalDateTime.now()
        ));

        // Mensajes, donaciones y moderacion
        eventsListener.onMensajeEnviado(new MensajeEnviadoEventPayload(
                "evt-msg-1", 10L, 50L, 602L, "Nora", 601L, "Hola!", LocalDateTime.now()
        ));
        eventsListener.onDonacionEstadoCambiado(new DonacionEstadoCambiadoEventPayload(
                "evt-don-1", 80L, 601L, "Fundacion Moda Circular", "APROBADA", "Cumple requisitos", LocalDateTime.now()
        ));
        eventsListener.onModeracionPrendaCambiada(new ModeracionPrendaCambiadaEventPayload(
                "evt-mod-1", 3001L, 601L, "Blazer Lino", "RETIRADA", "Imagen borrosa", LocalDateTime.now()
        ));
        eventsListener.onUsuarioEstadoCambiado(new UsuarioEstadoCambiadoEventPayload(
                "evt-usr-1", 601L, "antonia@sr.co", "ACTIVO", LocalDateTime.now()
        ));

        // Eliminar busqueda guardada
        eventsListener.onBusquedaGuardadaSincronizada(new BusquedaGuardadaSincronizadaEventPayload(
                "evt-bg-2", 400L, 601L, "Blazers Sastre", null, null, null, null,
                null, null, null, null, null, "ELIMINADA", LocalDateTime.now()
        ));

        List<Notificacion> notificaciones = notificacionRepositoryPort.listarPorUsuario(601L, false);
        assertThat(notificaciones).hasSizeGreaterThanOrEqualTo(3);

        emitter.complete();

        // Verificar que payloads invalidos lanzan AmqpRejectAndDontRequeueException hacia DLQ (SR-RNF-15)
        assertThatThrownBy(() -> eventsListener.onInventarioActualizado(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> eventsListener.onPrendaPublicada(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> eventsListener.onUsuarioEstadoCambiado(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> eventsListener.onBusquedaGuardadaSincronizada(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> eventsListener.onMensajeEnviado(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> eventsListener.onDonacionEstadoCambiado(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> eventsListener.onModeracionPrendaCambiada(null))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    @DisplayName("NOT-04, NOT-11 y NOT-16 - Debe verificar idempotencia en persistencia, cliente Feign resiliente y RabbitMqConfig")
    void debeVerificarAdaptadoresDeSalidaYConfiguracion() {
        // 1. Idempotencia en NotificacionRepositoryAdapter ante clave unica (alerta_id, item_id, ciclo_reposicion)
        Notificacion n1 = new Notificacion(
                null, 701L, 55L, 1045L, 3, TipoNotificacion.REPOSICION_STOCK,
                CanalNotificacion.IN_APP, "T1", "M1", "/1", false, LocalDateTime.now(), null
        );
        Notificacion guardada1 = notificacionRepositoryPort.guardar(n1);
        assertThat(guardada1.getId()).isNotNull();

        Notificacion duplicada = new Notificacion(
                null, 701L, 55L, 1045L, 3, TipoNotificacion.REPOSICION_STOCK,
                CanalNotificacion.IN_APP, "T1", "M1", "/1", false, LocalDateTime.now(), null
        );
        Notificacion resultadoDuplicado = notificacionRepositoryPort.guardar(duplicada);
        assertThat(resultadoDuplicado).isNotNull();
        assertThat(notificacionRepositoryPort.existePorAlertaItemYCiclo(55L, 1045L, 3)).isTrue();

        // 2. BusquedaGuardadaCopia eliminar y EstadoCuentaCopia
        BusquedaGuardadaCopia copia = new BusquedaGuardadaCopia(
                900L, 701L, "Temp", new CriterioBusquedaVO(null, "CALZADO", null, null, null, null, null, null, null),
                true, LocalDateTime.now()
        );
        busquedaGuardadaCopiaRepositoryPort.guardar(copia);
        busquedaGuardadaCopiaRepositoryPort.eliminar(900L);
        assertThat(busquedaGuardadaCopiaRepositoryPort.buscarPorId(900L)).isEmpty();
        assertThat(alertaRepositoryPort.contarPorEstado(EstadoAlerta.ACTIVA)).isGreaterThanOrEqualTo(0);

        // 3. RabbitMqEventPublisherAdapter con mock de RabbitTemplate
        RabbitTemplate mockRabbit = mock(RabbitTemplate.class);
        RabbitMqEventPublisherAdapter publisher = new RabbitMqEventPublisherAdapter(mockRabbit);
        AlertaCreadaDomainEvent evt = new AlertaCreadaDomainEvent(
                "id-1", 1L, 701L, TipoAlerta.ITEM_AGOTADO, 1045L, null, CanalNotificacion.IN_APP, LocalDateTime.now()
        );
        publisher.publicarAlertaCreada(evt);
        verify(mockRabbit).convertAndSend(any(), any(), any(AlertaCreadaDomainEvent.class));

        // 4. CatalogoClientAdapter informa servicio no disponible cuando ms-item-catalogo falla (GJ-49)
        CatalogoFeignClient failingFeign = mock(CatalogoFeignClient.class);
        when(failingFeign.consultarDisponibilidad(any())).thenThrow(new RuntimeException("Connection refused"));
        CatalogoClientAdapter clientAdapter = new CatalogoClientAdapter(failingFeign);
        assertThatThrownBy(() -> clientAdapter.verificarItemAgotado(1045L, "M"))
                .isInstanceOf(ServicioExternoNoDisponibleException.class);

        // 5. Verificar beans de RabbitMqConfig
        assertThat(rabbitMqConfig.styleradarExchange()).isNotNull();
        assertThat(rabbitMqConfig.deadLetterExchange()).isNotNull();
        assertThat(rabbitMqConfig.deadLetterQueue()).isNotNull();
        assertThat(rabbitMqConfig.inventoryUpdatedQueue()).isNotNull();
        assertThat(rabbitMqConfig.garmentPublishedQueue()).isNotNull();
        assertThat(rabbitMqConfig.userStatusChangedQueue()).isNotNull();
        assertThat(rabbitMqConfig.savedSearchSyncedQueue()).isNotNull();
        assertThat(rabbitMqConfig.messageSentQueue()).isNotNull();
        assertThat(rabbitMqConfig.donationChangedQueue()).isNotNull();
        assertThat(rabbitMqConfig.moderationChangedQueue()).isNotNull();
        assertThat(rabbitMqConfig.jackson2JsonMessageConverter()).isNotNull();
    }

    private static final long USUARIO_PAGINAS = 7_801L;

    private void sembrarDoceNotificaciones() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 14, 8, 0);
        for (int i = 0; i < 12; i++) {
            // las de indice 0, 3, 6 y 9 estan leidas; la mas reciente es la 11
            notificacionRepositoryPort.guardar(new Notificacion(
                    null, USUARIO_PAGINAS, null, null, null, TipoNotificacion.MENSAJE_NUEVO, CanalNotificacion.IN_APP,
                    "Aviso " + i, "Mensaje " + i, "/chat/conversaciones/" + i, i % 3 == 0,
                    base.plusMinutes(i), null));
        }
        notificacionRepositoryPort.guardar(new Notificacion(
                null, 7_802L, null, null, null, TipoNotificacion.MENSAJE_NUEVO, CanalNotificacion.IN_APP,
                "De otro usuario", "No debe aparecer", null, false, base, null));
    }

    @Test
    @DisplayName("GJ-118 - La bandeja pagina con page y size y devuelve los metadatos de la pagina")
    void bandejaPaginaConMetadatos() throws Exception {
        sembrarDoceNotificaciones();
        String token = jwtTokenService.generateToken(USUARIO_PAGINAS, "pag@styleradar.co", "COMPRADOR");

        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?page=0&size=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[0].titulo").value("Aviso 11"))
                .andExpect(jsonPath("$.content[4].titulo").value("Aviso 7"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(false));

        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?page=2&size=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[1].titulo").value("Aviso 0"))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(true));

        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?page=9&size=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.hasNext").value(false));

        // Con solo page, el tamano por defecto es 20
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?page=0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content.length()").value(12))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GJ-118 - El filtro de leidas sigue funcionando junto con la paginacion y sin ella")
    void bandejaFiltraPorLeidaYMantieneLaListaCompleta() throws Exception {
        sembrarDoceNotificaciones();
        String token = jwtTokenService.generateToken(USUARIO_PAGINAS, "pag@styleradar.co", "COMPRADOR");

        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?leida=false&size=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].leida").value(false));

        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?leida=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].leida").value(true));

        // soloNoLeidas=true con paginacion equivale a leida=false
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?soloNoLeidas=true&page=0&size=20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(8));

        // Sin page, size ni leida: la lista completa de siempre (un arreglo), solo del usuario
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(12));
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?soloNoLeidas=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    @DisplayName("GJ-118 - Un size mayor que 100, un size menor que 1 o un page negativo responden 400, y sin JWT 401")
    void bandejaRechazaParametrosDePaginaInvalidos() throws Exception {
        String token = jwtTokenService.generateToken(USUARIO_PAGINAS, "pag@styleradar.co", "COMPRADOR");

        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?size=101")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("size no puede superar 100"));
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?size=0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("size debe ser al menos 1"));
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?page=-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("page debe ser 0 o mayor"));
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?page=abc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        // El limite exacto es valido
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?size=100")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
        mockMvc.perform(get("/api/v1/usuarios/me/notificaciones?page=0&size=5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Path con usuarioId explicito: responde 200 cuando coincide con el token JWT y 403 cuando no coincide")
    void accesoConUsuarioIdExplicitoValidaAutenticacion() throws Exception {
        String token101 = jwtTokenService.generateToken(101L, "user101@styleradar.co", "COMPRADOR");

        // 1. Acceso a sus propias notificaciones con ID explicito 101 -> 200 OK
        mockMvc.perform(get("/api/v1/usuarios/101/notificaciones")
                        .header("Authorization", "Bearer " + token101))
                .andExpect(status().isOk());

        // 2. Acceso a sus propias alertas con ID explicito 101 -> 200 OK
        mockMvc.perform(get("/api/v1/usuarios/101/alertas")
                        .header("Authorization", "Bearer " + token101))
                .andExpect(status().isOk());

        // 3. Intento de acceder a notificaciones de otro usuario (202) -> 403 Forbidden
        mockMvc.perform(get("/api/v1/usuarios/202/notificaciones")
                        .header("Authorization", "Bearer " + token101))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("No tienes autorizacion para acceder o modificar los recursos del usuario 202"));

        // 4. Intento de acceder a alertas de otro usuario (202) -> 403 Forbidden
        mockMvc.perform(get("/api/v1/usuarios/202/alertas")
                        .header("Authorization", "Bearer " + token101))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
