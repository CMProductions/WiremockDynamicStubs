package com.cmp.wiremock.extension.utils;

import com.github.tomakehurst.wiremock.common.BinaryFile;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Created by lunabulnes
 */
public class XmlParser {

    private Object xmlObject;

    private XmlParser(Object xmlObject) {
        this.xmlObject = xmlObject;
    }

    public static XmlParser aXmlObjectFromBinaryFile(BinaryFile xmlObject) {
        return new XmlParser(xmlObject);
    }

    public static XmlParser aXmlObjectFromByteArray(byte[] xmlObject) {
        return new XmlParser(xmlObject);
    }

    public static XmlParser aXmlObjectFromString(String xmlObject) {
        return new XmlParser(xmlObject);
    }

    public static XmlParser aXmlObjectFromDocument(Document xmlObject) {
        return new XmlParser(xmlObject);
    }

    public XmlParser parseBinaryToByteArray() {
        this.xmlObject = ((BinaryFile)this.xmlObject).readContents();
        return this;
    }

    public XmlParser parseByteArrayToString() {
        this.xmlObject = new String((byte[])this.xmlObject, UTF_8);
        return this;
    }

    public XmlParser parseBinaryFileToString() {
        return this.parseBinaryToByteArray().parseByteArrayToString();
    }

    public XmlParser parseStringToDocument() throws Exception{
        this.xmlObject = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader((String)this.xmlObject)));
        return this;
    }

    public XmlParser parseDocumentToString() throws Exception{
        Transformer transformer = TransformerFactory.newInstance()
                .newTransformer();

        Source source = new DOMSource((Document)this.xmlObject);
        StringWriter writer = new StringWriter();
        Result destination = new StreamResult(writer);

        transformer.transform(source, destination);

        this.xmlObject = writer.toString();
        return this;
    }

    public String getAsString() {
        return (String) this.xmlObject;
    }

    public Document getAsDocument() {
        return (Document) this.xmlObject;
    }
}
