package com.cmp.wiremock.extension.utils;

import com.github.javafaker.Faker;
import com.github.tomakehurst.wiremock.http.Cookie;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.LoggedResponse;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.servlet.WireMockHttpServletRequestAdapter;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.cmp.wiremock.extension.enums.DSFakeDataType.*;
import static com.cmp.wiremock.extension.enums.DSParamType.*;

/**
 * Created by lunabulnes
 */
public class DataGatherer {

    private final static Faker faker = new Faker();

    public static String getValue(Object wiremockObject, JSONObject parameter) throws Exception {
        if(parameter.has(FROM_NEW.getKey())) {
            return parameter.getString(FROM_NEW.getKey());
        }
        if(parameter.has(FROM_RANDOM.getKey())) {
            return getRandomValue(parameter.getString(FROM_RANDOM.getKey()));
        }
        if(parameter.has(FROM_HEADER.getKey())) {
            return getFromHeader(getHeaders(wiremockObject), parameter.getString(FROM_HEADER.getKey()));
        }
        if(parameter.has(FROM_COOKIE.getKey())) {
            return getFromCookie(((Request)wiremockObject).getCookies(), parameter.getString(FROM_COOKIE.getKey()));
        }
        if(parameter.has(COMPOUND_VALUE.getKey())) {
            return getCompoundValue(wiremockObject, parameter.getJSONArray(COMPOUND_VALUE.getKey()));
        }

        try {
            return getValueFromBody(getBody(wiremockObject), parameter);
        } catch (Exception bodyException) {
            try {
                return getValueFromUrl(getUrl(wiremockObject), parameter);
            } catch (Exception urlException) {
                throw new Exception("No value found for: " + parameter.toString());
            }
        }
    }

    private static String getValueFromBody(String bodyString, JSONObject parameter) throws Exception {
        if(parameter.has(FROM_BODY_JSON_PATH.getKey())) {
            return getFromJsonPath(bodyString, parameter.getString(FROM_BODY_JSON_PATH.getKey()));
        }
        if(parameter.has(FROM_BODY_JSON_KEY.getKey())) {
            return getFromJsonKey(bodyString, parameter.getString(FROM_BODY_JSON_KEY.getKey()));
        }
        if(parameter.has(FROM_BODY_XML.getKey())) {
            return getFromXml(bodyString, parameter.getString(FROM_BODY_XML.getKey()));
        }
        if(parameter.has(FROM_BODY_PLAINTEXT.getKey())) {
            return getFromPlainText(bodyString, parameter.getString(FROM_BODY_XML.getKey()));
        }

        throw new Exception("No value found in body");
    }

    private static String getValueFromUrl(String url, JSONObject parameter) throws Exception {
        if(parameter.has(FROM_URL_QUERY_PARAMETER.getKey())) {
            return getFromQuery(url, parameter.getString(FROM_URL_QUERY_PARAMETER.getKey()));
        }
        if(parameter.has(FROM_URL_ASPLAINTEXT.getKey())) {
            return getFromPlainText(url, parameter.getString(FROM_URL_QUERY_PARAMETER.getKey()));
        }
        if(parameter.has(FROM_URL_BASEURL.getKey())) {
            return getBaseUrl(url);
        }
        if(parameter.has(FROM_URL_PATH.getKey())) {
            return new URL(url).getPath();
        }
        if(parameter.has(FROM_URL_QUERY_STRING.getKey())) {
            return new URL(url).getQuery();
        }

        throw new Exception("No value found in URL");
    }

    private static String getRandomValue(String randomType) throws Exception {
        int maxNumber = getMaxNumberChars(randomType);

        if(randomType.contains(INTEGER.dataKey())) {
            return String.valueOf((int)faker.number().randomDouble(0, getNumericMin(maxNumber), getNumericMax(maxNumber)));
        }
        if(randomType.contains(POS_INTEGER.dataKey())) {
            return String.valueOf((int)faker.number().randomDouble(0, 0, getNumericMax(maxNumber)));
        }
        if(randomType.contains(NEG_INTEGER.dataKey())) {
            return String.valueOf((int)faker.number().randomDouble(0, getNumericMin(maxNumber), 0));
        }
        if(randomType.contains(FLOAT.dataKey())) {
            return String.valueOf(faker.number().randomDouble(3, getNumericMin(maxNumber), getNumericMax(maxNumber)));
        }
        if(randomType.contains(POS_FLOAT.dataKey())) {
            return String.valueOf(faker.number().randomDouble(3, 0, getNumericMax(maxNumber)));
        }
        if(randomType.contains(NEG_FLOAT.dataKey())) {
            return String.valueOf(faker.number().randomDouble(3, getNumericMin(maxNumber), 0));
        }
        if(randomType.contains(STRING.dataKey())) {
            return RandomStringUtils.randomAlphabetic(maxNumber);
        }
        if(randomType.contains(TIMESTAMP.dataKey())) {
            return String.valueOf(System.currentTimeMillis());
        }
        if(randomType.contains(EMAIL.dataKey())) {
            return faker.internet().emailAddress();
        }
        if(randomType.contains(NAME.dataKey())) {
            return faker.name().firstName();
        }
        if(randomType.contains(LAST_NAME.dataKey())) {
            return faker.name().lastName();
        }
        if(randomType.contains(FULL_NAME.dataKey())) {
            return faker.name().fullName();
        }
        if(randomType.contains(PHONE.dataKey())) {
            return faker.phoneNumber().phoneNumber();
        }
        if(randomType.contains(ADDRESS.dataKey())) {
            return faker.address().fullAddress();
        }

        throw new Exception("Can not generate data of the specified type: " + randomType);
    }

