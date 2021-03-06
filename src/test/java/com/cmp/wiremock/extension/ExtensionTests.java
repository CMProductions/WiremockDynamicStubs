package com.cmp.wiremock.extension;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.response.ValidatableResponse;
import net.minidev.json.JSONObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
                .extensions(new Postbacks())
                .port(8886));
        wiremock.start();

        wiremock.stubFor(get(urlEqualTo("/fake/endpoint"))
                .withPostServeAction("Postbacks", new JSONObject())
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

    @Test
    public void checkPostbackExtensionWorks() {
        WireMockServer wiremock = new WireMockServer(wireMockConfig()
                .extensions(new Postbacks(), new DynamicStubs())
                .port(8886));
        wiremock.start();
        wiremock.loadMappingsUsing(new JsonFileMappingsSource(new SingleRootFileSource("src/test/resources/mappings")));
        RestAssured.useRelaxedHTTPSValidation();
        given().spec(new RequestSpecBuilder().build())
                .when()
                .get("http://localhost:8886" + "/postback")
                .then();

        wiremock.stop();
    }

    @Test
    public void checkNothingIsBrokenAfterPostbackDevelopment() {
        WireMockServer wiremock = new WireMockServer(wireMockConfig()
                .extensions(new DynamicStubs())
                .port(8886));
        wiremock.start();
        wiremock.loadMappingsUsing(new JsonFileMappingsSource(new SingleRootFileSource("src/test/resources/mappings")));
        RestAssured.useRelaxedHTTPSValidation();
        ValidatableResponse response = given().spec(new RequestSpecBuilder().build())
                .body("<Records><Record><DataSource>Criminal Court</DataSource><OffenderId>FAKE-OFFENDER-ID-0000000001</OffenderId><Name><First>sdfsdfsdf</First><Middle></Middle><Last>sdfsdfsdf</Last></Name></Record></Records>")
                .when()
                .post("http://localhost:8886" + "/complexmapping")
                .then();

        System.out.println("RESPONSE: " + response.extract().body().asString());

        wiremock.stop();
    }

    @Test
    public void checkPostbackExtensionWorksWithUrlEncodedBody() {
        WireMockServer wiremock = new WireMockServer(wireMockConfig()
                .extensions(new Postbacks(), new DynamicStubs())
                .port(8886));
        wiremock.start();
        wiremock.loadMappingsUsing(new JsonFileMappingsSource(new SingleRootFileSource("src/test/resources/mappings")));
        RestAssured.useRelaxedHTTPSValidation();
        given().spec(new RequestSpecBuilder().build())
                .when()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("sv_user_id=807779&email=luna.bulnes%2Bnew%40teamcmp.com&affiliate_id=109407&subid=default&tour_id=25247")
                .post("http://localhost:8886" + "/urlEncoded")
                .then();

        wiremock.stop();
    }

    @Test
    public void listToJSON() {
        List<NameValuePair> list = new ArrayList<>();
        NameValuePair pair = new BasicNameValuePair("first", "1");
        list.add(pair);
        pair = new BasicNameValuePair("second", "2");
        list.add(pair);

        org.json.JSONObject jsonParams = new org.json.JSONObject();
        list.forEach(param -> jsonParams.put(param.getName(), param.getValue()));

        System.out.println("JSON: " + jsonParams.toString());
    }

    @Test
    public void sendPostWithBody() {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("http://dev.seekverify.com/ss/upgrade");

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("sv_user_id", "807780"));
            params.add(new BasicNameValuePair("join_length", "30"));
            params.add(new BasicNameValuePair("trial_type", "person"));
            params.add(new BasicNameValuePair("rebillAmount", "35.95"));
            params.add(new BasicNameValuePair("biller_id", "5"));
            params.add(new BasicNameValuePair("expireDate", "2099-11-28 14:31:49"));
            params.add(new BasicNameValuePair("is_trial_member", "1"));
            httpPost.setEntity(new UrlEncodedFormEntity(params));

/*
            String json = "{\"sv_user_id\":\"807780\",\"join_length\":\"30\",\"trial_type\":\"person\",\"rebillAmount\":\"35.95\",\"biller_id\":\"5\",\"expireDate\":\"2099-11-28 14:31:49\",\"is_trial_member\":\"1\"}";
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
*/
            CloseableHttpResponse response = client.execute(httpPost);
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
