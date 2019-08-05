package com.lightcrafts.utils.xml;

import lombok.val;
import org.junit.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by Masahiro Kitagawa on 16/10/09.
 */
public class XmlNodeTest {
    private XmlNode xmlNode;

    @org.junit.Before
    public void setUp() throws Exception {
        val resource = XmlNodeTest.class.getClassLoader().getResource("test.lzt");
        if (resource == null) {
            fail();
        }
        val filename = resource.getPath();

        val factory = DocumentBuilderFactory.newInstance();
        val docBuilder = factory.newDocumentBuilder();
        val doc = docBuilder.parse(filename);

        Element elem = doc.getDocumentElement();
        xmlNode = new XmlNode(elem);
    }

    @Test
    public void getVersion() throws Exception {
        assertThat(xmlNode.getVersion(), is(7));
    }

    @Test
    public void getAttributes() throws Exception {
        val attrs = xmlNode.getAttributes();
        assertThat(attrs.length, is(1));
        assertThat(attrs[0], is("version"));
    }

    @org.junit.Test
    public void clearData() throws Exception {
        xmlNode.clearData();
        // TODO:
    }

}