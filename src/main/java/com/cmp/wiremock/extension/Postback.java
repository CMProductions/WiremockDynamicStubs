package com.cmp.wiremock.extension;

import com.cmp.wiremock.extension.utils.DSUtils;
import com.cmp.wiremock.extension.utils.DataGatherer;
import com.cmp.wiremock.extension.utils.DataParser;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.cmp.wiremock.extension.enums.DSParamType.*;

/**
 * Created by lunabulnes
 */
public class Postback extends PostServeAction {

    private static final String defaultCharset = "UTF-8";
    private static final CloseableHttpClient httpClient = HttpClientFactory.createClient();

    private String postbackUrl = "";
    private RequestMethod postbackMethod = RequestMethod.GET;
    private List<HttpHeader> postbackHeaders = new ArrayList<>();
    private CookieStore postbackCookies = new BasicCookieStore();
    private ArrayList<NameValuePair> bodyParameters;
    private String rawBody = "";

    private DataGatherer gatherer = new DataGatherer();

    public String getName() {
        return "Postback";
    }

    @Override
    public void doAction(ServeEvent serveEvent, Admin admin, Parameters parameters) {
        Request servedRequest = serveEvent.getRequest();
        LoggedResponse servedResponse = serveEvent.getResponse();

        try {
            if(!parameters.isEmpty()) {
                JSONArray postbackParameters = DSUtils.parseWiremockParametersToJsonArray(parameters, POSTBACK_PARAMS.getKey());
                for (int i = 0; i < postbackParameters.length(); i++) {
                    resetPostbackConfig();
                    gatherDataFromRequest(servedRequest, postbackParameters.getJSONObject(i));
                    gatherDataFromResponse(servedResponse, postbackParameters.getJSONObject(i));

                    doPostback(postbackParameters.getJSONObject(i));
                }
            }

        } catch (Exception e) {
            System.err.println("Unable to perfom postback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void resetPostbackConfig() {
        postbackUrl = "";
        postbackMethod = RequestMethod.GET;
        postbackHeaders.clear();
        postbackCookies.clear();
        bodyParameters.clear();
        rawBody = "";
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
                JSONObject bodyParams = fromRequestParams.getJSONObject(WITH_BODY.getKey());
                if(bodyParams.has(WITH_BODY_RAWBODY.getKey())) {
                    rawBody = gatherer.getValueFromRequest(request, bodyParams.getJSONObject(WITH_BODY_RAWBODY.getKey()));
                }
                if(bodyParams.has(WITH_BODY_PARAMETERS.getKey())) {
                    JSONArray bodyParameterParams = bodyParams.getJSONArray(WITH_BODY_PARAMETERS.getKey());
                    for (int i = 0; i < bodyParameterParams.length(); i++) {
                        setParamFromRequest(request, bodyParameterParams.getJSONObject(i));
                    }
                }
            }
        }
    }

