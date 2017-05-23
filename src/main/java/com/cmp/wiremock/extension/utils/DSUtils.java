package com.cmp.wiremock.extension.utils;

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
}
