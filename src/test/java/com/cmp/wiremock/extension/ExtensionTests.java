package com.cmp.wiremock.extension;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.response.ValidatableResponse;
import net.minidev.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
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
                .body("<Records><Record><DataSource>Criminal Court</DataSource><OffenderId>FAKE-OFFENDER-ID-0000000001</OffenderId><Name><First>TESTGUY</First><Middle></Middle><Last>SOMETHING</Last></Name></Record></Records>")
                .when()
                .post("http://localhost:8886" + "/fake/xml/transform?name=Arturo&other=3")
                .then();

        System.out.println("RESPONSE: " + response.extract().body().asString());

        wiremock.stop();
    }

    @Test
    public void transformResponseFromSavedResponseTest() {
        WireMockServer wiremock = new WireMockServer(wireMockConfig()
                .extensions(new DynamicStubs())
                .port(8886));
        wiremock.start();
        wiremock.loadMappingsUsing(new JsonFileMappingsSource(new SingleRootFileSource("src/test/resources/mappings")));

        ValidatableResponse response = given()
                .spec(new RequestSpecBuilder().build())
                .body("<Records><Record><DataSource>Criminal Court</DataSource><OffenderId>FAKE-OFFENDER-ID-0000000001</OffenderId><Name><First>TESTGUY</First><Middle></Middle><Last>SOMETHING</Last></Name></Record></Records>")
                .when()
                .post("http://localhost:8886" + "/fake/xml/save")
                .then();

        System.out.println("RESPONSE 1: " + response.extract().body().asString());

        response = given()
                .spec(new RequestSpecBuilder().build())
                .when()
                .post("http://localhost:8886" + "/fake/xml/retrieve")
                .then();

        System.out.println("RESPONSE 2: " + response.extract().body().asString());

        wiremock.stop();
    }

    @Test
    public void jsonResponseDefinitionTransformerFromMappingTest() {
        WireMockServer wiremock = new WireMockServer(wireMockConfig()
                .extensions(new DynamicStubs())
                .port(8886));
        wiremock.start();
        wiremock.loadMappingsUsing(new JsonFileMappingsSource(new SingleRootFileSource("src/test/resources/mappings")));

        ValidatableResponse response = given()
                .spec(new RequestSpecBuilder().build())
                .body("{\"query\":{\"emails\":[{\"address\":\"test.guy@gmail.com\",\"address_md5\":\"5d7d64c9c659f6ac4031a72b3578e9b3\"}]}}")
                .when()
                .post("http://localhost:8886" + "/fake/json/transform?name=Arturo&other=3")
                .then();

        System.out.println("RESPONSE: " + response.extract().body().asString());

        wiremock.stop();
    }

    @Test
    public void xmlResponseDefinitionTransformerFromMappingTestUsingRegex() {
        WireMockServer wiremock = new WireMockServer(wireMockConfig()
                .extensions(new DynamicStubs())
                .port(8886));
        wiremock.start();
        wiremock.loadMappingsUsing(new JsonFileMappingsSource(new SingleRootFileSource("src/test/resources/mappings")));

        ValidatableResponse response = given()
                .spec(new RequestSpecBuilder().build())
                .when()
                .get("http://localhost:8886" + "/wsdl")
                .then();

        System.out.println("RESPONSE: " + response.extract().body().asString());

        wiremock.stop();
    }

    @Test
    public void xmlResponseDefinitionTransformerFromMappingGettingTemplateFromProxy() {
        WireMockServer wiremock = new WireMockServer(wireMockConfig()
                .extensions(new DynamicStubs())
                .httpsPort(8886));
        wiremock.start();
        wiremock.loadMappingsUsing(new JsonFileMappingsSource(new SingleRootFileSource("src/test/resources/mappings")));
        RestAssured.useRelaxedHTTPSValidation();
        ValidatableResponse response = given()
                .spec(new RequestSpecBuilder().build())
                .when()
                .get("https://localhost:8886" + "/webservice/soap/AM2/?wsdl")
                .then();

        System.out.println("RESPONSE: " + response.extract().body().asString());

        wiremock.stop();
    }
}
