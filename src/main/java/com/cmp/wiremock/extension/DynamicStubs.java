package com.cmp.wiremock.extension;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.BinaryFile;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import org.json.JSONArray;
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
        ResponseDefinition newResponse = responseDefinition;
        String bodyFileName = responseDefinition.getBodyFileName();
        Object dynamicStubsParameters = parameters.getOrDefault("dynamicStubsParameters", null);

        try {
            if(bodyFileName != null && dynamicStubsParameters!= null) {
                System.out.println("PARAMETERS: " + dynamicStubsParameters.toString());
                if(isXmlFile(bodyFileName)) {
                    JSONArray xmlParams = xmlParameters(dynamicStubsParameters);
                    newResponse = transformXmlResponse(request, responseDefinition, files, parameters);
                }
                if (isJsonFile(bodyFileName)) {
                    newResponse = transformJsonResponse(request, responseDefinition, files, parameters);
                }
            }
        } catch(Exception e) {
            System.out.println("Unable to transform Response");
        }

        return newResponse;
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    private static boolean isXmlFile(String fileName) {
        //TODO: Mejorar esto
        return fileName.endsWith(".xml");
    }

    private static boolean isJsonFile(String fileName) {
        //TODO: Mejorar esto
        return fileName.endsWith(".json");
    }

    private ResponseDefinition transformXmlResponse(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) throws Exception {
        String xmlString = parseFileToString(files.getBinaryFileNamed(responseDefinition.getBodyFileName()));
        Document xmlDocument = parseStringToDocument(xmlString);
        updateMatchingNodesLocatedByXpath(xmlDocument, "//Name/First/text()", "NEW NAME");

        String newBodyResponse = parseDocumentToString(xmlDocument);
        return ResponseDefinitionBuilder
                .like(responseDefinition)
                .but()
                .withBodyFile(null)
                .withBody(newBodyResponse)
                .build();
    }

    private JSONArray xmlParameters(Object parameters) {
        Parameters params = (Parameters)parameters;
        Object xmlParams = params.getOrDefault("transformXmlNode", null);

        if(xmlParams != null) {
            System.out.println("XML PARAMETERS: " + params.toString());
        }
        else {
            System.out.println("NUUUUUUUUUUUUUUUUUUUUUUULL");
        }

        return new JSONArray();
    }

    private ResponseDefinition transformJsonResponse(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
        //TODO: Implementar
        return ResponseDefinitionBuilder
                .like(responseDefinition)
                .but()
                .withBody("THIS SHOULD BE A JSON")
                .build();
    }

    private static String parseFileToString(BinaryFile file) {
        byte[] fileBytes = file.readContents();

        return new String(fileBytes, UTF_8);
    }

    private static Document parseStringToDocument(String xmlString) throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(xmlString)));
    }

    private static String parseDocumentToString(Document document) throws Exception {
        Transformer transformer = TransformerFactory.newInstance()
                .newTransformer();

        Source source = new DOMSource(document);
        StringWriter writer = new StringWriter();
        Result destination = new StreamResult(writer);

        transformer.transform(source, destination);

        return writer.toString();
    }

    private static void updateMatchingNodesLocatedByXpath(Document document, String xpathExpression, String newValue) throws Exception{
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList matchingNodes = (NodeList) xpath.compile(xpathExpression)
                .evaluate(document, XPathConstants.NODESET);

        for(int i = 0; i < matchingNodes.getLength(); i++) {
            matchingNodes.item(i).setNodeValue(newValue);
        }

    }
}
