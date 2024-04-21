package org.example;
import com.google.gson.JsonObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Base64;

public class App {
    public static void sendEncodedXml(String base64EncodedXml, String apiUrl) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(apiUrl);
            post.setHeader("Client-Id", "enter the client id ");
            post.setHeader("Secret-Key", "enter the secret-key");
            post.setHeader("Cookie", "stickounet=347dabd37ee9add31f220ebf92bf6ff5|7480c8b0e4ce7933ee164081a50488f1");
            post.setHeader("Content-Type", "application/json");

            JsonObject json = new JsonObject();
            json.addProperty("invoice", base64EncodedXml);

            post.setEntity(new StringEntity(json.toString()));

            try (CloseableHttpResponse response = client.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                System.out.println("Response Status: " + statusCode);
                System.out.println("Response Body: " + responseBody);
            }
        }
    }

    public static void main(String[] args) {
        String filePath = "D:\\E-invoicingXmlGenerator\\src\\invoice.xml";
        String apiUrl = "api-url";
        for (int i = 0; i < 1000; i++) {
            try {
                String xmlContent = readFileToString(filePath);

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

                incrementElement(doc, "cbc:ID", "testApiLogs_", true);
                incrementElement(doc, "cbc:UUID", "7a4452eb-0acd-49983-b630-b2b79d0a", false);

                String updatedXML = documentToString(doc);

                writeStringToFile(filePath, updatedXML);

                Base64.Encoder encoder = Base64.getEncoder();
                String base64EncodedXML = encoder.encodeToString(updatedXML.getBytes());

                sendEncodedXml(base64EncodedXML, apiUrl);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String readFileToString(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private static void incrementElement(Document doc, String tagName, String prefix, boolean isNumeric) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        Node node = nodeList.item(0);
        if (node != null) {
            String value = node.getTextContent();
            if (isNumeric) {
                String numberPart = value.replace(prefix, "");
                try {
                    int newNumber = Integer.parseInt(numberPart) + 1;
                    node.setTextContent(prefix + newNumber);
                } catch (NumberFormatException e) {
                    System.err.println("Error: Unable to increment the ID");
                }
            }
        }
    }

    private static String documentToString(Document doc) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(outputStream));
        return outputStream.toString();
    }

    private static void writeStringToFile(String filePath, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        }
    }
}
