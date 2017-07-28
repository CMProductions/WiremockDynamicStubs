package com.cmp.wiremock.extension.enums;

/**
 * Created by lunabulnes
 */
public enum DSParamType {

    POSTBACK_PARAMS("doPostbacks"),
    KEY_PARAMS("key"),
    VALUE_PARAMS("value"),
    DOMAIN_PARAMS("domain"),
    PATH_PARAMS("path"),
    EXPIRY_PARAMS("expiry"),

    //Template type params
    XML_PARAMS("transformXmlBody"),
    JSON_PARAMS("transformJsonBody"),
    PLAIN_TEXT_PARAMS("transformPlainTextBody"),

    //Set value params
    BY_XPATH("findByXpath"),
    BY_NODE_NAME("findByNodeName"),
    BY_JSON_PATH("findByJsonPath"),
    BY_JSON_KEY("findByJsonKey"),
    BY_REGEX("findByRegex"),
    WITH_URL("withUrl"),
    WITH_METHOD("withMethod"),
    WITH_HEADERS("withHeaders"),
    WITH_COOKIES("withCookies"),
    WITH_BODY("withBody"),
    WITH_BODY_PARAMETERS("withParameters"),
    WITH_BODY_RAWBODY("withRawBody"),

    //Get value params
    FROM_NEW("setFixedValue"),
    FROM_RANDOM("setRandomValue"),
    FROM_BODY_JSON_PATH("gatherFromBodyByJsonPath"),
    FROM_BODY_JSON_KEY("gatherFromBodyByJsonKey"),
    FROM_BODY_XML("gatherFromBodyByXpath"),
    FROM_BODY_PLAINTEXT("gatherFromBodyByRegex"),
    FROM_URL_QUERY_PARAMETER("gatherFromQueryParameter"),
    FROM_URL_ASPLAINTEXT("gatherFromUrlByRegex"),
    FROM_URL_BASEURL("gatherBaseUrl"),
    FROM_URL_PATH("gatherPath"),
    FROM_URL_QUERY_STRING("gatherQuery"),
    FROM_HEADER("gatherHeader"),
    FROM_COOKIE("gatherCookie"),
    COMPOUND_VALUE("compoundValue"),
    FROM_SAVED_RESPONSE("fromSavedResponse"),
    FROM_REQUEST("fromRequest"),
    FROM_RESPONSE("fromResponse"),
    SAVE_RESPONSE("saveResponseWithTag"),
    WITH_TAG("withTag"),
    DELETE_RESPONSE("deleteResponseWithTag");

    private String paramKey;

    DSParamType(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getKey() {
        return this.paramKey;
    }
}
