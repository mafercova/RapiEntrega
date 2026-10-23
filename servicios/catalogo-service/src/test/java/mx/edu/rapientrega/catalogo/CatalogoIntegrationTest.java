package mx.edu.rapientrega.catalogo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:catalogo;MODE=MySQL",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.sql.init.mode=always"
    })
@AutoConfigureMockMvc
class CatalogoIntegrationTest {
  @Autowired MockMvc mvc;

  @Test
  void consultarMenuConPrecioDecimal() throws Exception {
    mvc.perform(get("/api/v1/restaurantes/1/menu"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].precio").value(65.50));
  }

  @Test
  void restauranteInexistente() throws Exception {
    mvc.perform(get("/api/v1/restaurantes/999/menu")).andExpect(status().isNotFound());
  }
}
