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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lunabulnes
 */
public class DynamicStubs extends ResponseDefinitionTransformer {

    private static Map<String, ResponseDefinition> savedResponses =  new HashMap<>();
    private final static String EXTENSION_PARAMS_NAME = "dynamicStubsParameters";
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
        ResponseDefinition transformedResponse = responseDefinition;
        String templateName = responseDefinition.getBodyFileName();
        Parameters dynamicStubsParameters = Parameters.of(parameters.getOrDefault(EXTENSION_PARAMS_NAME, null));

        try {
            if(templateName != null && !dynamicStubsParameters.isEmpty()) {
                String bodyTemplate = DataParser
                        .from(files.getBinaryFileNamed(responseDefinition.getBodyFileName()))
                        .toString();

                if(dynamicStubsParameters.containsKey(DSParamType.PLAIN_TEXT_PARAMS.getKey())) {
                    Parameters plainTextParameters = Parameters.of(dynamicStubsParameters.getOrDefault(DSParamType.PLAIN_TEXT_PARAMS.getKey(), null));
                    bodyTemplate = transformPlainTextTemplate(
                            request,
                            bodyTemplate,
                            plainTextParameters
                    );
                }
                if(dynamicStubsParameters.containsKey(DSParamType.XML_PARAMS.getKey())) {
                    Parameters xmlParameters = Parameters.of(dynamicStubsParameters.getOrDefault(DSParamType.XML_PARAMS.getKey(), null));
                    bodyTemplate = transformXmlTemplate(
                            request,
                            bodyTemplate,
                            xmlParameters
                    );
                }
                if(dynamicStubsParameters.containsKey(DSParamType.JSON_PARAMS.getKey())) {
                    Parameters jsonParameters = Parameters.of(dynamicStubsParameters.getOrDefault(DSParamType.JSON_PARAMS.getKey(), null));
                    bodyTemplate = transformJsonTemplate(
                            request,
                            bodyTemplate,
                            jsonParameters
                    );
                }

                transformedResponse = ResponseDefinitionBuilder
                        .like(responseDefinition)
                        .but()
                        .withBodyFile(null)
                        .withBody(bodyTemplate)
                        .build();

                if(dynamicStubsParameters.containsKey(DSParamType.SAVE_RESPONSE.getKey())) {
                    Parameters saveParams = Parameters.of(dynamicStubsParameters.get(DSParamType.SAVE_RESPONSE.getKey()));
                    String tag = getValueFromResponse(transformedResponse, DSUtils.parseWiremockParametersToJsonObject(saveParams));
                    savedResponses.put(tag, transformedResponse);
                }
                if(dynamicStubsParameters.containsKey(DSParamType.DELETE_RESPONSE.getKey())) {
                    Parameters deleteParams = Parameters.of(dynamicStubsParameters.get(DSParamType.DELETE_RESPONSE.getKey()));
                    String tag = getValueFromRequest(request, DSUtils.parseWiremockParametersToJsonObject(deleteParams));
                    savedResponses.remove(tag);
                }
            }
        } catch(Exception e) {
            System.err.println("Unable to transform Response: " + e.getMessage());
            e.printStackTrace();
        }

