package dev.cloudflow.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  void upsertsAndAdjustsStock() throws Exception {
    mvc.perform(
            put("/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"BOLT-1\",\"available\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(10));

    mvc.perform(
            post("/stock/BOLT-1/adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"delta\":-4}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(6));
  }

  @Test
  void blocksNegativeStock() throws Exception {
    mvc.perform(
        put("/stock")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sku\":\"BOLT-2\",\"available\":1}"));

    mvc.perform(
            post("/stock/BOLT-2/adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"delta\":-5}"))
        .andExpect(status().isConflict());
  }

  @Test
  void missingSkuReturns404() throws Exception {
    mvc.perform(get("/stock/NOPE")).andExpect(status().isNotFound());
  }
}
