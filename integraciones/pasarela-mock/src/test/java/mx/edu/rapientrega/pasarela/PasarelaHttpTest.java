package mx.edu.rapientrega.pasarela;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PasarelaController.class)
class PasarelaHttpTest {
  @Autowired MockMvc mvc;

  @Test
  void falloTransitorioYReintento() throws Exception {
    String s = "{\"pedidoId\":\"p1\",\"importe\":10.00,\"tokenTarjeta\":\"ok\"}";
    mvc.perform(
            post("/mock/cobros")
                .header("Idempotency-Key", "transitorio")
                .header("X-Mock-Failures", 1)
                .contentType("application/json")
                .content(s))
        .andExpect(status().isServiceUnavailable());
    mvc.perform(
            post("/mock/cobros")
                .header("Idempotency-Key", "transitorio")
                .header("X-Mock-Failures", 1)
                .contentType("application/json")
                .content(s))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("AUTORIZADO"));
  }

  @Test
  void respuestaPerdidaSePuedeConsultar() throws Exception {
    mvc.perform(
            post("/mock/cobros")
                .header("Idempotency-Key", "perdida")
                .contentType("application/json")
                .content(
                    "{\"pedidoId\":\"p2\",\"importe\":10.00,\"tokenTarjeta\":\"timeout-once\"}"))
        .andExpect(status().isGatewayTimeout());
    mvc.perform(get("/mock/operaciones/perdida"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("AUTORIZADO"));
  }

  @Test
  void tarjetaRechazada() throws Exception {
    mvc.perform(
            post("/mock/cobros")
                .header("Idempotency-Key", "reject")
                .contentType("application/json")
                .content("{\"pedidoId\":\"p3\",\"importe\":10.00,\"tokenTarjeta\":\"reject\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("RECHAZADO"));
  }
}