    private static int getMaxNumberChars(String randomType) throws Exception {
        int index = randomType.lastIndexOf("_");
        String maxNumber = randomType.substring(index + 1);
        try {
            return Integer.valueOf(maxNumber);
        } catch (NumberFormatException e) {
            return 6;
        }
    }

    private static int getNumericMax(int maxNumber) {
        int max = 0;
        for(int i = 0; i < maxNumber; i++) {
            max = max * 10 + 9;
        }
        return max;
    }

    private static int getNumericMin(int maxNumber) {
        return getNumericMax(maxNumber) * -1;
    }

    private static String getFromQuery(String urlString, String queryParameter) throws Exception {
        Map<String, String> queryParameters;
        queryParameters = DSUtils.splitQuery(urlString);
        return queryParameters.get(queryParameter);
    }

    private static String getBaseUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        String baseUrl = "";

        baseUrl += url.getProtocol() + "://" + url.getHost();
        if (url.getPort() != -1) {
            baseUrl += ":" + String.valueOf(url.getPort());
        }
        return baseUrl;
    }

    private static String getFromJsonPath(String requestBody, String jsonPath) throws Exception {
        return JsonPath.read(requestBody, jsonPath);
    }

    private static String getFromJsonKey(String requestBody, String jsonKey) throws Exception {
        return new JSONObject(requestBody).get(jsonKey).toString();
    }

    private static String getFromXml(String requestBody, String xpath) throws Exception {
        Document xmlRequest = DataParser
                .from(requestBody)
                .toDocument();

        return getMatchingNodesByXpath(xmlRequest, xpath)
                .item(0)
                .getNodeValue();
    }

    private static String getFromPlainText(String string, String regex) throws Exception {
        Pattern regexPattern = Pattern.compile(regex);
        Matcher regexMatcher = regexPattern.matcher(string);
        if (regexMatcher.find()) {
            return regexMatcher.group(0);
        }
        throw new Exception("No match found");
    }

    private static String getFromHeader(HttpHeaders requestHeaders, String headerKey) {
        return requestHeaders.getHeader(headerKey).toString();
    }

    private static String getFromCookie(Map<String, Cookie> requestCookies, String cookieKey) {
        return requestCookies.get(cookieKey).toString();
    }

    private static String getCompoundValue(Object wiremockObject, JSONArray parameters) throws Exception {
        String compoundValue = "";
        for (int i = 0; i < parameters.length(); i++) {
            compoundValue += getValue(wiremockObject, parameters.getJSONObject(i));
        }
        return compoundValue;
    }

    public static NodeList getMatchingNodesByXpath(Document document, String xpathExpression) throws Exception {
        XPath xpath = XPathFactory
                .newInstance()
                .newXPath();
        return (NodeList) xpath.
                compile(xpathExpression)
                .evaluate(
                        document,
                        XPathConstants.NODESET
                );
    }

    public static NodeList getMatchingNodesByNodeName(Document document, String nodeName) throws Exception {
        return getMatchingNodesByXpath(document, "//" + nodeName + "/text()");
    }

    private static HttpHeaders getHeaders(Object wiremockObject) throws Exception {
        if(WireMockHttpServletRequestAdapter.class.isInstance(wiremockObject)) {
            return ((WireMockHttpServletRequestAdapter)wiremockObject).getHeaders();
        }
        if(LoggedRequest.class.isInstance(wiremockObject)) {
            return ((LoggedRequest)wiremockObject).getHeaders();
        }
        if(LoggedResponse.class.isInstance(wiremockObject)) {
            return ((LoggedResponse)wiremockObject).getHeaders();
        }
        throw new Exception("Unable to get headers for provided class: " + wiremockObject.getClass());
    }

    private static String getBody(Object wiremockObject) throws Exception {
        if(WireMockHttpServletRequestAdapter.class.isInstance(wiremockObject)) {
            return ((WireMockHttpServletRequestAdapter)wiremockObject).getBodyAsString();
        }
        if (LoggedRequest.class.isInstance(wiremockObject)) {
            return ((LoggedRequest) wiremockObject).getBodyAsString();
        }
        if (LoggedResponse.class.isInstance(wiremockObject)) {
            return ((LoggedResponse) wiremockObject).getBody();
        }
        throw new Exception("Unable to get body for provided class: " + wiremockObject.getClass());
    }

    private static String getUrl(Object wiremockObject) throws Exception {
        if(WireMockHttpServletRequestAdapter.class.isInstance(wiremockObject)) {
            return ((WireMockHttpServletRequestAdapter)wiremockObject).getAbsoluteUrl();
        }
        if (LoggedRequest.class.isInstance(wiremockObject)) {
            return ((LoggedRequest) wiremockObject).getAbsoluteUrl();
        }
        throw new Exception("Unable to get body for provided class: " + wiremockObject.getClass());
    }
}
