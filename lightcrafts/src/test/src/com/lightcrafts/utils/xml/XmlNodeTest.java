package com.lightcrafts.utils.xml;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Masahiro Kitagawa on 16/10/09.
 */
public class XmlNodeTest {
    private XmlNode xmlNode;

    @BeforeEach
    public void setUp() throws Exception {
        val resource = XmlNodeTest.class.getClassLoader().getResource("test.lzt");
        assertThat(resource).isNotNull();

        val filename = resource.getPath();
        val factory = DocumentBuilderFactory.newInstance();
        val docBuilder = factory.newDocumentBuilder();
        val doc = docBuilder.parse(filename);

        Element elem = doc.getDocumentElement();
        xmlNode = new XmlNode(elem);
    }

    @Test
    public void getVersion() {
        assertThat(xmlNode.getVersion()).isEqualTo(7);
    }

    @Test
    public void getAttributes() {
        val attrs = xmlNode.getAttributes();
        assertThat(attrs.length).isEqualTo(1);
        assertThat(attrs[0]).isEqualTo("version");
    }

    @Test
    public void clearData() {
        xmlNode.clearData();
        // TODO:
    }

}