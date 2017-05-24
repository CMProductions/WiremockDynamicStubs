package com.cmp.wiremock.extension.utils;

import com.github.tomakehurst.wiremock.extension.Parameters;
import org.json.JSONArray;

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
            queryMap.put(URLDecoder.decode(pair.substring(0, splitIndex), "UTF-8"), URLDecoder.decode(pair.substring(splitIndex + 1), "UTF-8"));
        }
        return queryMap;
    }

    public static  JSONArray parseWiremockParametersToJsonArray(Object parameters, String paramKey) {
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
}
