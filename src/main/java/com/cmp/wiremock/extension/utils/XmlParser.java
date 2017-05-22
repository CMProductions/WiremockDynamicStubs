package com.cmp.wiremock.extension.utils;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Created by lunabulnes
 */
public class XmlParser {

    private Object xmlObject;

    private XmlParser(Object xmlObject) {
        this.xmlObject = xmlObject;
    }

    public static XmlParser aXmlObjectFromString(String xmlObject) {
        return new XmlParser(xmlObject);
    }

    public static XmlParser aXmlObjectFromBinaryFile(byte[] xmlObject) {
        return new XmlParser(xmlObject);
    }

    public XmlParser parseBinaryFileToString() {
        this.xmlObject = new String((byte[])this.xmlObject, UTF_8);
        return this;
    }

    public XmlParser parseStringToDocument() throws Exception{
        this.xmlObject = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader((String)this.xmlObject)));
        return this;
    }

    public String getAsString() {
        return (String) this.xmlObject;
    }

    public Document getAsDocument() {
        return (Document) this.xmlObject;
    }
}
