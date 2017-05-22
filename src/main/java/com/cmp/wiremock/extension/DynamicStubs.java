package com.cmp.wiremock.extension;

import com.cmp.wiremock.extension.enums.DSValueType;
import com.cmp.wiremock.extension.utils.DSUtils;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.BinaryFile;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Created by lunabulnes
 */
public class DynamicStubs extends ResponseDefinitionTransformer {

    public String getName() {
        return "DynamicStubs";
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
        ResponseDefinition newResponse = responseDefinition;
        String bodyFileName = responseDefinition.getBodyFileName();
        Object dynamicStubsParameters = parameters.getOrDefault("dynamicStubsParameters", null);

        try {
            if(bodyFileName != null && dynamicStubsParameters!= null) {
                if(isXmlFile(bodyFileName)) {
                    JSONArray xmlParams = xmlParameters(dynamicStubsParameters);
                    newResponse = transformXmlResponse(request, responseDefinition, files, xmlParams);
                }
                if (isJsonFile(bodyFileName)) {
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

    @Override
    public boolean applyGlobally() {
        return false;
    }

    private static boolean isXmlFile(String fileName) {
        //TODO: Mejorar esto
        return fileName.endsWith(".xml");
    }

    private static boolean isJsonFile(String fileName) {
        //TODO: Mejorar esto
        return fileName.endsWith(".json");
    }

    private ResponseDefinition transformXmlResponse(Request request, ResponseDefinition responseDefinition, FileSource files, JSONArray parameters) throws Exception {
        String xmlString = parseFileToString(files.getBinaryFileNamed(responseDefinition.getBodyFileName()));
        Document xmlDocument = parseStringToDocument(xmlString);

        parameters.forEach(item -> {
            JSONObject element = (JSONObject) item;
            try {
                String newValue = getNewValue(request, element);
                updateMatchingNodesLocatedByXpath(xmlDocument, element.getString("xpath"), newValue);
            } catch (Exception e) {
                System.out.println("FAIL");
                System.out.println(e.getMessage());
                System.out.println(e.getStackTrace());
            }
        });

        String newBodyResponse = parseDocumentToString(xmlDocument);
        return ResponseDefinitionBuilder
                .like(responseDefinition)
                .but()
                .withBodyFile(null)
                .withBody(newBodyResponse)
                .build();
    }

    private JSONArray xmlParameters(Object parameters) {
        return parseObjectToJsonArray(parameters, "transformXmlNode");
    }

    private JSONArray jsonParameters(Object parameters) {
        return parseObjectToJsonArray(parameters, "transformJsonNode");
    }

    private JSONArray parseObjectToJsonArray(Object parameters, String paramKey) {
        Parameters allParameters = Parameters.of(parameters);
        Object xmlParameters = allParameters.getOrDefault(paramKey, null);
        JSONArray formattedXmlParameters = new JSONArray();

        if(xmlParameters != null) {
            String jsonString = xmlParameters.toString()
                    .replaceAll("(?! )(?<=[=\\[\\]{}, ])([^\\[\\]{}=,]+?)(?=[=\\[{}\\],])", "\"$1\"")
                    .replaceAll("=", ":");
            formattedXmlParameters = new JSONArray(jsonString);
        }

        return formattedXmlParameters;
    }

    private ResponseDefinition transformJsonResponse(Request request, ResponseDefinition responseDefinition, FileSource files, JSONArray parameters) {
        //TODO: Implementar
        return ResponseDefinitionBuilder
                .like(responseDefinition)
                .but()
                .withBody("THIS SHOULD BE A JSON")
                .build();
    }

    private static String parseFileToString(BinaryFile file) {
        byte[] fileBytes = file.readContents();

        return new String(fileBytes, UTF_8);
    }

    private static Document parseStringToDocument(String xmlString) throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(xmlString)));
    }

    private static String parseDocumentToString(Document document) throws Exception {
        Transformer transformer = TransformerFactory.newInstance()
                .newTransformer();

        Source source = new DOMSource(document);
        StringWriter writer = new StringWriter();
        Result destination = new StreamResult(writer);

        transformer.transform(source, destination);

        return writer.toString();
    }

    private static void updateMatchingNodesLocatedByXpath(Document document, String xpathExpression, String newValue) throws Exception{
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList matchingNodes = (NodeList) xpath.compile(xpathExpression)
                .evaluate(document, XPathConstants.NODESET);

        for(int i = 0; i < matchingNodes.getLength(); i++) {
            matchingNodes.item(i).setNodeValue(newValue);
        }
    }

    private static void updateMatchingNodesLocatedByNodeName(Document document, String nodeName, String newValue) throws Exception{
        updateMatchingNodesLocatedByXpath(document, "//" + nodeName, newValue);
    }

    private static String getNewValue(Request request, JSONObject parameters) throws Exception{
        System.out.println("PARAMETERS: " + parameters.toString());
        if(parameters.has(DSValueType.NEW.getKey())) {
            return parameters.getString(DSValueType.NEW.getKey());
        }
        if(parameters.has(DSValueType.RANDOM.getKey())) {
            return getRandomValue(parameters.getString(DSValueType.RANDOM.getKey()));
        }
        if(parameters.has(DSValueType.RANDOM.getKey())) {
            return getRandomValue(parameters.getString(DSValueType.RANDOM.getKey()));
        }
        if(parameters.has(DSValueType.FROM_QUERY_STRING.getKey())) {
            return getFromQuery(request.getUrl(), parameters.getString(DSValueType.FROM_QUERY_STRING.getKey()));
        }
        if(parameters.has(DSValueType.FROM_BODY_JSON.getKey())) {
            return getFromJsonBody(request.getBodyAsString(), parameters.getString(DSValueType.FROM_BODY_JSON.getKey()));
        }
        if(parameters.has(DSValueType.FROM_BODY_JSONARRAY.getKey())) {
            return getFromJsonArray(request.getBodyAsString(), parameters.getString(DSValueType.FROM_BODY_JSONARRAY.getKey()));
        }
        if(parameters.has(DSValueType.FROM_BODY_XML.getKey())) {
            return getFromXml(request.getBodyAsString(), parameters.getString(DSValueType.FROM_BODY_XML.getKey()));
        }

        throw new Exception("No value found");
    }

    private static String getRandomValue(String randomType) {
        //TODO: Ampliar implementacion
        return String.valueOf(System.currentTimeMillis());
    }

    private static String getFromQuery(String requestURL, String queryParameter) throws Exception{
        Map<String, String> queryParameters;
        queryParameters = DSUtils.splitQuery(requestURL);
        return queryParameters.get(queryParameter);
    }

    private static String getFromJsonBody(String requestBody, String jsonPath) throws Exception{
        //TODO: Implementar
        return "";
    }

    private static String getFromJsonArray(String requestBody, String jsonPath) throws Exception{
        //TODO: Implementar
        return "";
    }

    private static String getFromXml(String requestBody, String jsonPath) throws Exception{
        //TODO: Implementar
        return "";
    }
}
