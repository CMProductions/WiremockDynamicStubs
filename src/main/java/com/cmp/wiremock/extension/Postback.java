package com.cmp.wiremock.extension;

import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

/**
 * Created by lunabulnes
 */
public class Postback extends PostServeAction {

    public String getName() {
        return "Postback";
    }

    @Override
    public void doAction(ServeEvent serveEvent, Admin admin, Parameters parameters) {
        System.out.println("IS THIS THING WORKING?");
        System.out.println("IS THIS THING WORKING?");
        System.out.println("IS THIS THING WORKING?");
        System.out.println("IS THIS THING WORKING?");
        System.out.println("IS THIS THING WORKING?");
        System.out.println("IS THIS THING WORKING?");
        System.out.println("IS THIS THING WORKING?");
        System.out.println("IS THIS THING WORKING?");
    }
}
