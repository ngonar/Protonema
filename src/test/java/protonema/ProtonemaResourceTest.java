package protonema;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
public class ProtonemaResourceTest {

    @Test
    public void testStatusEndpoint() {
        given()
          .when().get("/api/iso/status")
          .then()
             .statusCode(200)
             .body("status", equalTo("UP"))
             .body("application", equalTo("Protonema Quarkus ISO8583 Server"));
    }

    @Test
    public void testSimulateEndpoint() {
        Map<String, String> payload = new HashMap<>();
        payload.put("mti", "0100");
        payload.put("stan", "000002");
        payload.put("pan", "4567890123456789");

        given()
          .contentType(ContentType.JSON)
          .body(payload)
          .when().post("/api/iso/simulate")
          .then()
             .statusCode(200)
             .body("status", equalTo("QUEUED"))
             .body("mti", equalTo("0100"));
    }
}
