/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

public class WriteXMLFile_bytes1sec {

    //DataMeasurement Measurement = null;
    public WriteXMLFile_bytes1sec(String side, Vector<Integer> Samples, int TotalTransferedBytes, Vector<Double> MeanVector, Vector<Double> LowerBoundVector, Vector<Double> UpperBoundVector, String directory) {

        try {
            //this.Measurement = _Measurement;
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("Samples_1sec_bytes");
            Element node_stats = doc.createElement("Statistics");

            doc.appendChild(rootElement);
            rootElement.appendChild(node_stats);

            //Append Samples
            for (int i = 0; i < Samples.size(); i++) {
                String bytes = String.valueOf(Samples.get(i));
                rootElement.appendChild(getSample(doc, String.valueOf(i), "byteCount", bytes));
            }

            //Append Total Transfered Bytes
            String TotalTransfered = String.valueOf(TotalTransferedBytes);
            node_stats.appendChild(getSampleElements(doc, node_stats, "TotalBytes", TotalTransfered));

            //Append T Distribution Stats
            //Append Mean Vector
            for (int i = 0; i < MeanVector.size(); i++) {
                String mean = String.valueOf(MeanVector.get(i));
                node_stats.appendChild(getSample(doc, String.valueOf(i), "Mean", mean));
            }

            //Append LowerBoundVector
            for (int i = 0; i < LowerBoundVector.size(); i++) {
                String lower_bound = String.valueOf(LowerBoundVector.get(i));
                node_stats.appendChild(getSample(doc, String.valueOf(i), "LowerBound", lower_bound));
            }
            //Append UpperBoundVector
            for (int i = 0; i < UpperBoundVector.size(); i++) {
                String upper_bound = String.valueOf(UpperBoundVector.get(i));
                node_stats.appendChild(getSample(doc, String.valueOf(i), "UpperBound", upper_bound));
            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yy:HH.mm.SS");
            Date now = new Date();
            String date = DATE_FORMAT.format(now);
            String xmlName = side + "" + date;
            System.err.println("xmlName: " + xmlName);
            StreamResult result = new StreamResult(new File("/home/glazen/Desktop/Measurements/MV/" + directory + xmlName + ".xml"));

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

    private Node getSample(Document doc, String id, String name, String value) {
        Element sample = doc.createElement("Sample");
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
