package com.cmp.wiremock.extension;

import com.cmp.wiremock.extension.enums.DSParamType;
import com.cmp.wiremock.extension.utils.DSUtils;
import com.cmp.wiremock.extension.utils.DataParser;
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
        Object dynamicStubsParameters = parameters.getOrDefault("dynamicStubsParameters", null);

        try {
            if(templateName != null && dynamicStubsParameters!= null) {
                if(isXmlFile(templateName)) {
                    JSONArray xmlParams = xmlParameters(dynamicStubsParameters);
                    newResponse = transformXmlResponse(request, responseDefinition, files, xmlParams);
                }
                if (isJsonFile(templateName)) {
                    JSONArray jsonParams = jsonParameters(dynamicStubsParameters);
                    newResponse = transformJsonResponse(request, responseDefinition, files, jsonParams);
                }
            }
        } catch(Exception e) {
            System.out.println("Unable to transform Response");
            System.out.println(e.getMessage());
        }

        return newResponse;
    }

    private static boolean isXmlFile(String fileName) {
        //TODO: Mejorar esto
        return fileName.endsWith(".xml");
    }

    private static boolean isJsonFile(String fileName) {
        //TODO: Mejorar esto
        return fileName.endsWith(".json");
    }

    private JSONArray xmlParameters(Object parameters) {
        return parseObjectToJsonArray(parameters, "transformXmlNode");
    }

    private JSONArray jsonParameters(Object parameters) {
        return parseObjectToJsonArray(parameters, "transformJsonNode");
    }

    private ResponseDefinition transformXmlResponse(Request request, ResponseDefinition responseDefinition, FileSource files, JSONArray parameters) throws Exception {
        Document xmlTemplate = DataParser
                .from(files.getBinaryFileNamed(responseDefinition.getBodyFileName()))
                .toDocument();

        parameters.forEach(item -> {
            JSONObject parameter = (JSONObject) item;
            try {
                String newValue = getNewValue(request, parameter);
                updateXmlTemplateValues(xmlTemplate, parameter, newValue);
            } catch (Exception e) {
                throw  new RuntimeException(e);
            }
        });

        String newBodyResponse = DataParser
                .from(xmlTemplate)
                .toString();

        return ResponseDefinitionBuilder
                .like(responseDefinition)
                .but()
                .withBodyFile(null)
                .withBody(newBodyResponse)
                .build();
    }

    private JSONArray parseObjectToJsonArray(Object parameters, String paramKey) {
        Parameters allParameters = Parameters.of(parameters);
        Object specificParameters = allParameters.getOrDefault(paramKey, null);
        JSONArray formattedParameters = new JSONArray();

        if(specificParameters != null) {
            String jsonString = specificParameters.toString()
                    .replaceAll("(?! )(?!\\[)(?!])(?<=[={}, ])([^{},]+?)(?=[{}=,])", "\"$1\"")
                    .replaceAll("=", ":");
            formattedParameters = new JSONArray(jsonString);
        }

        return formattedParameters;
    }

    private ResponseDefinition transformJsonResponse(Request request, ResponseDefinition responseDefinition, FileSource files, JSONArray parameters) throws Exception {
        DocumentContext jsonTemplate = DataParser
                .from(files.getBinaryFileNamed(responseDefinition.getBodyFileName()))
                .toDocumentContext();

        for(int i = 0; i < parameters.length(); i++) {
            String newValue = getNewValue(request, parameters.getJSONObject(i));
            updateJsonTemplateValues(jsonTemplate, parameters.getJSONObject(i), newValue);
        }

        return ResponseDefinitionBuilder
                .like(responseDefinition)
                .but()
                .withBodyFile(null)
                .withBody(jsonTemplate.jsonString())
                .build();
    }

    private static void updateXmlTemplateValues(Document template, JSONObject parameter, String newValue) throws Exception {
        NodeList nodeList = null;

        if(parameter.has(DSParamType.BY_XPATH.getKey())) {
            nodeList = getMatchingNodesByXpath(template, parameter.getString(DSParamType.BY_XPATH.getKey()));
        }
        if(parameter.has(DSParamType.BY_NODE_NAME.getKey())) {
            nodeList = getMatchingNodesByNodeName(template, parameter.getString(DSParamType.BY_NODE_NAME.getKey()));
        }

        if (nodeList != null) {
            updateXmlNodes(nodeList, newValue);
        } else {
            throw new Exception("No matching nodes found");
        }
    }

    private static void updateXmlNodes(NodeList xmlNodes, String newValue) throws Exception {
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
                .evaluate(document, XPathConstants.NODESET);
    }

    private static NodeList getMatchingNodesByNodeName(Document document, String nodeName) throws Exception {
        return getMatchingNodesByXpath(document, "//" + nodeName);
    }

    private static void updateJsonTemplateValues(DocumentContext template, JSONObject parameter, String newValue) throws Exception {
        if(parameter.has(DSParamType.BY_JSON_PATH.getKey())) {
            updateJsonByPath(template, parameter.getString(DSParamType.BY_JSON_PATH.getKey()), newValue);
        }
        if(parameter.has(DSParamType.BY_JSON_KEY.getKey())) {
            updateJsonByPath(template, parameter.getString(DSParamType.BY_JSON_KEY.getKey()), newValue);
        }
    }

    private static void updateJsonByPath(DocumentContext template, String jsonPath, String newValue) throws Exception {
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

        throw new Exception("No value found");
    }

    private static String getRandomValue(String randomType) {
        //TODO: Ampliar implementacion con Faker
        return String.valueOf(System.currentTimeMillis());
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

    private static String getFromHeader(HttpHeaders requestHeaders, String headerKey) throws Exception {
        return requestHeaders.getHeader(headerKey).toString();
    }

    private static String getFromCookie(Map<String, Cookie> requestCookies, String cookieKey) throws Exception {
        return requestCookies.get(cookieKey).toString();
    }

    private static String getCompoundData(Request request, JSONArray parameters) throws Exception {
        String compoundValue = "";

        for (int i = 0; i < parameters.length(); i++) {
            compoundValue += getNewValue(request, parameters.getJSONObject(i));
        }

        return compoundValue;
    }
}
