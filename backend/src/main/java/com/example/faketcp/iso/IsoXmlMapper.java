package com.example.faketcp.iso;

import com.example.faketcp.dto.IsoMessageDto;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class IsoXmlMapper {
    private static final Pattern BITMAP_FIELD_PATTERN = Pattern.compile("\\d+");

    public IsoMessageDto parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new IllegalArgumentException("XML message must not be empty.");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Element root = document.getDocumentElement();
            if (root == null || !"isomsg".equals(root.getTagName())) {
                throw new IllegalArgumentException("Root element must be <isomsg>.");
            }

            IsoMessageDto message = new IsoMessageDto();
            String bitmapMacFieldHint = null;
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (!(node instanceof Element)) {
                    continue;
                }
                Element element = (Element) node;
                String tag = element.getTagName();
                String id = element.getAttribute("id");
                String value = element.getAttribute("value");
                if ("mti".equals(tag)) {
                    message.setMti(value);
                    continue;
                }
                if (!"field".equals(tag)) {
                    continue;
                }
                if ("bitmap".equalsIgnoreCase(id)) {
                    bitmapMacFieldHint = macFieldFromBitmap(value);
                    continue;
                }
                if ("0".equals(id) || "MTI".equalsIgnoreCase(id)) {
                    if (message.getMti() == null || message.getMti().trim().isEmpty()) {
                        message.setMti(value);
                    }
                    continue;
                }
                if (id != null && !id.trim().isEmpty()) {
                    message.getFields().put(IsoFieldReferences.canonical(id), value);
                }
            }
            message.setBitmapMacFieldHint(bitmapMacFieldHint);
            return message;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ISO XML message: " + e.getMessage(), e);
        }
    }

    public String render(IsoMessageDto message) {
        StringBuilder xml = new StringBuilder();
        xml.append("<isomsg>\n");
        if (message.getMti() != null) {
            xml.append("  <mti id=\"MTI\" value=\"").append(escape(message.getMti())).append("\"/>\n");
            xml.append("  <field id=\"0\" value=\"").append(escape(message.getMti())).append("\"/>\n");
        }
        message.getFields().forEach((key, value) -> xml.append("  <field id=\"")
                .append(escape(key))
                .append("\" value=\"")
                .append(escape(value))
                .append("\"/>\n"));
        xml.append("</isomsg>");
        return xml.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String macFieldFromBitmap(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        boolean has64 = false;
        boolean has128 = false;
        Matcher matcher = BITMAP_FIELD_PATTERN.matcher(value);
        while (matcher.find()) {
            int field = Integer.parseInt(matcher.group());
            if (field == 64) {
                has64 = true;
            } else if (field == 128) {
                has128 = true;
            }
        }
        if (has128) {
            return "128";
        }
        return has64 ? "64" : null;
    }
}
