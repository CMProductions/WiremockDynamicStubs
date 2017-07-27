package com.cmp.wiremock.extension.utils;

import com.cmp.wiremock.extension.enums.DSFakeDataType;
import com.cmp.wiremock.extension.enums.DSParamType;
import com.github.javafaker.Faker;
import com.github.tomakehurst.wiremock.http.*;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lunabulnes
 */
public class DataGatherer {

    private final static Faker faker = new Faker();

    public static String getValueFromRequest(Request request, JSONObject parameter) throws Exception {
        if(parameter.has(DSParamType.FROM_NEW.getKey())) {
            return parameter.getString(DSParamType.FROM_NEW.getKey());
        }
        if(parameter.has(DSParamType.FROM_RANDOM.getKey())) {
            return getRandomValue(parameter.getString(DSParamType.FROM_RANDOM.getKey()));
        }
        if(parameter.has(DSParamType.FROM_QUERY_STRING.getKey())) {
            return getFromQuery(request.getUrl(), parameter.getString(DSParamType.FROM_QUERY_STRING.getKey()));
        }
        if(parameter.has(DSParamType.FROM_HEADER.getKey())) {
            return getFromHeader(request.getHeaders(), parameter.getString(DSParamType.FROM_HEADER.getKey()));
        }
        if(parameter.has(DSParamType.FROM_COOKIE.getKey())) {
            return getFromCookie(request.getCookies(), parameter.getString(DSParamType.FROM_COOKIE.getKey()));
        }
        if(parameter.has(DSParamType.COMPOUND_VALUE.getKey())) {
            return getCompoundData(request, parameter.getJSONArray(DSParamType.COMPOUND_VALUE.getKey()));
        }

        try {
            return getValueFromBody(request.getBodyAsString(), parameter);
        } catch (Exception e) {
            throw new Exception("No value found");
        }
    }

    public static String getValueFromResponse(LoggedResponse response, JSONObject parameter) throws Exception {
        if(parameter.has(DSParamType.FROM_NEW.getKey())) {
            return parameter.getString(DSParamType.FROM_NEW.getKey());
        }
        if(parameter.has(DSParamType.FROM_RANDOM.getKey())) {
            return getRandomValue(parameter.getString(DSParamType.FROM_RANDOM.getKey()));
        }

        try {
            return getValueFromBody(response.getBody(), parameter);
        } catch (Exception e) {
            throw new Exception("No value found");
        }
    }

    public static String getValueFromBody(String bodyString, JSONObject parameter) throws Exception {
        if(parameter.has(DSParamType.FROM_BODY_JSON_PATH.getKey())) {
            return getFromJsonPath(bodyString, parameter.getString(DSParamType.FROM_BODY_JSON_PATH.getKey()));
        }
        if(parameter.has(DSParamType.FROM_BODY_JSON_KEY.getKey())) {
            return getFromJsonKey(bodyString, parameter.getString(DSParamType.FROM_BODY_JSON_KEY.getKey()));
        }
        if(parameter.has(DSParamType.FROM_BODY_XML.getKey())) {
            return getFromXml(bodyString, parameter.getString(DSParamType.FROM_BODY_XML.getKey()));
        }
        if(parameter.has(DSParamType.FROM_BODY_PLAINTEXT.getKey())) {
            return getFromPlainText(bodyString, parameter.getString(DSParamType.FROM_BODY_XML.getKey()));
        }

        throw new Exception("No value found in body");
    }

