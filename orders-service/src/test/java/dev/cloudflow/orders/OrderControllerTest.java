package dev.cloudflow.orders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class OrderControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  void createsAndReadsBackAnOrder() throws Exception {
    String id =
        mvc.perform(
                post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"sku\":\"WIDGET-1\",\"quantity\":3}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.sku").value("WIDGET-1"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String orderId = id.replaceAll(".*\"id\":(\\d+).*", "$1");
    mvc.perform(get("/orders/" + orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantity").value(3));
  }

  @Test
  void rejectsInvalidQuantity() throws Exception {
    mvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"WIDGET-1\",\"quantity\":0}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void missingOrderReturns404() throws Exception {
    mvc.perform(get("/orders/999999")).andExpect(status().isNotFound());
  }

  @Test
  void failingAnOrderEmitsErrorPath() throws Exception {
    String body =
        mvc.perform(
                post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"sku\":\"WIDGET-2\",\"quantity\":1}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String orderId = body.replaceAll(".*\"id\":(\\d+).*", "$1");
    mvc.perform(post("/orders/" + orderId + "/fail"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FAILED"));
  }
}
