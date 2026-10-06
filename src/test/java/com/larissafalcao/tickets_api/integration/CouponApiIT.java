package com.larissafalcao.tickets_api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.larissafalcao.tickets_api.application.port.TimeProvider;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(CouponApiIT.TimeConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class CouponApiIT {
    private static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");
    private static final AtomicReference<Instant> CLOCK = new AtomicReference<>(NOW);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate database;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final JsonMapper json = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();

    @BeforeEach
    void reset() {
        database.execute("TRUNCATE TABLE coupons");
        CLOCK.set(NOW);
    }

    @Test
    void createsAndRetrievesCouponWithNormalizedCodeAndExpectedResponse() throws Exception {
        ObjectNode body = validRequest().put("published", true);
        HttpResponse<String> created = call("POST", "/coupon", body);
        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode coupon = json.readTree(created.body());
        UUID id = UUID.fromString(coupon.path("id").asString());
        assertThat(created.headers().firstValue("Location")).contains("/coupon/" + id);
        assertThat(coupon.properties()).hasSize(8);
        assertThat(coupon.path("code").asString()).isEqualTo("ABC123");
        assertThat(coupon.path("description").asString()).isEqualTo("Original description");
        assertThat(coupon.path("discountValue").asDecimal()).isEqualByComparingTo("0.8");
        assertThat(Instant.parse(coupon.path("expirationDate").asString())).isEqualTo(NOW.plusSeconds(60));
        assertThat(coupon.path("status").asString()).isEqualTo("ACTIVE");
        assertThat(coupon.path("published").asBoolean()).isTrue();
        assertThat(coupon.path("redeemed").asBoolean()).isFalse();
        HttpResponse<String> retrieved = call("GET", "/coupon/" + id, null);
        assertThat(retrieved.statusCode()).isEqualTo(200);
        assertThat(json.readTree(retrieved.body())).isEqualTo(coupon);
        assertThat(database.queryForObject("SELECT code FROM coupons WHERE id = ?", String.class, id)).isEqualTo("ABC123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"omitted", "null", "false", "true"})
    void handlesOptionalPublishedFlag(String value) throws Exception {
        ObjectNode body = validRequest();
        switch (value) {
            case "null" -> body.putNull("published");
            case "false", "true" -> body.put("published", Boolean.parseBoolean(value));
            default -> { /* "omitted": the request goes without the field */ }
        }
        HttpResponse<String> response = call("POST", "/coupon", body);
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(json.readTree(response.body()).path("published").asBoolean()).isEqualTo(value.equals("true"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"code", "description", "discountValue", "expirationDate"})
    void rejectsMissingAndNullRequiredFieldsWithoutPersisting(String field) throws Exception {
        ObjectNode missing = validRequest();
        missing.remove(field);
        assertBadRequestWithoutRows(call("POST", "/coupon", missing));
        ObjectNode explicitNull = validRequest().putNull(field);
        assertBadRequestWithoutRows(call("POST", "/coupon", explicitNull));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void rejectsInvalidValuesWithoutPersisting(String field, String value) throws Exception {
        ObjectNode body = validRequest();
        body.set(field, json.readTree(value));
        assertBadRequestWithoutRows(call("POST", "/coupon", body));
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("code", "\"\""), Arguments.of("code", "\"!!!!!!\""),
                Arguments.of("code", "\"ABCDE\""), Arguments.of("code", "\"ABC-1234\""),
                Arguments.of("description", "\"\""), Arguments.of("description", "\"  \""),
                Arguments.of("discountValue", "0.499999999999999999"), Arguments.of("discountValue", "-5"),
                Arguments.of("discountValue", "0"), Arguments.of("discountValue", "\"not-a-number\""),
                Arguments.of("expirationDate", "\"2026-10-04T11:59:59Z\""),
                Arguments.of("expirationDate", "\"not-a-date\""),
                Arguments.of("expirationDate", "\"2026-10-04T12:00:00\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "1.12345678901234567890123456789",
            "999999999999999999999999999999999999999999999999999999999999.123456789"})
    void persistsDiscountWithoutUpperLimitOrRounding(String value) throws Exception {
        HttpResponse<String> response = call("POST", "/coupon", validRequest().put("discountValue", new BigDecimal(value)));
        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode coupon = json.readTree(response.body());
        UUID id = UUID.fromString(coupon.path("id").asString());
        assertThat(coupon.path("discountValue").asDecimal()).isEqualByComparingTo(value);
        assertThat(database.queryForObject("SELECT discount_value FROM coupons WHERE id = ?", BigDecimal.class, id))
                .isEqualByComparingTo(value);
        assertThat(json.readTree(call("GET", "/coupon/" + id, null).body()).path("discountValue").asDecimal())
                .isEqualByComparingTo(value);
    }

    @Test
    void acceptsExpirationExactlyNowAndNormalizesTimezone() throws Exception {
        HttpResponse<String> response = call("POST", "/coupon", validRequest()
                .put("expirationDate", "2026-10-04T09:00:00-03:00"));
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(Instant.parse(json.readTree(response.body()).path("expirationDate").asString())).isEqualTo(NOW);
    }

    @Test
    void permitsRepeatedCodesAndPreservesCase() throws Exception {
        String first = createdId(validRequest());
        String second = createdId(validRequest().put("code", "ABC123"));
        String lower = createdId(validRequest().put("code", "abc-123"));
        assertThat(first).isNotEqualTo(second);
        assertThat(count()).isEqualTo(3);
        assertThat(json.readTree(call("GET", "/coupon/" + lower, null).body()).path("code").asString()).isEqualTo("abc123");
    }

    @Test
    void ignoresAttemptsToSetInternalState() throws Exception {
        String forgedId = UUID.randomUUID().toString();
        ObjectNode body = validRequest().put("id", forgedId).put("status", "DELETED")
                .put("redeemed", true).put("deletedAt", NOW.toString());

        HttpResponse<String> response = call("POST", "/coupon", body);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode coupon = json.readTree(response.body());
        assertThat(coupon.path("id").asString()).isNotEqualTo(forgedId);
        assertThat(coupon.path("status").asString()).isEqualTo("ACTIVE");
        assertThat(coupon.path("redeemed").asBoolean()).isFalse();
        assertThat(database.queryForMap("SELECT deleted_at FROM coupons").get("deleted_at")).isNull();
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/coupon")).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{broken-json")).build();
        assertBadRequestWithoutRows(http.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "[]"})
    void rejectsMissingOrNonObjectRequestBody(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/coupon")).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        assertBadRequestWithoutRows(http.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void deletingOneOfRepeatedCodesDoesNotAffectTheOtherCoupon() throws Exception {
        String first = createdId(validRequest());
        String second = createdId(validRequest());
        JsonNode secondBefore = json.readTree(call("GET", "/coupon/" + second, null).body());

        int firstDeletion = call("DELETE", "/coupon/" + first, null).statusCode();
        int repeatedDeletion = call("DELETE", "/coupon/" + first, null).statusCode();

        assertThat(firstDeletion).isEqualTo(204);
        assertThat(repeatedDeletion).isEqualTo(409);
        HttpResponse<String> secondAfter = call("GET", "/coupon/" + second, null);
        assertThat(secondAfter.statusCode()).isEqualTo(200);
        assertThat(json.readTree(secondAfter.body())).isEqualTo(secondBefore);
        assertThat(call("DELETE", "/coupon/" + second, null).statusCode()).isEqualTo(204);
        assertThat(count()).isEqualTo(2);
        assertThat(database.queryForObject("SELECT COUNT(*) FROM coupons WHERE status = 'DELETED'", Long.class))
                .isEqualTo(2);
    }

    @Test
    void canCreateTheSameCodeAfterDeletionWithoutRestoringTheDeletedCoupon() throws Exception {
        String deletedId = createdId(validRequest());
        assertThat(call("DELETE", "/coupon/" + deletedId, null).statusCode()).isEqualTo(204);
        var deletedBefore = database.queryForMap("SELECT * FROM coupons WHERE id = ?", UUID.fromString(deletedId));

        String newId = createdId(validRequest());

        assertThat(newId).isNotEqualTo(deletedId);
        assertThat(call("GET", "/coupon/" + newId, null).statusCode()).isEqualTo(200);
        assertThat(call("GET", "/coupon/" + deletedId, null).statusCode()).isEqualTo(404);
        assertThat(database.queryForMap("SELECT * FROM coupons WHERE id = ?", UUID.fromString(deletedId)))
                .isEqualTo(deletedBefore);
        assertThat(count()).isEqualTo(2);
    }

    @Test
    void deletingAnUnknownIdDoesNotChangeExistingCoupons() throws Exception {
        UUID existing = UUID.fromString(createdId(validRequest()));
        var before = database.queryForMap("SELECT * FROM coupons WHERE id = ?", existing);

        HttpResponse<String> response = call("DELETE", "/coupon/" + UUID.randomUUID(), null);

        assertThat(response.statusCode()).isEqualTo(404);
        assertProblem(response, 404);
        assertThat(database.queryForMap("SELECT * FROM coupons WHERE id = ?", existing)).isEqualTo(before);
        assertThat(call("GET", "/coupon/" + existing, null).statusCode()).isEqualTo(200);
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void deletesLogicallyPreservingRegistrationAndRejectsSecondDeletion() throws Exception {
        String id = createdId(validRequest().put("published", true));
        UUID uuid = UUID.fromString(id);
        var before = database.queryForMap("SELECT * FROM coupons WHERE id = ?", uuid);
        HttpResponse<String> deleted = call("DELETE", "/coupon/" + id, null);
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(deleted.body()).isEmpty();
        var after = database.queryForMap("SELECT * FROM coupons WHERE id = ?", uuid);
        for (String field : List.of("id", "code", "description", "discount_value", "expiration_date", "published", "redeemed")) {
            assertThat(after).containsEntry(field, before.get(field));
        }
        assertThat(after).containsEntry("status", "DELETED");
        assertThat(after.get("deleted_at")).isNotNull();
        assertThat(call("GET", "/coupon/" + id, null).statusCode()).isEqualTo(404);
        CLOCK.set(NOW.plusSeconds(10));
        HttpResponse<String> repeated = call("DELETE", "/coupon/" + id, null);
        assertThat(repeated.statusCode()).isEqualTo(409);
        assertProblem(repeated, 409);
        assertThat(database.queryForMap("SELECT * FROM coupons WHERE id = ?", uuid)).isEqualTo(after);
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void canRetrieveAndDeleteCouponAfterItExpires() throws Exception {
        String id = createdId(validRequest().put("published", true));
        CLOCK.set(NOW.plusSeconds(3600));
        assertThat(call("GET", "/coupon/" + id, null).statusCode()).isEqualTo(200);
        assertThat(call("DELETE", "/coupon/" + id, null).statusCode()).isEqualTo(204);
    }

    @Test
    void onlyOneConcurrentDeletionSucceeds() throws Exception {
        String id = createdId(validRequest());
        Callable<Integer> deletion = () -> call("DELETE", "/coupon/" + id, null).statusCode();
        List<Integer> statuses = new ArrayList<>();

        try (var executor = Executors.newFixedThreadPool(8)) {
            for (var result : executor.invokeAll(Collections.nCopies(8, deletion))) {
                statuses.add(result.get());
            }
        }

        assertThat(statuses).containsExactlyInAnyOrder(204, 409, 409, 409, 409, 409, 409, 409);
        assertThat(count()).isEqualTo(1);
        assertThat(database.queryForObject("SELECT status FROM coupons WHERE id = ?", String.class, UUID.fromString(id)))
                .isEqualTo("DELETED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "DELETE"})
    void handlesMissingCouponsAndMalformedUuids(String method) throws Exception {
        HttpResponse<String> missing = call(method, "/coupon/" + UUID.randomUUID(), null);
        assertThat(missing.statusCode()).isEqualTo(404);
        assertProblem(missing, 404);
        HttpResponse<String> invalid = call(method, "/coupon/not-a-uuid", null);
        assertThat(invalid.statusCode()).isEqualTo(400);
        assertProblem(invalid, 400);
    }

    @Test
    void exposesSwaggerAndDocumentedOperations() throws Exception {
        HttpResponse<String> response = call("GET", "/v3/api-docs", null);
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode spec = json.readTree(response.body());
        assertThat(spec.path("paths").path("/coupon").has("post")).isTrue();
        assertThat(spec.path("paths").path("/coupon/{id}").has("get")).isTrue();
        assertThat(spec.path("paths").path("/coupon/{id}").has("delete")).isTrue();
        assertThat(spec.path("paths").path("/coupon/{id}").path("delete").path("responses").path("204").has("content"))
                .isFalse();
        HttpResponse<String> swagger = http.send(HttpRequest.newBuilder(uri("/swagger-ui/index.html")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(swagger.statusCode()).isEqualTo(200);
        assertThat(swagger.body()).contains("Swagger UI");
    }

    @Test
    void logsEachOutcomeWithoutCouponCodeOrDescription(CapturedOutput output) throws Exception {
        ObjectNode secret = validRequest().put("code", "SEC-RET").put("description", "confidential-text");
        String id = createdId(secret);
        call("GET", "/coupon/" + id, null);
        call("DELETE", "/coupon/" + id, null);
        call("DELETE", "/coupon/" + id, null);
        call("POST", "/coupon", secret.deepCopy().put("discountValue", 0));
        HttpRequest malformed = HttpRequest.newBuilder(uri("/coupon")).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"code\":\"SEC-RET\",\"description\":\"confidential-text\",")).build();
        http.send(malformed, HttpResponse.BodyHandlers.ofString());

        assertThat(output.getOut())
                .contains("Coupon created: id=" + id, "Coupon retrieved: id=" + id, "Coupon deleted: id=" + id)
                .contains("status=409", "Coupon rejected: field=discountValue", "status=400")
                .doesNotContain("SECRET", "SEC-RET", "confidential-text");
    }

    private ObjectNode validRequest() {
        return json.createObjectNode().put("code", "ABC-123").put("description", "Original description")
                .put("discountValue", new BigDecimal("0.8")).put("expirationDate", NOW.plusSeconds(60).toString());
    }

    private String createdId(ObjectNode body) throws Exception {
        HttpResponse<String> response = call("POST", "/coupon", body);
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(201);
        return json.readTree(response.body()).path("id").asString();
    }

    private HttpResponse<String> call(String method, String path, JsonNode body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json, application/problem+json");
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private long count() {
        return database.queryForObject("SELECT COUNT(*) FROM coupons", Long.class);
    }

    private void assertBadRequestWithoutRows(HttpResponse<String> response) {
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(400);
        assertProblem(response, 400);
        assertThat(count()).isZero();
    }

    private void assertProblem(HttpResponse<String> response, int status) {
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertThat(json.readTree(response.body()).path("status").asInt()).isEqualTo(status);
        assertThat(json.readTree(response.body()).path("detail").asString()).isNotBlank();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TimeConfiguration {
        @Bean
        @Primary
        TimeProvider controlledTime() {
            return CLOCK::get;
        }
    }
}