        return transformedResponse;
    }

    private String transformXmlTemplate(Request request, String template, Parameters parameters) throws Exception {
        Document xmlTemplate = DataParser
                .from(template)
                .toDocument();

        if(parameters.containsKey(DSParamType.FROM_REQUEST.getKey())) {
            JSONArray requestParams = DSUtils.parseWiremockParametersToJsonArray(parameters, DSParamType.FROM_REQUEST.getKey());
            transformXmlTemplateFromRequest(request, xmlTemplate, requestParams);
        }
        if(parameters.containsKey(DSParamType.FROM_SAVED_RESPONSE.getKey())) {
            JSONArray responseParams = DSUtils.parseWiremockParametersToJsonArray(parameters, DSParamType.FROM_SAVED_RESPONSE.getKey());
            ResponseDefinition response = retrieveSavedResponse(request, responseParams);
            transformXmlTemplateFromResponse(response, xmlTemplate, responseParams);
        }

        return DataParser
                .from(xmlTemplate)
                .toString();
    }

    private void transformXmlTemplateFromRequest(Request request, Document xmlTemplate, JSONArray parameters) throws Exception {
        parameters.forEach(item -> {
            JSONObject parameter = (JSONObject) item;
            try {
                String newValue = getValueFromRequest(request, parameter);
                updateXmlTemplateValues(xmlTemplate, parameter, newValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void transformXmlTemplateFromResponse(ResponseDefinition savedResponse, Document xmlTemplate, JSONArray parameters) throws Exception {
        parameters.forEach(item -> {
            JSONObject parameter = (JSONObject) item;
            if (!parameter.has(DSParamType.WITH_TAG.getKey())) {
                try {
                    String newValue = getValueFromResponse(savedResponse, parameter);
                    updateXmlTemplateValues(xmlTemplate, parameter, newValue);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private String transformJsonTemplate(Request request, String template, Parameters parameters) throws Exception {
        DocumentContext jsonTemplate = DataParser
                .from(template)
                .toDocumentContext();

        if(parameters.containsKey(DSParamType.FROM_REQUEST.getKey())) {
            JSONArray requestParams = DSUtils.parseWiremockParametersToJsonArray(parameters, DSParamType.FROM_REQUEST.getKey());
            transformJsonResponseFromRequest(request, jsonTemplate, requestParams);
        }
        if(parameters.containsKey(DSParamType.FROM_SAVED_RESPONSE.getKey())) {
            JSONArray responseParams = DSUtils.parseWiremockParametersToJsonArray(parameters, DSParamType.FROM_SAVED_RESPONSE.getKey());
            ResponseDefinition response = retrieveSavedResponse(request, responseParams);
            transformJsonTemplateFromResponse(response, jsonTemplate, responseParams);
        }

        return DataParser
                .from(jsonTemplate)
                .toString();
    }

    private void transformJsonResponseFromRequest(Request request, DocumentContext jsonTemplate, JSONArray parameters) throws Exception {
        for(int i = 0; i < parameters.length(); i++) {
            String newValue = getValueFromRequest(request, parameters.getJSONObject(i));
            updateJsonTemplateValues(
                    jsonTemplate,
                    parameters.getJSONObject(i),
                    newValue
            );
        }
    }

    private void transformJsonTemplateFromResponse(ResponseDefinition savedResponse, DocumentContext jsonTemplate, JSONArray parameters) throws Exception {
        for(int i = 0; i < parameters.length(); i++) {
            String newValue = getValueFromResponse(savedResponse, parameters.getJSONObject(i));
            updateJsonTemplateValues(
                    jsonTemplate,
                    parameters.getJSONObject(i),
                    newValue
            );
        }
    }

    private String transformPlainTextTemplate(Request request, String template, Parameters parameters) throws Exception {
        String plainTextTemplate = template;

        if(parameters.containsKey(DSParamType.FROM_REQUEST.getKey())) {
            JSONArray requestParams = DSUtils.parseWiremockParametersToJsonArray(parameters, DSParamType.FROM_REQUEST.getKey());
            plainTextTemplate = transformPlainTextTemplateFromRequest(request, plainTextTemplate, requestParams);
        }
        if(parameters.containsKey(DSParamType.FROM_SAVED_RESPONSE.getKey())) {
            JSONArray responseParams = DSUtils.parseWiremockParametersToJsonArray(parameters, DSParamType.FROM_SAVED_RESPONSE.getKey());
            ResponseDefinition response = retrieveSavedResponse(request, responseParams);
            plainTextTemplate = transformPlainTextTemplateFromResponse(response, plainTextTemplate, responseParams);
        }

        return plainTextTemplate;
    }

    private String transformPlainTextTemplateFromRequest(Request request, String plainTextTemplate, JSONArray parameters) throws Exception {
        String transformedTemplate = plainTextTemplate;

        for(int i = 0; i < parameters.length(); i++) {
            String newValue = getValueFromRequest(request, parameters.getJSONObject(i));
            transformedTemplate = updatePlainTextTemplateValues(plainTextTemplate, parameters.getJSONObject(i), newValue);
        }

        return transformedTemplate;
    }

    private String transformPlainTextTemplateFromResponse(ResponseDefinition savedResponse, String plainTextTemplate, JSONArray parameters) throws Exception {
        String transformedTemplate = plainTextTemplate;

        for(int i = 0; i < parameters.length(); i++) {
            String newValue = getValueFromResponse(savedResponse, parameters.getJSONObject(i));
            transformedTemplate = updatePlainTextTemplateValues(plainTextTemplate, parameters.getJSONObject(i), newValue);
        }

        return transformedTemplate;
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
        if(xmlNodes != null) {
            for (int i = 0; i < xmlNodes.getLength(); i++) {
                xmlNodes.item(i).setNodeValue(newValue);
            }
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

    private String updatePlainTextTemplateValues(String template, JSONObject parameter, String newValue) throws Exception {
        if(parameter.has(DSParamType.BY_REGEX.getKey())) {
            return replaceAllMatchingsInString(template, parameter.getString(DSParamType.BY_REGEX.getKey()), newValue);
        }
        return template;
    }

    private String replaceAllMatchingsInString(String string, String regex, String newValue) {
        return string.replaceAll(regex, newValue);
    }

    private static String getValueFromRequest(Request request, JSONObject parameter) throws Exception {
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

    private static String getValueFromResponse(ResponseDefinition response, JSONObject parameter) throws Exception {
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
            compoundValue += getValueFromRequest(request, parameters.getJSONObject(i));
        }

        return compoundValue;
    }

    private ResponseDefinition retrieveSavedResponse(Request request, JSONArray parameters) throws Exception {
        JSONObject tagParam = getFirstJsonObjectWithMatchingKey(parameters, DSParamType.WITH_TAG.getKey());
        String responseTag = getValueFromRequest(request, tagParam);

        for (int i = 0; i < savedResponses.size(); i++) {
            if(savedResponses.containsKey(responseTag)) {
                return savedResponses.get(responseTag);
            }
        }

        throw new Exception("Response not found");
    }

    private JSONObject getFirstJsonObjectWithMatchingKey(JSONArray jsonArray, String key) throws Exception {
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).has(key)) {
                return jsonArray.getJSONObject(i).getJSONObject(key);
            }
        }

        throw new Exception("JSONArray does not contains any JSON Element with matching key");
    }
}
