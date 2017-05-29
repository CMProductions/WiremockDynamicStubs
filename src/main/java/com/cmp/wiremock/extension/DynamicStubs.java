package com.cmp.wiremock.extension;

import com.cmp.wiremock.extension.enums.DSFakeDataType;
import com.cmp.wiremock.extension.enums.DSParamType;
import com.cmp.wiremock.extension.utils.DSUtils;
import com.cmp.wiremock.extension.utils.DataParser;
import com.github.javafaker.Faker;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Cookie;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import com.jayway.jsonpath.DocumentContext;
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
public class DynamicStubs extends ResponseDefinitionTransformer {

    private static Map<String, ResponseDefinition> savedResponses;

    private final static String EXTENSION_PARAMS_NAME = "dynamicStubsParameters";
    private final static String XML_PARAMS = "transformXmlNode";
    private final static String JSON_PARAMS = "transformJsonNode";

    private final static Faker faker = new Faker();

    public String getName() {
        return "DynamicStubs";
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
        ResponseDefinition newResponse = responseDefinition;
        String templateName = responseDefinition.getBodyFileName();
        Parameters dynamicStubsParameters = (Parameters) parameters.getOrDefault(EXTENSION_PARAMS_NAME, null);

        try {
            if(templateName != null && dynamicStubsParameters!= null) {
                if(isXmlFile(templateName)) {
                    newResponse = transformXmlTemplate(
                            request,
                            responseDefinition,
                            files,
                            dynamicStubsParameters
                    );
                }
                if (isJsonFile(templateName)) {
                    newResponse = transformJsonResponseFromRequest(
                            request,
                            responseDefinition,
                            files,
                            getJsonParameters(dynamicStubsParameters)
                    );
                }
            }
        } catch(Exception e) {
            System.err.println("Unable to transform Response: " + e.getMessage());
        }

        return newResponse;
    }

    private static boolean isXmlFile(String fileName) {
        return fileName.endsWith(".xml");
    }

    private static boolean isJsonFile(String fileName) {
        return fileName.endsWith(".json");
    }

    private static JSONArray getXmlParameters(Object parameters) {
        return DSUtils.parseWiremockParametersToJsonArray(parameters, XML_PARAMS);
    }

    private static JSONArray getJsonParameters(Object parameters) {
        return DSUtils.parseWiremockParametersToJsonArray(parameters, JSON_PARAMS);
    }

    private ResponseDefinition transformXmlTemplate(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) throws Exception {
        Document xmlTemplate = DataParser
                .from(files.getBinaryFileNamed(responseDefinition.getBodyFileName()))
                .toDocument();

        if(parameters.getOrDefault(DSParamType.FROM_REQUEST.getKey(), null) != null) {
            transformXmlTemplateFromRequest(request, xmlTemplate, getXmlParameters(parameters));
        }
        if(parameters.getOrDefault(DSParamType.FROM_SAVED_RESPONSE.getKey(), null) != null) {
            ResponseDefinition response = retrievedSavedResponse(parameters.getString(DSParamType.FROM_SAVED_RESPONSE.getKey()));
            transformXmlTemplateFromResponse(response, xmlTemplate, getXmlParameters(parameters));
        }

        String transformedBody = DataParser
                .from(xmlTemplate)
                .toString();

        return ResponseDefinitionBuilder
                .like(responseDefinition)
                .but()
                .withBodyFile(null)
                .withBody(transformedBody)
                .build();
    }

