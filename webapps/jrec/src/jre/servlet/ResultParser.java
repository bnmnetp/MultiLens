package jre.servlet;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.*;
//import org.apache.xerces.parsers.SAXParser;
import java.io.InputStream;
import java.util.TreeSet;

import org.grouplens.multilens.Recommendation;

public class ResultParser extends DefaultHandler {
    private TreeSet myTree;
    private String currentID;
    private float currentSim;
    private float currentPred;

    public ResultParser() {
    }

    public TreeSet getRecommendations(InputStream input) {
        try {
            XMLReader  myParser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            myParser.setContentHandler(this);
            myParser.parse(new InputSource(input));
        }
        catch(Exception e) {
            System.out.println("Exc " + e);
            e.printStackTrace();
        }
        return myTree;
    }

    public void startDocument() {
        // Do nothing
        //System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    }

    public void endDocument() {
        //out.flush();
    }

    public void startElement(String nsURI, String strippedName, String name, Attributes attrs) {
        //System.out.print('<' + name );
        if (name.equals("recapi")) {
            myTree = new TreeSet();
        } else if (name.equals("item")) {
            // since we are using the compact format check the attributes.
            if (attrs != null) {
                int len = attrs.getLength();
                for (int i = 0; i < len; i++) {
                    if (attrs.getLocalName(i).equals("movie")) {
                        currentID = attrs.getValue(i);
                    } else if (attrs.getLocalName(i).equals("sim")) {
                        //System.out.println("sim = " + attrs.getValue(i));
                        currentSim = Float.parseFloat(attrs.getValue(i));
                    } else if (attrs.getLocalName(i).equals("pred")) {
                        //System.out.println("pred = " + attrs.getValue(i));
                        currentPred = Float.parseFloat(attrs.getValue(i));
                    }
                }
            }
            // If we wanted to be complete would could add processing for movie,sim, and pred
            // as their own tags.
        }

    }

    public void endElement(String uri, String name, String qname) {
        // putting the add here is cool becuase it gives us the flexibility above to process
        // items either with attributes, or with elements!
        if (name.equals("item")) {
            myTree.add(new Recommendation(currentID,currentSim,currentPred));
        }
    }

    public void characters(char ch[], int start, int length) {
        //System.out.print(new String(ch, start, length));
    }


    public void ignorableWhitespace(char ch[], int start, int length) {

        characters(ch, start, length);

    }

}