package com.cmp.wiremock.extension;

import com.cmp.wiremock.extension.utils.DSUtils;
import com.cmp.wiremock.extension.utils.DataGatherer;
import com.cmp.wiremock.extension.utils.DataParser;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.LoggedResponse;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
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

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import static com.cmp.wiremock.extension.enums.DSParamType.*;

/**
 * Created by lunabulnes
 */
public class Postbacks extends PostServeAction {

    private static final CloseableHttpClient httpClient = HttpClientFactory.createClient();

    private String postbackUrl = "";
    private RequestMethod postbackMethod = RequestMethod.GET;
    private List<HttpHeader> postbackHeaders = new ArrayList<>();
    private CookieStore postbackCookies = new BasicCookieStore();
    private List<NameValuePair> bodyParameters = new ArrayList<>();
    private String contentType = "";
    private String rawBody = "";

    private DataGatherer gatherer = new DataGatherer();

    public String getName() {
        return "Postbacks";
    }

    @Override
    public void doAction(ServeEvent serveEvent, Admin admin, Parameters parameters) {
        LoggedRequest servedRequest = serveEvent.getRequest();
        LoggedResponse servedResponse = serveEvent.getResponse();

        try {
            if(!parameters.isEmpty()) {
                JSONArray postbackParameters = DSUtils.parseWiremockParametersToJsonArray(parameters, POSTBACK_PARAMS.getKey());
                for (int i = 0; i < postbackParameters.length(); i++) {
                    resetPostbackConfig();
                    gatherData(servedRequest, postbackParameters.getJSONObject(i));
                    gatherData(servedResponse, postbackParameters.getJSONObject(i));

                    doPostback();
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

    private void gatherData(Object wiremockObject, JSONObject parameters) throws Exception {
        if (LoggedRequest.class.isInstance(wiremockObject) && parameters.has(FROM_REQUEST.getKey())) {
            JSONObject fromRequestParams = parameters.getJSONObject(FROM_REQUEST.getKey());
            gatherDataFromSpecificSource(wiremockObject, fromRequestParams);
        }
        if (LoggedResponse.class.isInstance(wiremockObject) && parameters.has(FROM_RESPONSE.getKey())) {
            JSONObject fromResponseParams = parameters.getJSONObject(FROM_RESPONSE.getKey());
            gatherDataFromSpecificSource(wiremockObject, fromResponseParams);
        }
    }

    private void gatherDataFromSpecificSource(Object wiremockObject, JSONObject parameters) throws Exception {
        if(parameters.has(WITH_URL.getKey())) {
            JSONObject urlParams = parameters.getJSONObject(WITH_URL.getKey());
            postbackUrl = gatherer.getValue(wiremockObject, urlParams);
        }
        if(parameters.has(WITH_METHOD.getKey())) {
            JSONObject methodParams = parameters.getJSONObject(WITH_METHOD.getKey());
            String method = gatherer.getValue(wiremockObject, methodParams);
            postbackMethod = DataParser.from(method).toRequestMethod();
        }
        if(parameters.has(WITH_HEADERS.getKey())) {
            JSONArray headerParams = parameters.getJSONArray(WITH_HEADERS.getKey());
            for (int i = 0; i < headerParams.length(); i++) {
                setHeader(wiremockObject, headerParams.getJSONObject(i));
            }
        }
        if(parameters.has(WITH_COOKIES.getKey())) {
            JSONArray cookieParams = parameters.getJSONArray(WITH_COOKIES.getKey());
            for (int i = 0; i < cookieParams.length(); i++) {
                setCookie(wiremockObject, cookieParams.getJSONObject(i));
            }
        }
        if(parameters.has(WITH_BODY.getKey())) {
            JSONObject bodyParams = parameters.getJSONObject(WITH_BODY.getKey());
            if(bodyParams.has(WITH_BODY_RAWBODY.getKey())) {
                rawBody = gatherer.getValue(wiremockObject, bodyParams.getJSONObject(WITH_BODY_RAWBODY.getKey()));
            }
            if(bodyParams.has(WITH_BODY_PARAMETERS.getKey())) {
                JSONArray bodyParameterParams = bodyParams.getJSONArray(WITH_BODY_PARAMETERS.getKey());
                for (int i = 0; i < bodyParameterParams.length(); i++) {
                    setParam(wiremockObject, bodyParameterParams.getJSONObject(i));
                }
            }
        }
        if(parameters.has(WITH_CONTENT_TYPE.getKey())) {
            JSONObject contentTypeParams = parameters.getJSONObject(WITH_CONTENT_TYPE.getKey());
            contentType = gatherer.getValue(wiremockObject, contentTypeParams);
        }
    }

    private void doPostback() throws Exception {
        if (!postbackUrl.isEmpty()) {
            try {
                HttpUriRequest postbackRequest = HttpClientFactory.getHttpRequestFor(postbackMethod, postbackUrl);
                postbackHeaders.forEach(header -> postbackRequest.setHeader(header.key(), header.firstValue()));
                HttpContext postbackContext = new BasicHttpContext();
                postbackContext.setAttribute(HttpClientContext.COOKIE_STORE, postbackCookies);

                if (!contentType.isEmpty()) {
                    postbackRequest.setHeader("Accept", contentType);
                    postbackRequest.setHeader("Content-type", contentType);
                    StringEntity stringParams = null;
                    if (!bodyParameters.isEmpty()) {
                        stringParams = DSUtils.parseHttpParamsToString(bodyParameters, contentType);
                    } else if (!rawBody.isEmpty()) {
                        stringParams = new StringEntity(rawBody, DataParser.defaultCharset);
                    } else {
                        System.err.println("content-type was specified but there is not data for the body");
                    }
                    ((HttpEntityEnclosingRequest) postbackRequest).setEntity(stringParams);
                }

                System.out.println(String.format("Sending %s to %s", postbackMethod.toString(), postbackUrl));
                System.out.println(String.format("With content type %s", contentType));
                httpClient.execute(postbackRequest, postbackContext);
            } catch (Exception e) {
                System.err.println("Error sending the postback: " + e.getMessage());
            }
        }
        else {
            System.err.println("URL is mandatory for sending a postback");
        }
    }

    private void setHeader(Object wiremockObject, JSONObject parameters) throws Exception {
        String key = "";
        String value = "";

        if(parameters.has(KEY_PARAMS.getKey())) {
            JSONObject keyParams = parameters.getJSONObject(KEY_PARAMS.getKey());
            key = gatherer.getValue(wiremockObject, keyParams);
        }
        if(parameters.has(VALUE_PARAMS.getKey())) {
            JSONObject valueParams = parameters.getJSONObject(VALUE_PARAMS.getKey());
            value = gatherer.getValue(wiremockObject, valueParams);
        }

        if(!key.isEmpty() && !value.isEmpty()) {
            HttpHeader header = new HttpHeader(key, value);
            postbackHeaders.add(header);
        }
        else {
            throw new Exception("Missing key or value for postback header");
        }
    }

    private void setCookie(Object wiremockObject, JSONObject parameters) throws Exception {
        String key = "";
        String value = "";
        String domain = "";
        String path = "";
        String expiry = "";

        if(parameters.has(KEY_PARAMS.getKey())) {
            JSONObject keyParams = parameters.getJSONObject(KEY_PARAMS.getKey());
            key = gatherer.getValue(wiremockObject, keyParams);
        }
        if(parameters.has(VALUE_PARAMS.getKey())) {
            JSONObject valueParams = parameters.getJSONObject(VALUE_PARAMS.getKey());
            value = gatherer.getValue(wiremockObject, valueParams);
        }
        if(parameters.has(DOMAIN_PARAMS.getKey())) {
            JSONObject domainParams = parameters.getJSONObject(DOMAIN_PARAMS.getKey());
            domain = gatherer.getValue(wiremockObject, domainParams);
        }
        if(parameters.has(PATH_PARAMS.getKey())) {
            JSONObject pathParams = parameters.getJSONObject(PATH_PARAMS.getKey());
            path = gatherer.getValue(wiremockObject, pathParams);
        }
        if(parameters.has(EXPIRY_PARAMS.getKey())) {
            JSONObject expiryParams = parameters.getJSONObject(EXPIRY_PARAMS.getKey());
            expiry = gatherer.getValue(wiremockObject, expiryParams);
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
                cookie.setExpiryDate(Date.valueOf(expiry));
            }
            postbackCookies.addCookie(cookie);
        }
        else {
            throw new Exception("Missing key or value for postback cookie");
        }
    }

    private void setParam(Object wiremockObject, JSONObject parameters) throws Exception {
        String key = "";
        String value = "";

        if(parameters.has(KEY_PARAMS.getKey())) {
            JSONObject keyParams = parameters.getJSONObject(KEY_PARAMS.getKey());
            key = gatherer.getValue(wiremockObject, keyParams);
        }
        if(parameters.has(VALUE_PARAMS.getKey())) {
            JSONObject valueParams = parameters.getJSONObject(VALUE_PARAMS.getKey());
            value = gatherer.getValue(wiremockObject, valueParams);
        }
        if(!key.isEmpty() && !value.isEmpty()) {
            NameValuePair parameter = new BasicNameValuePair(key, value);
            bodyParameters.add(parameter);
        }
        else {
            throw new Exception("Missing key or value for postback param");
        }
    }
}
