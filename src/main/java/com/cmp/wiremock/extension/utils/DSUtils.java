package com.cmp.wiremock.extension.utils;

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

    public static Map<String, String> splitQuery(String url) throws Exception {
        Map<String, String> queryMap = new LinkedHashMap<String, String>();

        int queryIndex = url.indexOf('?');
        String[] pairs = url.substring(queryIndex + 1)
                .split("&");

        for (String pair : pairs) {
            int splitIndex = pair.indexOf("=");
            queryMap.put(URLDecoder.decode(pair.substring(0, splitIndex), DataParser.defaultCharset), URLDecoder.decode(pair.substring(splitIndex + 1), DataParser.defaultCharset));
        }
        return queryMap;
    }

    public static JSONArray parseWiremockParametersToJsonArray(Parameters parameters, String paramKey) {
        Object specificParameters = parameters.getOrDefault(paramKey, null);
        JSONArray formattedParameters = new JSONArray();

        if(specificParameters != null) {
            String jsonString = specificParameters.toString()
                    .replaceAll("(?! )(?!\\[)(?!])(?<=[={}, ])([^{},]+?)(?=[{}=,])", "\"$1\"")
                    .replaceAll("=", ":");

            formattedParameters = new JSONArray(jsonString);
        }

        return formattedParameters;
    }

    public static JSONObject parseWiremockParametersToJsonObject(Parameters parameters) {
        String jsonString = "";
        if(parameters != null) {
            jsonString = parameters.toString()
                    .replaceAll("(?! )(?!\\[)(?!])(?<=[={}, ])([^{},]+?)(?=[{}=,])", "\"$1\"")
                    .replaceAll("=", ":");
        }

        return new JSONObject(jsonString);
    }
}
