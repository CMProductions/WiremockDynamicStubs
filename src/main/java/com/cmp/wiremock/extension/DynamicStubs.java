package com.cmp.wiremock.extension;

import com.github.tomakehurst.wiremock.common.BinaryFile;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;


import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.io.StringReader;
import java.io.StringWriter;

import static com.google.common.base.Charsets.UTF_8;

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
        System.out.println("REQUEST BODY: " + request.getBodyAsString());
        try {
            System.out.println("RESPONSE: " + responseDefinition.toString());
        } catch(Exception e) {
            System.out.println("No response to print");
        }
        try {
            System.out.println("RESPONSE BODY: " + responseDefinition.getBody());
        } catch(Exception e) {
            System.out.println("No response body to print");
        }
        try {
            /******** This parses given response from file to String  *********/
            BinaryFile bodyFile = files.getBinaryFileNamed(responseDefinition.getBodyFileName());
            byte[] bodyBytes = bodyFile.readContents();
            String bodyString = new String(bodyBytes, UTF_8);
            /******** This parses given response from file to String  *********/
            System.out.println("BINARY RESPONSE BODY: " + bodyString);

            Document xmlDocument = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(bodyString)));

            System.out.println("DOCUMENT: " + documentToString(xmlDocument));

            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList myNodeList = (NodeList) xpath.compile("//Name/First/text()")
                    .evaluate(xmlDocument, XPathConstants.NODESET);
            myNodeList.item(0).setNodeValue("NEW NAME");

            System.out.println("DOCUMENT: " + documentToString(xmlDocument));
        } catch(Exception e) {
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace());
        }
        try {
            System.out.println("FILENAME: " + responseDefinition.getBodyFileName());
        } catch(Exception e) {
            System.out.println("No filename to print");
        }
        try {
            System.out.println("PARAMETERS: " + parameters.toString());
        } catch(Exception e) {
            System.out.println("No parameters to print");
        }
        try {
            System.out.println("FILESOURCE: " + files.toString());
        } catch(Exception e) {
            System.out.println("No filesource to print");
        }
        try {
            System.out.println("FILE CONTENT: " + files.getTextFileNamed(responseDefinition.getBodyFileName()).toString());
        } catch(Exception e) {
            System.out.println("No file content to print");
        }
        return new ResponseDefinition(200, "<response>RESPONSE MODIFIED!!</response>");
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }


    private static String documentToString(Document document) {
        try {
            Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer();

            Source source = new DOMSource(document);
            StringWriter writer = new StringWriter();
            Result destination = new StreamResult(writer);

            transformer.transform(source, destination);

            return writer.toString();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace());
        }

        return "";
    }
}
