/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

/**
 *
 * @author glazen
 */
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.util.Vector;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class WriteXMLFile_GraphBW_TCPwindows {

    private String byteCount = null;
    private String startTime = null;
    private String endTime = null;
    //DataMeasurement Measurement = null;

    public WriteXMLFile_GraphBW_TCPwindows(String name, Vector<Double> BWVector, Vector<Integer> TCPwindows) {

        try {
            //this.Measurement = _Measurement;
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("Graph_BW_TCPWindows");
            doc.appendChild(rootElement);

            //All BW vectors have the same size
            for (int i = 0; i < BWVector.size(); i++) {
                String BW = String.valueOf(BWVector.get(i));
                rootElement.appendChild(getSample(doc, "BWVector", String.valueOf(i), "BWVector", BW));
            }
            for (int i = 0; i < TCPwindows.size(); i++) {
                String Window = String.valueOf(TCPwindows.get(i));
                rootElement.appendChild(getSample(doc, "TCPWindow", String.valueOf(i), "TCPWindow", Window));
            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yy:HH.mm.SS");
            Date now = new Date();
            String date = DATE_FORMAT.format(now);
            String xmlName = name + "" + date;
            System.err.println("xmlName: " + xmlName);
            StreamResult result = new StreamResult(new File("/home/glazen/Desktop/Measurements/Graph/" + xmlName + ".xml"));

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);
            transformer.transform(source, result);

            System.out.println("File saved!");

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
    }

    public WriteXMLFile_GraphBW_TCPwindows(String name, Vector<Double> BWVector, boolean isInteger) {

        try {
            //this.Measurement = _Measurement;
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("Graph_BW_TCPWindows");
            doc.appendChild(rootElement);

            //All BW vectors have the same size
            for (int i = 0; i < BWVector.size(); i++) {
                String BW = String.valueOf(BWVector.get(i));
                rootElement.appendChild(getSample(doc, "BWVector", String.valueOf(i), "BWVector", BW));
            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yy:HH.mm.SS");
            Date now = new Date();
            String date = DATE_FORMAT.format(now);
            String xmlName = name + "" + date;
            System.err.println("xmlName: " + xmlName);
            StreamResult result = new StreamResult(new File("/home/glazen/Desktop/Measurements/Graph/" + xmlName + ".xml"));

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);
            transformer.transform(source, result);

            System.out.println("File saved!");

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
    }

    private Node getSample(Document doc, String elementName, String id, String name, String value) {
        Element sample = doc.createElement(elementName);
        sample.setAttribute("id", id);

        sample.appendChild(getSampleElements(doc, sample, name, value));
        return sample;
    }

    // utility method to create text node
    private Node getSampleElements(Document doc, Element element, String name, String value) {
        Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(value));
        return node;
    }

}
