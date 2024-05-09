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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class App {

    public static class SendInvoiceTask implements Callable<Void> {
        private final String xmlFilePath;
        private final String apiUrl;
        private final String prefix;
        private final String uuid;
        private final int index;

        public SendInvoiceTask(String xmlFilePath, String apiUrl, String prefix, String uuid, int index) {
            this.xmlFilePath = xmlFilePath;
            this.apiUrl = apiUrl;
            this.prefix = prefix;
            this.uuid = uuid;
            this.index = index;
        }

        @Override
        public Void call() throws Exception {
            String xmlContent = readFileToString(xmlFilePath);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

            incrementElement(doc, "cbc:ID", prefix, true, index);
            incrementElement(doc, "cbc:UUID", uuid, false, index);

            String updatedXML = documentToString(doc);

            writeStringToFile(xmlFilePath, updatedXML);

            Base64.Encoder encoder = Base64.getEncoder();
            String base64EncodedXML = encoder.encodeToString(updatedXML.getBytes());

            sendEncodedXml(base64EncodedXML, apiUrl);

            return null;
        }

        private void sendEncodedXml(String base64EncodedXml, String apiUrl) throws Exception {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(apiUrl);
                post.setHeader("Client-Id", "f7ca5099-00795858-4bf2-b3fb-4d644b10e216");
                post.setHeader("Secret-Key", "Gj5nS9wyYHRadaVffz5VKB4v4wlVWyPhcJvrTD4NHtPrq8WMlP6bXS9xTf2o15tyA23wkv50r4yreeJ4HE4JZb6LA//vRT07v9U4gCh96B/f4sfg3E7/ExxBJ/CwIO5Hz5WBXn6DDvuPH9jx5Is0Et3hGpMF2/l5Pcgi806pPRNx8MT5UX7GdW2Ecked13fQ3xSu3casn8jVEiPvrI+yhs4HrBOEV2wNf5h9o7l86Hmfe1nsReAkdYQNkO83VEBgbxfICDfwWnc+18sfHCreHQ==");
                post.setHeader("Cookie", "stickounet=347dabd37ee9add31f220ebf92bf6ff5|7480c8b0e4ce7933ee164081a50488f1");
                post.setHeader("Content-Type", "application/json");

                JsonObject json = new JsonObject();
                json.addProperty("invoice", base64EncodedXml);

                post.setEntity(new StringEntity(json.toString()));

                try (CloseableHttpResponse response = client.execute(post)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    System.out.println("Invoice " + index + ": Response Status: " + statusCode);
                    System.out.println("Invoice " + index + ": Response Body: " + responseBody);
                }
            }
        }

        private String readFileToString(String filePath) throws IOException {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            return content.toString();
        }

        private static void incrementElement(Document doc, String tagName, String prefix, boolean isNumeric, int index) {
            NodeList nodeList = doc.getElementsByTagName(tagName);
            Node node = nodeList.item(0);
            if (node != null) {
                String value = node.getTextContent();
                if (isNumeric) {
                    String numberPart = value.replace(prefix, "");
                    int newNumber = Integer.parseInt(numberPart) + index;
                    node.setTextContent(prefix + newNumber);
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

    public static void main(String[] args) {
        String filePath = "D:\\E-invoicingXmlGenerator\\src\\invoice.xml";
        String apiUrl = "http://localhost:8083/core/invoices/";
        int numberOfInvoices = 50000;
        int numberOfThreads = 50;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 1; i <= numberOfInvoices; i++) {
            SendInvoiceTask task = new SendInvoiceTask(filePath, apiUrl, "testApiLogs_", "7a4452eb-0acd-49983-b630-b2b79d0a", i);
            futures.add(executor.submit(task));
        }

        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        System.out.println("All invoices have been sent.");
    }
}
