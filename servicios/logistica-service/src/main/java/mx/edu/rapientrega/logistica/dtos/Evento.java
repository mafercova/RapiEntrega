package mx.edu.rapientrega.logistica.dtos;

import java.time.Instant;
import java.util.Map;

public record Evento(
    String eventId,
    String eventType,
    int version,
    Instant occurredAt,
    String aggregateId,
    Map<String, Object> payload) {}