    private void gatherDataFromResponse(LoggedResponse response, JSONObject parameters) throws Exception  {
        if (parameters.has(FROM_RESPONSE.getKey())) {
            JSONObject fromRequestParams = parameters.getJSONObject(FROM_RESPONSE.getKey());

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
                JSONObject bodyParams = fromRequestParams.getJSONObject(WITH_BODY.getKey());
                if(bodyParams.has(WITH_BODY_RAWBODY.getKey())) {
                    rawBody = gatherer.getValueFromResponse(response, bodyParams.getJSONObject(WITH_BODY_RAWBODY.getKey()));
                }
                if(bodyParams.has(WITH_BODY_PARAMETERS.getKey())) {
                    JSONArray bodyParameterParams = bodyParams.getJSONArray(WITH_BODY_PARAMETERS.getKey());
                    for (int i = 0; i < bodyParameterParams.length(); i++) {
                        setParamFromResponse(response, bodyParameterParams.getJSONObject(i));
                    }
                }
            }
        }
    }

    private void doPostback(JSONObject parameters) throws Exception {
        if (!postbackUrl.isEmpty()) {
            try {
                HttpUriRequest postbackRequest = HttpClientFactory.getHttpRequestFor(postbackMethod, postbackUrl);
                postbackHeaders.forEach(header -> postbackRequest.addHeader(header.key(), header.firstValue()));
                HttpContext postbackContext = new BasicHttpContext();
                postbackContext.setAttribute(HttpClientContext.COOKIE_STORE, postbackCookies);

                if(postbackMethod != RequestMethod.GET && !bodyParameters.isEmpty()) {
                    ((HttpEntityEnclosingRequest) postbackRequest).setEntity(new UrlEncodedFormEntity(bodyParameters, defaultCharset));
                }
                else if(postbackMethod != RequestMethod.GET && !rawBody.isEmpty()) {
                    ((HttpEntityEnclosingRequest) postbackRequest).setEntity(new StringEntity(rawBody, defaultCharset));
                }

                httpClient.execute(postbackRequest, postbackContext);
            } catch (Exception e) {
                throw  new Exception("Error sending the postback: " + e.getMessage());
            }
        }
        else {
            throw new Exception("URL is mandatory for sending a postback");
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
        String domain = "";
        String path = "";
        //String expiry = "";

        if(parameters.has(KEY_PARAMS.getKey())) {
            JSONObject keyParams = parameters.getJSONObject(KEY_PARAMS.getKey());
            key = gatherer.getValueFromRequest(request, keyParams);
        }
        if(parameters.has(VALUE_PARAMS.getKey())) {
            JSONObject valueParams = parameters.getJSONObject(VALUE_PARAMS.getKey());
            value = gatherer.getValueFromRequest(request, valueParams);
        }
        if(parameters.has(DOMAIN_PARAMS.getKey())) {
            JSONObject domainParams = parameters.getJSONObject(DOMAIN_PARAMS.getKey());
            domain = gatherer.getValueFromRequest(request, domainParams);
        }
        if(parameters.has(PATH_PARAMS.getKey())) {
            JSONObject pathParams = parameters.getJSONObject(PATH_PARAMS.getKey());
            path = gatherer.getValueFromRequest(request, pathParams);
        }
        /*if(parameters.has(EXPIRY_PARAMS.getKey())) {
            JSONObject pathParams = parameters.getJSONObject(EXPIRY_PARAMS.getKey());
            expiry = gatherer.getValueFromRequest(request, pathParams);
        }*/
        if(!key.isEmpty() && !value.isEmpty()) {
            BasicClientCookie cookie = new BasicClientCookie(key, value);
            if (!domain.isEmpty()) {
                System.out.println("DOMAIN");
                cookie.setDomain(domain);
            }
            if (!path.isEmpty()) {
                System.out.println("PATH");
                cookie.setPath(path);
            }
            /*if (!expiry.isEmpty()) {
                cookie.setExpiryDate(new Date());
            }*/
            postbackCookies.addCookie(cookie);
        }
        else {
            throw new Exception("Missing key or value for postback cookie");
        }
    }

    private void setParamFromRequest(Request request, JSONObject parameters) throws Exception {
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
            NameValuePair parameter = new BasicNameValuePair(key, value);
            bodyParameters.add(parameter);
        }
        else {
            throw new Exception("Missing key or value for postback cookie");
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
        String domain = "";
        String path = "";
        String expiry = "";

        if(parameters.has(KEY_PARAMS.getKey())) {
            JSONObject keyParams = parameters.getJSONObject(KEY_PARAMS.getKey());
            key = gatherer.getValueFromResponse(response, keyParams);
        }
        if(parameters.has(VALUE_PARAMS.getKey())) {
            JSONObject valueParams = parameters.getJSONObject(VALUE_PARAMS.getKey());
            value = gatherer.getValueFromResponse(response, valueParams);
        }
        if(parameters.has(DOMAIN_PARAMS.getKey())) {
            JSONObject domainParams = parameters.getJSONObject(DOMAIN_PARAMS.getKey());
            domain = gatherer.getValueFromResponse(response, domainParams);
        }
        if(parameters.has(PATH_PARAMS.getKey())) {
            JSONObject pathParams = parameters.getJSONObject(PATH_PARAMS.getKey());
            path = gatherer.getValueFromResponse(response, pathParams);
        }
        if(parameters.has(EXPIRY_PARAMS.getKey())) {
            JSONObject pathParams = parameters.getJSONObject(EXPIRY_PARAMS.getKey());
            expiry = gatherer.getValueFromResponse(response, pathParams);
        }

        if(!key.isEmpty() && !value.isEmpty()) {
            BasicClientCookie cookie = new BasicClientCookie(key, value);
            if (!domain.isEmpty()) {
                cookie.setDomain(domain);
            }
            if (!path.isEmpty()) {
                cookie.setPath(path);
            }
            if (!expiry.isEmpty()) {
                cookie.setExpiryDate(new Date());
            }
            postbackCookies.addCookie(cookie);
        }
        else {
            throw new Exception("Missing key or value for postback cookie");
        }
    }

    private void setParamFromResponse(LoggedResponse response, JSONObject parameters) throws Exception {
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
            NameValuePair parameter = new BasicNameValuePair(key, value);
            bodyParameters.add(parameter);
        }
        else {
            throw new Exception("Missing key or value for postback parameter");
        }
    }
}
