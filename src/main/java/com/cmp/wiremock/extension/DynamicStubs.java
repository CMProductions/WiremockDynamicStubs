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

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
        System.out.println("TRANSFOOOOOOOOORM");
        System.out.println("TRANSFOOOOOOOOORM");
        System.out.println("TRANSFOOOOOOOOORM");
        System.out.println("REQUEST: " + request.getUrl());
        System.out.println("REQUEST: " + request.getBodyAsString());
        System.out.println("RESPONSE: " + responseDefinition.getBody());
        return new ResponseDefinition(200, "<response>RESPONSE MODIFIED!!</response>");
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }
}
