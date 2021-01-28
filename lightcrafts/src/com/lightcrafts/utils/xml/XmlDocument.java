/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.xml;

import lombok.Getter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.Arrays;

/**
 *  This class provides restricted access to XML DOM trees.  The goal is to
 *  provide a simplified structure for internal use only.
 * <p>
 * This class excludes access to most of the structure of the XML standards,
 * especially character encodings, namespaces, text nodes, comments, entity
 * declarations, CDATA, processor instructions, and everything to do with
 * schemas.  It preserves the serialization capability though, exploiting the
 * readable character streams and built-in parsing of XML.
 * <p>
 * DOM structure is reduced to an elementary tree of XmlDocument.XmlNode
 * objects.  Each of these Nodes has a name (String), attributes (String
 * key-value pairs), and children (all Nodes).  Simple as could be.
 * <p>
 * Each instance represents one Document, which is always and only accessible
 * through its root XmlDocument.XmlNode.
 */

public class XmlDocument {

    private static DocumentBuilder Builder;
    private static TransformerFactory Transformers;

    static {
        try {
            final var dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Builder = dbf.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException("Couldn't init XML builder", e);
        }
        try {
            Transformers = TransformerFactory.newInstance();
        }
        catch (TransformerFactoryConfigurationError e) {
            throw new RuntimeException("Couldn't init XML transformer", e);
        }
        try {
            Transformers.setAttribute("indent-number", "2");
        }
        catch (IllegalArgumentException e) {
            // ignore, file will still be correct.
        }
    }

    private Document doc;
    private Transformer xform;

    @Getter
    private XmlNode root;

    /**
     * Create a new XmlDocument containing a root XmlNode with the given tag,
     * not in any namespace.
     */
    public XmlDocument(String name) {
        doc = createDocument();
        xform = createTransformer();
        Element e = doc.createElement(name);
        doc.appendChild(e);
        root = new XmlNode(e);
    }

    /**
     * Create a new XmlDocument containing a root XmlNode with the given tag,
     * in the given namespace.
     */
    public XmlDocument(String uri, String name) {
        this(name);
        root.setAttribute("xmlns", uri);
    }

    public XmlDocument(Document doc) {
        this.doc = doc;
        xform = createTransformer();
        root = new XmlNode(doc.getDocumentElement());
    }

    public XmlDocument(InputStream in) throws IOException {
        doc = createDocument();
        xform = createTransformer();
        read(in);
        Element e = doc.getDocumentElement();
        root = new XmlNode(e);
    }

    public XmlDocument(Element e) {
        e = (Element) e.cloneNode(true);
        doc = createDocument();
        e = (Element) doc.importNode(e, true);
        doc.appendChild(e);
        xform = createTransformer();
        root = new XmlNode(e);
    }

    public XmlDocument(XmlDocument xml) {
        doc = (Document) xml.doc.cloneNode(true);
        xform = createTransformer();
        Element e = doc.getDocumentElement();
        root = new XmlNode(e);
    }

    public void write(OutputStream out) throws IOException {
        DOMSource source = new DOMSource(doc);
        Writer writer = new OutputStreamWriter(out, "UTF-8");
        StreamResult result = new StreamResult(writer);
        try {
            xform.transform(source, result);
        }
        catch (TransformerException e) {
            throw new IOException("Couldn't write XML: " + e.getMessage());
        }
    }

    private void read(InputStream in) throws IOException {
        StreamSource source = new StreamSource(in);
        DOMResult result = new DOMResult(doc);
        Transformer xform = createTransformer();
        try {
            xform.transform(source, result);
        }
        catch (TransformerException e) {
            throw new IOException("Couldn't read XML: " + e.getMessage());
        }
    }

    private static Document createDocument() {
        return Builder.newDocument();
    }

    private static Transformer createTransformer() {
        Transformer xform = null;
        try {
            xform = Transformers.newTransformer();
        }
        catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        if (xform != null) {
            xform.setOutputProperty(OutputKeys.INDENT, "yes");
            xform.setOutputProperty(
                    "{http://xml.apache.org/xalan}indent-amount", "2"
            );
            xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        }
        return xform;
    }

    public static void main(String[] args) throws IOException {

        // Cook up some binary data to test the CDATA feature:
        InputStream in =
            XmlDocument.class.getResourceAsStream("XmlDocument.class");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int i;
        do {
            i = in.read();
            if (i >= 0) {
                bytes.write(i);
            }
        } while (i >= 0);
        byte[] inBytes = bytes.toByteArray();

        XmlDocument doc = new XmlDocument("namespace", "root");
        XmlNode root = doc.getRoot();
        XmlNode child = root.addChild("child");
        child.setAttribute("attribute", "value");
        child.setData(inBytes);

        doc.write(System.out);

        byte[] outBytes = child.getData();

        if (Arrays.equals(inBytes, outBytes)) {
            System.out.println("CDATA match");
        }
        else {
            System.out.println("CDATA differ");
        }
    }
}
