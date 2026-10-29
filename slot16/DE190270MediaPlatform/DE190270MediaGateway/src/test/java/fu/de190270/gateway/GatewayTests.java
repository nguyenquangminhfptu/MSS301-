package fu.de190270.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayTests {
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newHttpClient();
    private static final HttpServer UPSTREAM = upstream();

    private static HttpServer upstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/", exchange -> {
                int status = exchange.getRequestURI().getPath().endsWith("duplicate") ? 226 : 200;
                String body = "{\"status\":" + (status == 226 ? 3 : 1) + ",\"path\":\""
                        + exchange.getRequestURI() + "\",\"method\":\"" + exchange.getRequestMethod() + "\"}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "https://example.com");
                exchange.sendResponseHeaders(status, bytes.length);
                try (var output = exchange.getResponseBody()) { output.write(bytes); }
            });
            server.start();
            return server;
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("user.service.url", () -> "http://127.0.0.1:" + UPSTREAM.getAddress().getPort());
        registry.add("post.service.url", () -> "http://127.0.0.1:" + UPSTREAM.getAddress().getPort());
    }

    @AfterAll
    static void stop() { UPSTREAM.stop(0); }

    private HttpResponse<String> send(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void forwardsBasePathsNestedPathsAndQueryStrings() throws Exception {
        for (String path : new String[]{"/api/users", "/api/users/1", "/api/posts", "/api/posts/1?size=2"}) {
            HttpResponse<String> response = send(path);
            assertEquals(200, response.statusCode(), response.body());
            assertTrue(response.body().contains(path), response.body());
        }
    }

    @Test
    void preserves226AndJsonBody() throws Exception {
        HttpResponse<String> response = send("/api/users/duplicate");
        assertEquals(226, response.statusCode());
        assertTrue(response.body().contains("\"status\":3"));
    }

    @Test
    void allowsCorsPreflightAndDeduplicatesOrigin() throws Exception {
        var uri = URI.create("http://localhost:" + port + "/api/posts");
        var preflight = client.send(HttpRequest.newBuilder(uri).method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "https://example.com").header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type").build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, preflight.statusCode());
        assertEquals("https://example.com", preflight.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        var response = client.send(HttpRequest.newBuilder(uri).header("Origin", "https://example.com").GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(1, response.headers().allValues("Access-Control-Allow-Origin").size());
    }

    @Test
    void exposesOpenApiAndBothSwaggerDefinitions() throws Exception {
        assertEquals(200, send("/v3/api-docs").statusCode());
        var config = send("/v3/api-docs/swagger-config");
        assertEquals(200, config.statusCode());
        assertTrue(config.body().contains("http://localhost:8081/v3/api-docs"));
        assertTrue(config.body().contains("http://localhost:8082/v3/api-docs"));
        assertEquals(200, send("/swagger-ui/index.html").statusCode());
    }
}
