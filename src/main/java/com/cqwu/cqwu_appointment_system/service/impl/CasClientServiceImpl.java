package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.config.properties.CasProperties;
import com.cqwu.cqwu_appointment_system.domain.CasUserPrincipal;
import com.cqwu.cqwu_appointment_system.service.CasClientService;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Service
@Slf4j
public class CasClientServiceImpl implements CasClientService {

    @Autowired
    private CasProperties casProperties;

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Override
    public CasUserPrincipal validateServiceTicket(String ticket, String serviceUrl) {
        Assert.hasText(ticket, "CAS ticket 不能为空");
        Assert.hasText(serviceUrl, "CAS serviceUrl 不能为空");

        String validateUrl = UriComponentsBuilder
                .fromUriString(Objects.requireNonNull(casProperties.getServerUrlPrefix()))
                .path("/serviceValidate")
                .queryParam("ticket", ticket)
                .queryParam("service", serviceUrl)
                .build()
                .encode()
                .toUriString();

        String body = doGet(validateUrl);
        return parseCasValidationResponse(body);
    }

    private String doGet(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalArgumentException("CAS 校验失败，HTTP 状态码: " + response.statusCode());
            }
            return response.body();
        } catch (Exception e) {
            log.error("调用 CAS 校验接口失败: {}", e.getMessage(), e);
            throw new RuntimeException("调用 CAS 校验接口失败", e);
        }
    }

    private CasUserPrincipal parseCasValidationResponse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

            NodeList failures = document.getElementsByTagName("cas:authenticationFailure");
            if (failures.getLength() == 0) {
                failures = document.getElementsByTagName("authenticationFailure");
            }
            if (failures.getLength() > 0) {
                String error = failures.item(0).getTextContent();
                throw new IllegalArgumentException("CAS 认证失败: " + error);
            }

            String userId = getFirstText(document, "cas:user");
            if (userId == null) {
                userId = getFirstText(document, "user");
            }
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("CAS 返回中未包含 user 标识");
            }

            Map<String, String> attributes = extractAttributes(document);
            return new CasUserPrincipal(userId, attributes);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析 CAS 校验响应失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析 CAS 校验响应失败", e);
        }
    }

    private String getFirstText(Document document, String tagName) {
        NodeList nodeList = document.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) {
            return null;
        }
        String text = nodeList.item(0).getTextContent();
        return text == null ? null : text.trim();
    }

    private Map<String, String> extractAttributes(Document document) {
        Map<String, String> attributes = new HashMap<>();

        NodeList attrsNodes = document.getElementsByTagName("cas:attributes");
        if (attrsNodes.getLength() == 0) {
            attrsNodes = document.getElementsByTagName("attributes");
        }
        if (attrsNodes.getLength() == 0) {
            return attributes;
        }

        Node attrsNode = attrsNodes.item(0);
        NodeList children = attrsNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) child;
            String key = element.getTagName();
            int index = key.indexOf(':');
            if (index >= 0 && index < key.length() - 1) {
                key = key.substring(index + 1);
            }
            String value = element.getTextContent();
            attributes.put(key, value == null ? "" : value.trim());
        }
        return attributes;
    }
}