    private void transformXmlTemplateFromRequest(Request request, Document xmlTemplate, JSONArray parameters) throws Exception {
        parameters.forEach(item -> {
            JSONObject parameter = (JSONObject) item;
            try {
                String newValue = getNewValue(request, parameter);
                updateXmlTemplateValues(xmlTemplate, parameter, newValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void transformXmlTemplateFromResponse(ResponseDefinition savedResponse, Document xmlTemplate, JSONArray parameters) throws Exception {
        parameters.forEach(item -> {
            JSONObject parameter = (JSONObject) item;
            try {
                String newValue = getValueFromBody(savedResponse.getBody(), parameter);
                updateXmlTemplateValues(xmlTemplate, parameter, newValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private ResponseDefinition transformJsonResponseFromRequest(Request request, ResponseDefinition responseDefinition, FileSource files, JSONArray parameters) throws Exception {
        DocumentContext jsonTemplate = DataParser
                .from(files.getBinaryFileNamed(responseDefinition.getBodyFileName()))
                .toDocumentContext();

        for(int i = 0; i < parameters.length(); i++) {
            String newValue = getNewValue(request, parameters.getJSONObject(i));
            updateJsonTemplateValues(
                    jsonTemplate,
                    parameters.getJSONObject(i),
                    newValue
            );
        }

        return ResponseDefinitionBuilder
                .like(responseDefinition)
                .but()
                .withBodyFile(null)
                .withBody(jsonTemplate.jsonString())
                .build();
    }

    private void transformJsonTemplateFromResponse(ResponseDefinition savedResponse, DocumentContext jsonTemplate, JSONArray parameters) throws Exception {
        for(int i = 0; i < parameters.length(); i++) {
            String newValue = getValueFromBody(savedResponse.getBody(), parameters.getJSONObject(i));
            updateJsonTemplateValues(
                    jsonTemplate,
                    parameters.getJSONObject(i),
                    newValue
            );
        }
    }

    private void updateXmlTemplateValues(Document template, JSONObject parameter, String newValue) throws Exception {
        NodeList nodeList = null;

        if(parameter.has(DSParamType.BY_XPATH.getKey())) {
            nodeList = getMatchingNodesByXpath(template, parameter.getString(DSParamType.BY_XPATH.getKey()));
        }
        if(parameter.has(DSParamType.BY_NODE_NAME.getKey())) {
            nodeList = getMatchingNodesByNodeName(template, parameter.getString(DSParamType.BY_NODE_NAME.getKey()));
        }

        updateXmlNodes(nodeList, newValue);
    }

    private void updateXmlNodes(NodeList xmlNodes, String newValue) throws Exception {
        for(int i = 0; i < xmlNodes.getLength(); i++) {
            xmlNodes.item(i).setNodeValue(newValue);
        }
    }

    private static NodeList getMatchingNodesByXpath(Document document, String xpathExpression) throws Exception {
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

    private static NodeList getMatchingNodesByNodeName(Document document, String nodeName) throws Exception {
        return getMatchingNodesByXpath(document, "//" + nodeName + "/text()");
    }

    private void updateJsonTemplateValues(DocumentContext template, JSONObject parameter, String newValue) throws Exception {
        if(parameter.has(DSParamType.BY_JSON_PATH.getKey())) {
            updateJsonByPath(template, parameter.getString(DSParamType.BY_JSON_PATH.getKey()), newValue);
        }
        if(parameter.has(DSParamType.BY_JSON_KEY.getKey())) {
            updateJsonByPath(template, parameter.getString(DSParamType.BY_JSON_KEY.getKey()), newValue);
        }
    }

    private void updateJsonByPath(DocumentContext template, String jsonPath, String newValue) throws Exception {
        template.set(jsonPath, newValue);
    }

    private static String getNewValue(Request request, JSONObject parameter) throws Exception {
        if(parameter.has(DSParamType.FROM_NEW.getKey())) {
            return parameter.getString(DSParamType.FROM_NEW.getKey());
        }
        if(parameter.has(DSParamType.FROM_RANDOM.getKey())) {
            return getRandomValue(parameter.getString(DSParamType.FROM_RANDOM.getKey()));
        }
        if(parameter.has(DSParamType.FROM_RANDOM.getKey())) {
            return getRandomValue(parameter.getString(DSParamType.FROM_RANDOM.getKey()));
        }
        if(parameter.has(DSParamType.FROM_QUERY_STRING.getKey())) {
            return getFromQuery(request.getUrl(), parameter.getString(DSParamType.FROM_QUERY_STRING.getKey()));
        }
        if(parameter.has(DSParamType.FROM_BODY_JSON_PATH.getKey())) {
            return getFromJsonPath(request.getBodyAsString(), parameter.getString(DSParamType.FROM_BODY_JSON_PATH.getKey()));
        }
        if(parameter.has(DSParamType.FROM_BODY_JSON_KEY.getKey())) {
            return getFromJsonKey(request.getBodyAsString(), parameter.getString(DSParamType.FROM_BODY_JSON_KEY.getKey()));
        }
        if(parameter.has(DSParamType.FROM_BODY_XML.getKey())) {
            return getFromXml(request.getBodyAsString(), parameter.getString(DSParamType.FROM_BODY_XML.getKey()));
        }
        if(parameter.has(DSParamType.FROM_BODY_PLAINTEXT.getKey())) {
            return getFromPlainText(request.getBodyAsString(), parameter.getString(DSParamType.FROM_BODY_XML.getKey()));
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

    private static String getValueFromBody(String bodyString, JSONObject parameter) throws Exception {
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



    private static String getRandomValue(String randomType) throws Exception {
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

    private static String getFromQuery(String requestURL, String queryParameter) throws Exception {
        Map<String, String> queryParameters;
        queryParameters = DSUtils.splitQuery(requestURL);
        return queryParameters.get(queryParameter);
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

    private static String getFromPlainText(String requestBody, String regex) throws Exception {
        Pattern regexPattern = Pattern.compile(regex);
        Matcher regexMatcher = regexPattern.matcher(requestBody);

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

    private static String getCompoundData(Request request, JSONArray parameters) throws Exception {
        String compoundValue = "";

        for (int i = 0; i < parameters.length(); i++) {
            compoundValue += getNewValue(request, parameters.getJSONObject(i));
        }

        return compoundValue;
    }

    private static ResponseDefinition retrievedSavedResponse(String responseTag) throws Exception {
        for (int i = 0; i < savedResponses.size(); i++) {
            if(savedResponses.containsKey(responseTag)) {
                return savedResponses.get(responseTag);
            }
        }

        throw new Exception("Response not found");
    }
}
