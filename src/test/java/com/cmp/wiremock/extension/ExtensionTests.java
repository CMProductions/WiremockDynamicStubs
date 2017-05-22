package com.cmp.wiremock.extension;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
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
    public void basicTest() {
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
    public void postServeActionTest() {
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

        wiremock.stop();
    }

    @Test
    public void responseDefinitionTransformerTest() {
        WireMockServer wiremock = new WireMockServer(wireMockConfig()
                .extensions(new DynamicStubs())
                .port(8886));
        wiremock.start();

        wiremock.stubFor(get(urlEqualTo("/fake/endpoint"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>RESPONSE WITH TRANSFORMER</response>")
                        .withTransformers("DynamicStubs")));

        ValidatableResponse response = given()
                .spec(new RequestSpecBuilder().build())
                .when()
                .get("http://localhost:8886" + "/fake/endpoint")
                .then();

        System.out.println("RESPONSE: " + response.extract().body().asString());

        wiremock.stop();
    }

    @Test
    public void responseDefinitionTransformerFromJsonMappingTest() {
        WireMockServer wiremock = new WireMockServer(wireMockConfig()
                .extensions(new DynamicStubs())
                .port(8886));
        wiremock.start();
        wiremock.loadMappingsUsing(new JsonFileMappingsSource(new SingleRootFileSource("src/test/resources/mappings")));

        ValidatableResponse response = given()
                .spec(new RequestSpecBuilder().build())
                .when()
                .get("http://localhost:8886" + "/fake/transform?name=Arturo&other=3")
                .then();

        System.out.println("RESPONSE: " + response.extract().body().asString());

        wiremock.stop();
    }

    @Test
    public void postServeActionTestStandAlone() {
        //Wiremock should be running already in localhost:8887

        ValidatableResponse response = given()
                .spec(new RequestSpecBuilder().build())
                .when()
                .get("http://localhost:8887" + "/fake/postAction")
                .then();

        System.out.println("RESPONSE: " + response.extract().body().asString());
    }

    @Test
    public void responseDefinitionTransformerTestStandAlone() {
        // Wiremock should be running already in localhost:8887
        // you can run it executing the following command in your Wiremock directory, with the proper .jar files:
        // java -cp "wiremockDynamicStubs-0.1.0.jar:wiremock-standalone-2.5.1.jar" com.github.tomakehurst.wiremock.standalone.WireMockServerRunner --verbose --extensions com.cmp.wiremock.extension.Postback,com.cmp.wiremock.extension.DynamicStubs --port 8887

        ValidatableResponse response = given()
                .spec(new RequestSpecBuilder().build())
                .when()
                .get("http://localhost:8887" + "/fake/transform")
                .then();

        System.out.println("RESPONSE: " + response.extract().body().asString());
    }
}
