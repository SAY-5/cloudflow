package dev.cloudflow.collector;

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
class CollectorControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  void ingestsLogsThenReturnsThemInRecentLogs() throws Exception {
    String body =
        "{\"lines\":[\"{\\\"ts\\\":\\\"2026-05-26T14:00:00Z\\\",\\\"service\\\":\\\"orders\\\","
            + "\\\"level\\\":\\\"ERROR\\\",\\\"trace_id\\\":\\\"t\\\",\\\"msg\\\":\\\"boom\\\","
            + "\\\"fields\\\":{}}\"]}";

    mvc.perform(post("/v1/ingest/logs").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.stored").value(1));

    mvc.perform(get("/v1/ingest/logs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].service").value("orders"))
        .andExpect(jsonPath("$[0].level").value("ERROR"));
  }
}