    public static String getRandomValue(String randomType) throws Exception {
        int maxNumber = getMaxNumberChars(randomType);

        if(randomType.contains(DSFakeDataType.INTEGER.dataKey())) {
            return String.valueOf((int)faker.number().randomDouble(0, getNumericMin(maxNumber), getNumericMax(maxNumber)));
        }
        if(randomType.contains(DSFakeDataType.POS_INTEGER.dataKey())) {
            return String.valueOf((int)faker.number().randomDouble(0, 0, getNumericMax(maxNumber)));
        }
        if(randomType.contains(DSFakeDataType.NEG_INTEGER.dataKey())) {
            return String.valueOf((int)faker.number().randomDouble(0, getNumericMin(maxNumber), 0));
        }
        if(randomType.contains(DSFakeDataType.FLOAT.dataKey())) {
            return String.valueOf(faker.number().randomDouble(3, getNumericMin(maxNumber), getNumericMax(maxNumber)));
        }
        if(randomType.contains(DSFakeDataType.POS_FLOAT.dataKey())) {
            return String.valueOf(faker.number().randomDouble(3, 0, getNumericMax(maxNumber)));
        }
        if(randomType.contains(DSFakeDataType.NEG_FLOAT.dataKey())) {
            return String.valueOf(faker.number().randomDouble(3, getNumericMin(maxNumber), 0));
        }
        if(randomType.contains(DSFakeDataType.STRING.dataKey())) {
            return RandomStringUtils.randomAlphabetic(maxNumber);
        }
        if(randomType.contains(DSFakeDataType.TIMESTAMP.dataKey())) {
            return String.valueOf(System.currentTimeMillis());
        }
        if(randomType.contains(DSFakeDataType.EMAIL.dataKey())) {
            return faker.internet().emailAddress();
        }
        if(randomType.contains(DSFakeDataType.NAME.dataKey())) {
            return faker.name().firstName();
        }
        if(randomType.contains(DSFakeDataType.LAST_NAME.dataKey())) {
            return faker.name().lastName();
        }
        if(randomType.contains(DSFakeDataType.FULL_NAME.dataKey())) {
            return faker.name().fullName();
        }
        if(randomType.contains(DSFakeDataType.PHONE.dataKey())) {
            return faker.phoneNumber().phoneNumber();
        }
        if(randomType.contains(DSFakeDataType.ADDRESS.dataKey())) {
            return faker.address().fullAddress();
        }

        throw new Exception("Can not generate data of the specified type");
    }

    public static int getMaxNumberChars(String randomType) throws Exception {
        int index = randomType.lastIndexOf("_");
        String maxNumber = randomType.substring(index + 1);

        try {
            return Integer.valueOf(maxNumber);
        } catch (NumberFormatException e) {
            return 6;
        }
    }

    public static int getNumericMax(int maxNumber) {
        int max = 0;

        for(int i = 0; i < maxNumber; i++) {
            max = max * 10 + 9;
        }

        return max;
    }

    public static int getNumericMin(int maxNumber) {
        return getNumericMax(maxNumber) * -1;
    }

    public static String getFromQuery(String requestURL, String queryParameter) throws Exception {
        Map<String, String> queryParameters;
        queryParameters = DSUtils.splitQuery(requestURL);
        return queryParameters.get(queryParameter);
    }

    public static String getFromJsonPath(String requestBody, String jsonPath) throws Exception {
        return JsonPath.read(requestBody, jsonPath);
    }

    public static String getFromJsonKey(String requestBody, String jsonKey) throws Exception {
        return new JSONObject(requestBody).get(jsonKey).toString();
    }

    public static String getFromXml(String requestBody, String xpath) throws Exception {
        Document xmlRequest = DataParser
                .from(requestBody)
                .toDocument();

        return getMatchingNodesByXpath(xmlRequest, xpath)
                .item(0)
                .getNodeValue();
    }

    public static String getFromPlainText(String requestBody, String regex) throws Exception {
        Pattern regexPattern = Pattern.compile(regex);
        Matcher regexMatcher = regexPattern.matcher(requestBody);

        if (regexMatcher.find()) {
            return regexMatcher.group(0);
        }

        throw new Exception("No match found");
    }

    public static String getFromHeader(HttpHeaders requestHeaders, String headerKey) {
        return requestHeaders.getHeader(headerKey).toString();
    }

    public static String getFromCookie(Map<String, Cookie> requestCookies, String cookieKey) {
        return requestCookies.get(cookieKey).toString();
    }

    public static String getCompoundData(Request request, JSONArray parameters) throws Exception {
        String compoundValue = "";

        for (int i = 0; i < parameters.length(); i++) {
            compoundValue += getValueFromRequest(request, parameters.getJSONObject(i));
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
}
