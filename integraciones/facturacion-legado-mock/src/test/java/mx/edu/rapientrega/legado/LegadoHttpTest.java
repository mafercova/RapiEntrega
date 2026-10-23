package mx.edu.rapientrega.legado;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LegadoController.class)
class LegadoHttpTest {
  @Autowired MockMvc mvc;

  @Test
  void legadoReintentaSinDuplicarFolio() throws Exception {
    mvc.perform(
            post("/legacy2012/comprobantes")
                .header("Idempotency-Key", "folio1")
                .header("X-Mock-Failures", 1)
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isServiceUnavailable());
    var a =
        mvc.perform(
                post("/legacy2012/comprobantes")
                    .header("Idempotency-Key", "folio1")
                    .contentType("application/json")
                    .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.SIMULACION").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
    mvc.perform(
            post("/legacy2012/comprobantes")
                .header("Idempotency-Key", "folio1")
                .contentType("application/json")
                .content("{}"))
        .andExpect(content().json(a));
  }
}
