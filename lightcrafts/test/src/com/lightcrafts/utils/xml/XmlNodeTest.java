package com.lightcrafts.utils.xml;

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
        final var resource = XmlNodeTest.class.getClassLoader().getResource("test.lzt");
        assertThat(resource).isNotNull();

        final var filename = resource.getPath();
        final var factory = DocumentBuilderFactory.newInstance();
        final var docBuilder = factory.newDocumentBuilder();
        final var doc = docBuilder.parse(filename);

        Element elem = doc.getDocumentElement();
        xmlNode = new XmlNode(elem);
    }

    @Test
    public void getVersion() {
        assertThat(xmlNode.getVersion()).isEqualTo(7);
    }

    @Test
    public void getAttributes() {
        final var attrs = xmlNode.getAttributes();
        assertThat(attrs.length).isEqualTo(1);
        assertThat(attrs[0]).isEqualTo("version");
    }

    @Test
    public void clearData() {
        xmlNode.clearData();
        // TODO:
    }

}
