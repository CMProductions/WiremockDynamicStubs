package com.cmp.wiremock.extension;

import com.cmp.wiremock.extension.enums.DSParamType;
import com.cmp.wiremock.extension.utils.DSUtils;
import com.cmp.wiremock.extension.utils.DataParser;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.Gzip;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.LoggedResponse;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.jayway.jsonpath.DocumentContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

import static com.cmp.wiremock.extension.utils.DataGatherer.*;

/**
 * Created by lunabulnes
 */
public class DynamicStubs extends ResponseTransformer {

    private static Map<String, Response> savedResponses =  new HashMap<>();
    private final static String EXTENSION_PARAMS_NAME = "dynamicStubsParameters";

    public String getName() {
        return "DynamicStubs";
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    @Override
    public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
        Response transformedResponse = response;
        Parameters dynamicStubsParameters = Parameters.of(parameters.getOrDefault(EXTENSION_PARAMS_NAME, null));
        String bodyTemplate = getBodyFromResponse(response);
        System.out.println("PARAMETERS: " + parameters.toString());
        try {
            if(!bodyTemplate.isEmpty() && !dynamicStubsParameters.isEmpty()) {
                if (dynamicStubsParameters.containsKey(DSParamType.PLAIN_TEXT_PARAMS.getKey())) {
                    System.out.println("PLAIN TEXT");
                    Parameters plainTextParameters = Parameters.of(dynamicStubsParameters.getOrDefault(DSParamType.PLAIN_TEXT_PARAMS.getKey(), null));
                    bodyTemplate = transformPlainTextTemplate(
                            request,
                            bodyTemplate,
                            plainTextParameters
                    );
                }
                if (dynamicStubsParameters.containsKey(DSParamType.XML_PARAMS.getKey())) {
                    System.out.println("XML");
                    Parameters xmlParameters = Parameters.of(dynamicStubsParameters.getOrDefault(DSParamType.XML_PARAMS.getKey(), null));
                    bodyTemplate = transformXmlTemplate(
                            request,
                            bodyTemplate,
                            xmlParameters
                    );
                }
                if (dynamicStubsParameters.containsKey(DSParamType.JSON_PARAMS.getKey())) {
                    System.out.println("JSON");
                    Parameters jsonParameters = Parameters.of(dynamicStubsParameters.getOrDefault(DSParamType.JSON_PARAMS.getKey(), null));
                    bodyTemplate = transformJsonTemplate(
                            request,
                            bodyTemplate,
                            jsonParameters
                    );
                }
                byte[] transformedBody = encodeBody(bodyTemplate, response);

                transformedResponse = Response.Builder
                        .like(response)
                        .but()
                        .body(transformedBody)
                        .build();

                if (dynamicStubsParameters.containsKey(DSParamType.SAVE_RESPONSE.getKey())) {
                    System.out.println("SAVE");
                    Parameters saveParams = Parameters.of(dynamicStubsParameters.get(DSParamType.SAVE_RESPONSE.getKey()));
                    String tag = getValue(LoggedResponse.from(transformedResponse), DSUtils.parseWiremockParametersToJsonObject(saveParams));
                    savedResponses.put(tag, transformedResponse);
                }
                if (dynamicStubsParameters.containsKey(DSParamType.DELETE_RESPONSE.getKey())) {
                    System.out.println("RETRIEVE");
                    Parameters deleteParams = Parameters.of(dynamicStubsParameters.get(DSParamType.DELETE_RESPONSE.getKey()));
                    String tag = getValue(request, DSUtils.parseWiremockParametersToJsonObject(deleteParams));
                    savedResponses.remove(tag);
                }

            }
        } catch(Exception e) {
            System.err.println("Unable to transform Response: " + e.getMessage());
            e.printStackTrace();
        }

        return transformedResponse;
    }

    private String getBodyFromResponse(Response response) {
        if(Gzip.isGzipped(response.getBody())) {
            return Gzip.unGzipToString(response.getBody());
        }
        return response.getBodyAsString();
    }

    private byte[] encodeBody(String bodyResponse, Response response) throws Exception {
        if(Gzip.isGzipped(response.getBody())) {
            return Gzip.gzip(bodyResponse);
        }
        return DataParser.from(bodyResponse).toByteArray();
    }

