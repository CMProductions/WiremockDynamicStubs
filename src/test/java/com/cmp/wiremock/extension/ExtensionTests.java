package com.cmp.wiremock.extension;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.response.ValidatableResponse;
import net.minidev.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.jayway.restassured.RestAssured.given;

/**
 * Created by lunabulnes
 */
public class ExtensionTests {

    private static final String BASE_URL = "http://localhost:8888";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8888);

    @Test
    public void basicWiremockTest() {
        stubFor(get(urlEqualTo("/fake/endpoint"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>THIS IS A RESPONSE</response>")));

        ValidatableResponse response = given()
                .spec(new RequestSpecBuilder().build())
                .when()
                .get(BASE_URL + "/fake/endpoint")
                .then();

        System.out.println("RESPONSE: " + response.extract().body().asString());
    }

    @Test
    public void postServeActionWiremockTest() {
        WireMockServer wiremock = new WireMockServer(wireMockConfig()
                .extensions(new Postback())
                .port(8886));
        wiremock.start();

        wiremock.stubFor(get(urlEqualTo("/fake/endpoint"))
                .withPostServeAction("Postback", new JSONObject())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>RESPONSE WITH POST SERVE ACTION</response>")));

        ValidatableResponse response = given()
                .spec(new RequestSpecBuilder().build())
                .when()
                .get("http://localhost:8886" + "/fake/endpoint")
                .then();

        System.out.println("RESPONSE: " + response.extract().body().asString());
    }
}
