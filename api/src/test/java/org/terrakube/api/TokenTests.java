package org.terrakube.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;

class TokenTests extends ServerApplicationTests {

    @Test
    void createToken() throws JsonProcessingException {
        String token = given()
                .headers("Authorization", "Bearer " + generatePAT("TERRAKUBE_DEVELOPERS"))
                .headers("Content-Type", "application/vnd.api+json")
                .when()
                .body("{\"description\":\"12345\",\"days\":1}")
                .post("/pat/v1")
                .then()
                .assertThat()
                .log()
                .all()
                .statusCode(HttpStatus.CREATED.value()).extract().path("token");

        String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getDecoder();
        String payload = new String(decoder.decode(chunks[1]));
        Map<String, Object> result = new ObjectMapper().readValue(payload, HashMap.class);
        Assert.assertNotNull(result.get("exp"));

        given()
                .headers("Authorization", "Bearer " + token).when()
                .get("/api/v1/organization/d9b58bd3-f3fc-4056-a026-1163297e80a8")
                .then()
                .log()
                .all()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void createTokenNoExpiration() throws JsonProcessingException {
        String token = given()
                .headers("Authorization", "Bearer " + generatePAT("TERRAKUBE_DEVELOPERS"))
                .headers("Content-Type", "application/vnd.api+json")
                .when()
                .body("{\"description\":\"12345\",\"days\":0}")
                .post("/pat/v1")
                .then()
                .assertThat()
                .log()
                .all()
                .statusCode(HttpStatus.CREATED.value()).extract().path("token");

        String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getDecoder();
        String payload = new String(decoder.decode(chunks[1]));
        Map<String, Object> result = new ObjectMapper().readValue(payload, HashMap.class);
        Assert.assertNull(result.get("exp"));

        given()
                .headers("Authorization", "Bearer " + token).when()
                .get("/api/v1/organization/d9b58bd3-f3fc-4056-a026-1163297e80a8")
                .then()
                .log()
                .all()
                .statusCode(HttpStatus.OK.value());

    }

}