    private String transformXmlTemplate(Request request, String template, Parameters parameters) throws Exception {
        Document xmlTemplate = DataParser
                .from(template)
                .toDocument();

        if(parameters.containsKey(DSParamType.FROM_REQUEST.getKey())) {
            System.out.println("FROM REQUEST");
            JSONArray requestParams = DSUtils.parseWiremockParametersToJsonArray(parameters, DSParamType.FROM_REQUEST.getKey());
            transformXmlTemplateFromRequest(request, xmlTemplate, requestParams);
        }
        if(parameters.containsKey(DSParamType.FROM_SAVED_RESPONSE.getKey())) {
            System.out.println("FROM RESPONSE");
            JSONArray responseParams = DSUtils.parseWiremockParametersToJsonArray(parameters, DSParamType.FROM_SAVED_RESPONSE.getKey());
            Response response = retrieveSavedResponse(request, responseParams);
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
                System.out.println("SETTING VALUES");
                String newValue = getValue(request, parameter);
                updateXmlTemplateValues(xmlTemplate, parameter, newValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void transformXmlTemplateFromResponse(Response savedResponse, Document xmlTemplate, JSONArray parameters) throws Exception {
        parameters.forEach(item -> {
            JSONObject parameter = (JSONObject) item;
            if (!parameter.has(DSParamType.WITH_TAG.getKey())) {
                try {
                    System.out.println("SETTING VALUES");
                    String newValue = getValue(LoggedResponse.from(savedResponse), parameter);
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
            System.out.println("FROM REQUEST");
            JSONArray requestParams = DSUtils.parseWiremockParametersToJsonArray(parameters, DSParamType.FROM_REQUEST.getKey());
            transformJsonResponseFromRequest(request, jsonTemplate, requestParams);
        }
        if(parameters.containsKey(DSParamType.FROM_SAVED_RESPONSE.getKey())) {
            System.out.println("FROM RESPONSE");
            JSONArray responseParams = DSUtils.parseWiremockParametersToJsonArray(parameters, DSParamType.FROM_SAVED_RESPONSE.getKey());
            Response response = retrieveSavedResponse(request, responseParams);
            transformJsonTemplateFromResponse(response, jsonTemplate, responseParams);
        }

        return DataParser
                .from(jsonTemplate)
                .toString();
    }

    private void transformJsonResponseFromRequest(Request request, DocumentContext jsonTemplate, JSONArray parameters) throws Exception {
        for(int i = 0; i < parameters.length(); i++) {
            System.out.println("SETTING VALUES");
            String newValue = getValue(request, parameters.getJSONObject(i));
            updateJsonTemplateValues(
                    jsonTemplate,
                    parameters.getJSONObject(i),
                    newValue
            );
        }
    }

    private void transformJsonTemplateFromResponse(Response savedResponse, DocumentContext jsonTemplate, JSONArray parameters) throws Exception {
        for(int i = 0; i < parameters.length(); i++) {
            System.out.println("SETTING VALUES");
            String newValue = getValue(LoggedResponse.from(savedResponse), parameters.getJSONObject(i));
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
            System.out.println("FROM REQUEST");
            JSONArray requestParams = DSUtils.parseWiremockParametersToJsonArray(parameters, DSParamType.FROM_REQUEST.getKey());
            plainTextTemplate = transformPlainTextTemplateFromRequest(request, plainTextTemplate, requestParams);
        }
        if(parameters.containsKey(DSParamType.FROM_SAVED_RESPONSE.getKey())) {
            System.out.println("FROM RESPONSE");
            JSONArray responseParams = DSUtils.parseWiremockParametersToJsonArray(parameters, DSParamType.FROM_SAVED_RESPONSE.getKey());
            Response response = retrieveSavedResponse(request, responseParams);
            plainTextTemplate = transformPlainTextTemplateFromResponse(response, plainTextTemplate, responseParams);
        }

        return plainTextTemplate;
    }

    private String transformPlainTextTemplateFromRequest(Request request, String plainTextTemplate, JSONArray parameters) throws Exception {
        String transformedTemplate = plainTextTemplate;

        for(int i = 0; i < parameters.length(); i++) {
            String newValue = getValue(request, parameters.getJSONObject(i));
            transformedTemplate = updatePlainTextTemplateValues(plainTextTemplate, parameters.getJSONObject(i), newValue);
        }

        return transformedTemplate;
    }

    private String transformPlainTextTemplateFromResponse(Response savedResponse, String plainTextTemplate, JSONArray parameters) throws Exception {
        String transformedTemplate = plainTextTemplate;

        for(int i = 0; i < parameters.length(); i++) {
            String newValue = getValue(LoggedResponse.from(savedResponse), parameters.getJSONObject(i));
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

    private Response retrieveSavedResponse(Request request, JSONArray parameters) throws Exception {
        JSONObject tagParam = getFirstJsonObjectWithMatchingKey(parameters, DSParamType.WITH_TAG.getKey());
        String responseTag = getValue(request, tagParam);

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
