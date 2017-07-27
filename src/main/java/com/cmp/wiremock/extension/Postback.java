package com.cmp.wiremock.extension;

import com.cmp.wiremock.extension.utils.DSUtils;
import com.cmp.wiremock.extension.utils.DataGatherer;
import com.cmp.wiremock.extension.utils.DataParser;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.cmp.wiremock.extension.enums.DSParamType.*;

/**
 * Created by lunabulnes
 */
public class Postback extends PostServeAction {

    private DataGatherer gatherer = new DataGatherer();

    private String postbackUrl = "";
    private RequestMethod postbackMethod = RequestMethod.GET;
    private List<HttpHeader> postbackHeaders = new ArrayList<>();
    private CookieStore postbackCookies = new BasicCookieStore();

    private Request servedRequest = null;
    private LoggedResponse servedResponse = null;
    private static final CloseableHttpClient httpClient = HttpClientFactory.createClient();

    public String getName() {
        return "Postback";
    }

    @Override
    public void doAction(ServeEvent serveEvent, Admin admin, Parameters parameters) {
        servedRequest = serveEvent.getRequest();
        servedResponse = serveEvent.getResponse();

        try {
            if(!parameters.isEmpty()) {
                JSONArray postbackParameters = DSUtils.parseWiremockParametersToJsonArray(parameters, POSTBACK_PARAMS.getKey());
                for (int i = 0; i < postbackParameters.length(); i++) {
                    postbackUrl = "";
                    postbackMethod = RequestMethod.GET;
                    postbackHeaders = new ArrayList<>();
                    postbackCookies = new BasicCookieStore();

                    doPostback(postbackParameters.getJSONObject(i));
                }
            }

        } catch (Exception e) {
            System.err.println("Unable to perfom postback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void doPostback(JSONObject parameters) throws Exception {
        gatherDataFromRequest(servedRequest, parameters);
        gatherDataFromResponse(servedResponse, parameters);

        if (!postbackUrl.isEmpty()) {
            try {
                HttpUriRequest postbackRequest = HttpClientFactory.getHttpRequestFor(postbackMethod, postbackUrl);
                postbackHeaders.forEach(header -> postbackRequest.addHeader(header.key(), header.firstValue()));

                HttpContext postbackContext = new BasicHttpContext();
                postbackContext.setAttribute(HttpClientContext.COOKIE_STORE, postbackCookies);

                httpClient.execute(postbackRequest, postbackContext);
            } catch (Exception e) {
                throw  new Exception("Error sending the postback");
            }
        }
        else {
            throw new Exception("URL is mandatory for sending a postback");
        }
    }

    private void gatherDataFromRequest(Request request, JSONObject parameters) throws Exception {
        if (parameters.has(FROM_REQUEST.getKey())) {
            JSONObject fromRequestParams = parameters.getJSONObject(FROM_REQUEST.getKey());

            if(fromRequestParams.has(WITH_URL.getKey())) {
                JSONObject urlParams = fromRequestParams.getJSONObject(WITH_URL.getKey());
                postbackUrl = gatherer.getValueFromRequest(request, urlParams);
            }
            if(fromRequestParams.has(WITH_METHOD.getKey())) {
                JSONObject methodParams = fromRequestParams.getJSONObject(WITH_METHOD.getKey());
                String method = gatherer.getValueFromRequest(request, methodParams);
                postbackMethod = DataParser.from(method).toRequestMethod();
            }
            if(fromRequestParams.has(WITH_HEADERS.getKey())) {
                JSONArray headerParams = fromRequestParams.getJSONArray(WITH_HEADERS.getKey());
                for (int i = 0; i < headerParams.length(); i++) {
                    setHeaderFromRequest(request, headerParams.getJSONObject(i));
                }
            }
            if(fromRequestParams.has(WITH_COOKIES.getKey())) {
                JSONArray cookieParams = fromRequestParams.getJSONArray(WITH_COOKIES.getKey());
                for (int i = 0; i < cookieParams.length(); i++) {
                    setCookieFromRequest(request, cookieParams.getJSONObject(i));
                }
            }
            if(fromRequestParams.has(WITH_BODY.getKey())) {
                //TODO: IMPLEMENT THAT
            }
        }
    }

    private void setHeaderFromRequest(Request request, JSONObject parameters) throws Exception {
        String key = "";
        String value = "";

        if(parameters.has(KEY_PARAMS.getKey())) {
            JSONObject keyParams = parameters.getJSONObject(KEY_PARAMS.getKey());
            key = gatherer.getValueFromRequest(request, keyParams);
        }
        if(parameters.has(VALUE_PARAMS.getKey())) {
            JSONObject valueParams = parameters.getJSONObject(VALUE_PARAMS.getKey());
            value = gatherer.getValueFromRequest(request, valueParams);
        }

        if(!key.isEmpty() && !value.isEmpty()) {
            HttpHeader header = new HttpHeader(key, value);
            postbackHeaders.add(header);
        }
        else {
            throw new Exception("Missing key or value for postback header");
        }
    }

    private void setCookieFromRequest(Request request, JSONObject parameters) throws Exception {
        String key = "";
        String value = "";

        if(parameters.has(KEY_PARAMS.getKey())) {
            JSONObject keyParams = parameters.getJSONObject(KEY_PARAMS.getKey());
            key = gatherer.getValueFromRequest(request, keyParams);
        }
        if(parameters.has(VALUE_PARAMS.getKey())) {
            JSONObject valueParams = parameters.getJSONObject(VALUE_PARAMS.getKey());
            value = gatherer.getValueFromRequest(request, valueParams);
        }

        if(!key.isEmpty() && !value.isEmpty()) {
            Cookie cookie = new BasicClientCookie(key, value);
            postbackCookies.addCookie(cookie);
        }
        else {
            throw new Exception("Missing key or value for postback cookie");
        }
    }

    private void gatherDataFromResponse(LoggedResponse response, JSONObject parameters) throws Exception  {
        if (parameters.has(FROM_REQUEST.getKey())) {
            JSONObject fromRequestParams = parameters.getJSONObject(FROM_REQUEST.getKey());

            if(fromRequestParams.has(WITH_URL.getKey())) {
                JSONObject urlParams = fromRequestParams.getJSONObject(WITH_URL.getKey());
                postbackUrl = gatherer.getValueFromResponse(response, urlParams);
            }
            if(fromRequestParams.has(WITH_METHOD.getKey())) {
                JSONObject methodParams = fromRequestParams.getJSONObject(WITH_METHOD.getKey());
                String method = gatherer.getValueFromResponse(response, methodParams);
                postbackMethod = DataParser.from(method).toRequestMethod();
            }
            if(fromRequestParams.has(WITH_HEADERS.getKey())) {
                JSONArray headerParams = fromRequestParams.getJSONArray(WITH_HEADERS.getKey());
                for (int i = 0; i < headerParams.length(); i++) {
                    setHeaderFromResponse(response, headerParams.getJSONObject(i));
                }
            }
            if(fromRequestParams.has(WITH_COOKIES.getKey())) {
                JSONArray cookieParams = fromRequestParams.getJSONArray(WITH_COOKIES.getKey());
                for (int i = 0; i < cookieParams.length(); i++) {
                    setCookieFromResponse(response, cookieParams.getJSONObject(i));
                }
            }
            if(fromRequestParams.has(WITH_BODY.getKey())) {
                //TODO: IMPLEMENT THAT
            }
        }
    }

    private void setHeaderFromResponse(LoggedResponse response, JSONObject parameters) throws Exception {
        String key = "";
        String value = "";

        if(parameters.has(KEY_PARAMS.getKey())) {
            JSONObject keyParams = parameters.getJSONObject(KEY_PARAMS.getKey());
            key = gatherer.getValueFromResponse(response, keyParams);
        }
        if(parameters.has(VALUE_PARAMS.getKey())) {
            JSONObject valueParams = parameters.getJSONObject(VALUE_PARAMS.getKey());
            value = gatherer.getValueFromResponse(response, valueParams);
        }

        if(!key.isEmpty() && !value.isEmpty()) {
            HttpHeader header = new HttpHeader(key, value);
            postbackHeaders.add(header);
        }
        else {
            throw new Exception("Missing key or value for postback header");
        }
    }

    private void setCookieFromResponse(LoggedResponse response, JSONObject parameters) throws Exception {
        String key = "";
        String value = "";

        if(parameters.has(KEY_PARAMS.getKey())) {
            JSONObject keyParams = parameters.getJSONObject(KEY_PARAMS.getKey());
            key = gatherer.getValueFromResponse(response, keyParams);
        }
        if(parameters.has(VALUE_PARAMS.getKey())) {
            JSONObject valueParams = parameters.getJSONObject(VALUE_PARAMS.getKey());
            value = gatherer.getValueFromResponse(response, valueParams);
        }

        if(!key.isEmpty() && !value.isEmpty()) {
            Cookie cookie = new BasicClientCookie(key, value);
            postbackCookies.addCookie(cookie);
        }
        else {
            throw new Exception("Missing key or value for postback cookie");
        }
    }
}
