package fu.de190270.user;

import fu.de190270.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserApiTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository repository;
    @Autowired ObjectMapper mapper;
    private static final String USER = """
            {"username":"creator_test","email":"test@example.com","role":"CREATOR",
             "ratePerPost":500000,"maxDailyPosts":3,"level":1}
            """;

    @BeforeEach
    void reset() { repository.deleteAll(); }

    private long createUser() throws Exception {
        String json = mvc.perform(post("/api/users").contentType("application/json").content(USER))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.timestamp").value(org.hamcrest.Matchers.matchesPattern(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("data").get("userId").asLong();
    }

    @Test
    void createUpdateDeactivateAndFilter() throws Exception {
        long id = createUser();
        mvc.perform(put("/api/users/" + id).contentType("application/json").content("{\"level\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.level").value(2))
                .andExpect(jsonPath("$.data.username").value("creator_test"));
        mvc.perform(get("/api/users?role=CREATOR&status=ACTIVE&page=0&size=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].userId").value(id))
                .andExpect(jsonPath("$.data.first").value(true)).andExpect(jsonPath("$.data.last").value(true));
        mvc.perform(delete("/api/users/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(get("/api/users/" + id)).andExpect(jsonPath("$.data.status").value("DEACTIVATED"));
        mvc.perform(get("/api/users?status=ACTIVE")).andExpect(jsonPath("$.data.totalElements").value(0));
        assertEquals(1, repository.count());
    }

    @Test
    void duplicateAndUpdateToDuplicateReturn226() throws Exception {
        long id = createUser();
        mvc.perform(post("/api/users").contentType("application/json").content(USER))
                .andExpect(status().is(226)).andExpect(jsonPath("$.status").value(3));
        mvc.perform(put("/api/users/" + id).contentType("application/json").content("{\"username\":\"creator_test\"}"))
                .andExpect(status().isOk());
        String second = mvc.perform(post("/api/users").contentType("application/json")
                .content(USER.replace("creator_test", "creator_second")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long secondId = mapper.readTree(second).get("data").get("userId").asLong();
        mvc.perform(put("/api/users/" + secondId).contentType("application/json").content("{\"username\":\"creator_test\"}"))
                .andExpect(status().is(226)).andExpect(jsonPath("$.status").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"username\":\"   \"}", "{\"email\":\"bad\"}", "{\"role\":\"INVALID\"}",
            "{\"ratePerPost\":0}", "{\"ratePerPost\":1.001}", "{\"maxDailyPosts\":11}", "{\"level\":0}"})
    void invalidCreatesAre406(String body) throws Exception {
        if (!body.equals("{}")) {
            var merged = (tools.jackson.databind.node.ObjectNode) mapper.readTree(USER);
            merged.setAll((tools.jackson.databind.node.ObjectNode) mapper.readTree(body));
            body = mapper.writeValueAsString(merged);
        }
        mvc.perform(post("/api/users").contentType("application/json").content(body))
                .andExpect(status().is(406)).andExpect(jsonPath("$.status").value(2));
        assertEquals(0, repository.count());
    }

    @Test
    void invalidPartialUpdateDoesNotPersist() throws Exception {
        long id = createUser();
        mvc.perform(put("/api/users/" + id).contentType("application/json").content("{\"email\":\"\",\"level\":9}"))
                .andExpect(status().is(406)).andExpect(jsonPath("$.status").value(2));
        mvc.perform(get("/api/users/" + id)).andExpect(jsonPath("$.data.level").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"size=101", "size=0", "page=-1", "page=abc", "role=BAD", "status=BAD"})
    void invalidQueryParametersAre406(String query) throws Exception {
        mvc.perform(get("/api/users?" + query)).andExpect(status().is(406)).andExpect(jsonPath("$.status").value(2));
    }

    @Test
    void missingUserReturns404ForAllOperations() throws Exception {
        mvc.perform(get("/api/users/999999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(4));
        mvc.perform(put("/api/users/999999").contentType("application/json").content("{}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(4));
        mvc.perform(delete("/api/users/999999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(4));
    }

    @Test
    void swaggerAndCorsAreEnabled() throws Exception {
        mvc.perform(get("/v3/api-docs").header("Origin", "http://localhost:8080"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"))
                .andExpect(jsonPath("$.paths['/api/users']").exists());
        mvc.perform(options("/api/users").header("Origin", "https://example.com")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk()).andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}
