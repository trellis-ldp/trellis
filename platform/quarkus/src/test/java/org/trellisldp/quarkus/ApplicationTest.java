/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.quarkus;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import java.util.List;

import org.junit.jupiter.api.Test;

@QuarkusTest
class ApplicationTest {

    @Test
    void healthCheckTest() {
        given().when().get("/health").then().assertThat()
            .contentType(ContentType.JSON)
            .statusCode(200);
    }

    @Test
    void readinessCheckTest() {
        given().when().get("/health/ready").then().assertThat()
            .contentType(ContentType.JSON)
            .statusCode(200);
    }

    @Test
    void livenessCheckTest() {
        given().when().get("/health/live").then().assertThat()
            .contentType(ContentType.JSON)
            .statusCode(200);
    }

    @Test
    void rootResourceContentType() {
        given().when().get("/").then().assertThat()
            .contentType("text/turtle")
            .statusCode(200);
    }

    @Test
    void rootResourceLinkHeaders() {
        final List<String> links = get("/").getHeaders().getValues("Link");
        assertTrue(links.contains("<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\""));
        assertTrue(links.contains("<http://www.w3.org/ns/ldp#Resource>; rel=\"type\""));
    }

    @Test
    void rootResourceBody() {
        final String body = get("/").getBody().asString();
        assertTrue(body.contains("ldp:BasicContainer"));
    }
}
