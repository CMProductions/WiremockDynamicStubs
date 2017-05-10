package com.cmp.wiremock.extension;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

/**
 * Created by lunabulnes
 */
public class DynamicStubs extends ResponseDefinitionTransformer {

    public String getName() {
        return "DynamicStubs";
    }

    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
        System.out.println("REQUEST URL: " + request.getAbsoluteUrl());
        System.out.println("REQUEST BODY: " + request.getBodyAsString());
        System.out.println("PARAMETERS: " + parameters.getString("DynamicStubsParameters"));
        System.out.println("RESPONSE DEFINITION: " + responseDefinition.getBody());
        return responseDefinition;
    }
/*
    @Override
    public boolean applyGlobally() {
        return false;
    }*/
}
