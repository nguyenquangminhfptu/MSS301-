package fu.de190270.post;

import com.sun.net.httpserver.HttpServer;
import fu.de190270.post.repository.PostRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostApiTests {
    @Autowired MockMvc mvc;
    @Autowired PostRepository repository;
    @Autowired ObjectMapper mapper;
    private static volatile String userStatus = "ACTIVE";
    private static volatile int rate = 500000;
    private static final HttpServer USER_SERVER = startUserServer();
    private static final String POST = """
            {"advertiserName":"Shop ABC","advertiserEmail":"ads@example.com","advertiserPhone":"0900000000",
             "startDate":"25/07/2026","endDate":"28/07/2026","numberOfPosts":2,"userId":1}
            """;

    private static HttpServer startUserServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/users/", exchange -> {
                String id = exchange.getRequestURI().getPath().substring("/api/users/".length());
                int code = id.equals("999") ? 404 : id.equals("503") ? 503 : 200;
                String body = code == 200 ? """
                        {"status":1,"message":"Successful","data":{"userId":%s,"username":"creator_a",
                        "email":"a@example.com","role":"CREATOR","ratePerPost":%d,"maxDailyPosts":3,
                        "level":1,"status":"%s"},"timestamp":"2026-07-24T02:21:45Z"}
                        """.formatted(id, rate, userStatus) : "{\"status\":4,\"message\":\"Not found\",\"data\":null}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(code, bytes.length);
                try (var output = exchange.getResponseBody()) { output.write(bytes); }
            });
            server.start();
            return server;
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("user.service.url", () -> "http://127.0.0.1:" + USER_SERVER.getAddress().getPort());
    }

    @AfterAll
    static void stopServer() { USER_SERVER.stop(0); }

    @BeforeEach
    void reset() { repository.deleteAll(); userStatus = "ACTIVE"; rate = 500000; }

    private long createPost() throws Exception {
        String json = mvc.perform(post("/api/posts").contentType("application/json").content(POST))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.totalCost").value(1500000))
                .andExpect(jsonPath("$.data.startDate").value("25/07/2026"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("data").get("postId").asLong();
    }

    @Test
    void createDetailFilterRepriceAndCancel() throws Exception {
        long id = createPost();
        mvc.perform(get("/api/posts/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("creator_a"))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.endDate").value("28/07/2026"));
        mvc.perform(get("/api/posts?advertiserName=abc&status=SCHEDULED&size=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].user.userId").value(1));
        rate = 600000;
        mvc.perform(put("/api/posts/" + id).contentType("application/json")
                .content("{\"endDate\":\"29/07/2026\",\"totalCost\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCost").value(2400000))
                .andExpect(jsonPath("$.data.advertiserName").value("Shop ABC"));
        mvc.perform(delete("/api/posts/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(get("/api/posts/" + id)).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        assertEquals(1, repository.count());
    }

    @Test
    void feign404Is404AndOtherUpstreamFailureIs500() throws Exception {
        mvc.perform(post("/api/posts").contentType("application/json").content(POST.replace("\"userId\":1", "\"userId\":999")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(4));
        mvc.perform(post("/api/posts").contentType("application/json").content(POST.replace("\"userId\":1", "\"userId\":503")))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(0));
        assertEquals(0, repository.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUSPENDED", "DEACTIVATED"})
    void inactiveUserIs400(String state) throws Exception {
        userStatus = state;
        mvc.perform(post("/api/posts").contentType("application/json").content(POST))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(5));
        assertEquals(0, repository.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"28/07/2026", "29/07/2026"})
    void invalidMergedDatesDoNotPersist(String start) throws Exception {
        long id = createPost();
        mvc.perform(put("/api/posts/" + id).contentType("application/json")
                .content("{\"startDate\":\"" + start + "\",\"advertiserName\":\"Changed\"}"))
                .andExpect(status().is(406)).andExpect(jsonPath("$.status").value(2));
        mvc.perform(get("/api/posts/" + id)).andExpect(jsonPath("$.data.startDate").value("25/07/2026"))
                .andExpect(jsonPath("$.data.advertiserName").value("Shop ABC"));
    }

    @Test
    void dailyLimitIsEnforcedOnCreateAndPartialUpdate() throws Exception {
        mvc.perform(post("/api/posts").contentType("application/json").content(POST.replace("\"numberOfPosts\":2", "\"numberOfPosts\":4")))
                .andExpect(status().is(406)).andExpect(jsonPath("$.status").value(2));
        long id = createPost();
        mvc.perform(put("/api/posts/" + id).contentType("application/json").content("{\"numberOfPosts\":4}"))
                .andExpect(status().is(406)).andExpect(jsonPath("$.status").value(2));
        assertEquals(2, repository.findById(id).orElseThrow().getNumberOfPosts());
    }

    @ParameterizedTest
    @ValueSource(strings = {"25/07/2026", "24/07/2026", "31/02/2026", "2026-07-28", "nonsense"})
    void invalidDatesAre406(String date) throws Exception {
        mvc.perform(post("/api/posts").contentType("application/json").content(POST.replace("28/07/2026", date)))
                .andExpect(status().is(406)).andExpect(jsonPath("$.status").value(2));
        assertEquals(0, repository.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"size=101", "size=0", "page=-1", "status=BAD"})
    void invalidQueryIs406(String query) throws Exception {
        mvc.perform(get("/api/posts?" + query)).andExpect(status().is(406)).andExpect(jsonPath("$.status").value(2));
    }

    @Test
    void malformedAndMissingBodiesAre406() throws Exception {
        for (String body : new String[]{"{}", "{", POST.replace("ads@example.com", "bad"),
                POST.replace("\"numberOfPosts\":2", "\"numberOfPosts\":1.5")}) {
            mvc.perform(post("/api/posts").contentType("application/json").content(body))
                    .andExpect(status().is(406)).andExpect(jsonPath("$.status").value(2));
        }
    }

    @Test
    void missingPostIs404() throws Exception {
        mvc.perform(get("/api/posts/999999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(4));
        mvc.perform(put("/api/posts/999999").contentType("application/json").content("{}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(4));
        mvc.perform(delete("/api/posts/999999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(4));
    }

    @Test
    void swaggerSupportsCrossOrigin() throws Exception {
        mvc.perform(get("/v3/api-docs").header("Origin", "http://localhost:8080"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"))
                .andExpect(jsonPath("$.paths['/api/posts']").exists());
    }
}
