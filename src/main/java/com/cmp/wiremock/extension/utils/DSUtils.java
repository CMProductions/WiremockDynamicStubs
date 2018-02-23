package com.cmp.wiremock.extension.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.extension.Parameters;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by lunabulnes
 */
public class DSUtils {

    public static Map<String, String> splitUrl(String url) throws Exception {
        Map<String, String> queryMap = new LinkedHashMap<String, String>();

        int queryIndex = url.indexOf('?');
        String urlEncodedParams = url.substring(queryIndex + 1);

        return splitQuery(urlEncodedParams);
    }

    public static Map<String, String> splitQuery(String urlEncodedParams) throws Exception {
        Map<String, String> queryMap = new LinkedHashMap<String, String>();

        String[] pairs = urlEncodedParams
                .split("&");

        for (String pair : pairs) {
            int splitIndex = pair.indexOf("=");
            queryMap.put(URLDecoder.decode(pair.substring(0, splitIndex), DataParser.defaultCharset), URLDecoder.decode(pair.substring(splitIndex + 1), DataParser.defaultCharset));
        }
        return queryMap;
    }

    public static JSONArray parseWiremockParametersToJsonArray(Parameters parameters, String paramKey) {
        Object specificParameters = parameters.getOrDefault(paramKey, null);
        String jsonString = "";

        if(specificParameters != null) {
            try {
                jsonString = new ObjectMapper().writeValueAsString(specificParameters);
            } catch (Exception ex) {
                System.err.println("Error trying to parse parameters fo JSONArray: " + ex.getMessage());
            }
        }
        return new JSONArray(jsonString);
    }

    public static JSONObject parseWiremockParametersToJsonObject(Parameters parameters) {
        String jsonString = "";
        try {
            jsonString = new ObjectMapper().writeValueAsString(parameters);
        } catch (Exception ex) {
            System.err.println("Error trying to parse parameters fo JSONObject: " + ex.getMessage());
        }
        return new JSONObject(jsonString);
    }
}
