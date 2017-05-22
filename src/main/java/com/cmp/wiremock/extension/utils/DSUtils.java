package com.cmp.wiremock.extension.utils;

import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by lunabulnes
 */
public class DSUtils {
    public static Map<String, String> splitQuery(String url) throws Exception {
        Map<String, String> queryPairs = new LinkedHashMap<String, String>();

        int queryIndex = url.indexOf('?');
        String queryString = url.substring(queryIndex + 1);
        String[] pairs = queryString.split("&");

        for (String pair : pairs) {
            int splitIndex = pair.indexOf("=");
            queryPairs.put(URLDecoder.decode(pair.substring(0, splitIndex), "UTF-8"), URLDecoder.decode(pair.substring(splitIndex + 1), "UTF-8"));
        }
        return queryPairs;
    }
}
