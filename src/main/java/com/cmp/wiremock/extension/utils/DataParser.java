package com.cmp.wiremock.extension.utils;

import com.github.tomakehurst.wiremock.common.BinaryFile;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Created by lunabulnes
 */
public class DataParser {

    private String objectAsString;

    private DataParser(String object) {
        this.objectAsString = object;
    }

    public static DataParser from(BinaryFile binaryFile) {
        return from(binaryFile.readContents());
    }

    public static DataParser from(byte[] byteArray) {
        String parsedFile = new String(byteArray, UTF_8);
        return new DataParser(parsedFile);
    }

    public static DataParser from(String string) {
        return new DataParser(string);
    }

    public static DataParser from(Document document) throws Exception {
        Source source = new DOMSource(document);
        StringWriter writer = new StringWriter();
        Result destination = new StreamResult(writer);

        TransformerFactory.newInstance()
                .newTransformer()
                .transform(source, destination);

        String parsedDocument = writer.toString();
        return new DataParser(parsedDocument);
    }

    public static DataParser from(DocumentContext documentContext) throws Exception {
        return new DataParser(documentContext.jsonString());
    }

    @Override
    public String toString() {
        return objectAsString;
    }

    public Document toDocument() throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(objectAsString)));
    }

    public DocumentContext toDocumentContext() throws Exception {
        return JsonPath.parse(objectAsString);
    }

    public byte[] toByteArray() throws Exception {
        return objectAsString.getBytes();
    }

    public RequestMethod toRequestMethod() throws Exception {
        switch (objectAsString) {
            case "GET":
                return RequestMethod.GET;
            case "POST" :
                return RequestMethod.POST;
            case "PUT" :
                return RequestMethod.PUT;
            case "DELETE" :
                return RequestMethod.DELETE;
            case "PATCH" :
                return RequestMethod.PATCH;
            case "OPTIONS" :
                return RequestMethod.OPTIONS;
            case "HEAD" :
                return RequestMethod.HEAD;
            case "TRACE" :
                return RequestMethod.TRACE;
            case "ANY" :
                return RequestMethod.ANY;
            default:
                throw new Exception("Http method not existing");
        }
    }
}
