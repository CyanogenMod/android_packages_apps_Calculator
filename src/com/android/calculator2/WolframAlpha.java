package com.android.calculator2;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.os.Handler;

public class WolframAlpha{
    public static void solve(final String equation, final Handler handle, final ResultsRunnable actionOnSuccess, final Runnable actionOnFailure, final String key) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String query = String.format("http://api.wolframalpha.com/v2/query?appid=%s&input=%s&format=plaintext", URLEncoder.encode(key, "UTF-8"), URLEncoder.encode(equation,"UTF-8"));
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser parser = spf.newSAXParser();
                    XMLReader reader = parser.getXMLReader();
                    XMLHandler xmlHandler = new XMLHandler();
                    reader.setContentHandler(xmlHandler);
                    reader.parse(new InputSource(new URL(query).openStream()));

                    ParsedDataset parsedDataset = xmlHandler.getParsedData();
                    if(parsedDataset.results.size() == 0) parsedDataset.error = true;
                    if(!parsedDataset.error) {
                        actionOnSuccess.setResults(parsedDataset.getResults());
                        handle.post(actionOnSuccess);
                    }
                    else{
                        handle.post(actionOnFailure);
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static class XMLHandler extends DefaultHandler{
        private boolean in_queryresult  = false;
        private boolean in_pod = false;
        private boolean in_subpod = false;
        private boolean in_plaintext_result = false;
        private ParsedDataset parsedDataset = new ParsedDataset();

        public ParsedDataset getParsedData() {
            return this.parsedDataset;
        }

        @Override
        public void startDocument() throws SAXException {
            this.parsedDataset = new ParsedDataset();
        }

        @Override
        public void endDocument() throws SAXException {
            // Nothing to do
        }

        /** Gets be called on opening tags like:
         * <tag>
         * Can provide attribute(s), when xml is like:
         * <tag attribute="attributeValue">*/
        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if(localName.equals("queryresult")) {
                this.in_queryresult = true;
                parsedDataset.error = Boolean.valueOf(atts.getValue("error")) && Boolean.valueOf(atts.getValue("success"));
            }
            else if(in_queryresult) {
                if(localName.equals("pod") && atts.getValue("title").equals("Result")) {
                    this.in_pod = true;
                }
                else if(in_pod) {
                    if(localName.equals("subpod")) {
                        this.in_subpod = true;
                    }
                    else if(in_subpod) {
                        if(localName.equals("plaintext")) {
                            this.in_plaintext_result = true;
                        }
                    }
                }
            }
        }

        /** Gets be called on closing tags like:
         * </tag> */
        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if(localName.equals("queryresult ")) {
                this.in_queryresult = false;
            }
            else if(in_queryresult) {
                if(localName.equals("pod")) {
                    this.in_pod = false;
                }
                else if(in_pod) {
                    if(localName.equals("subpod")) {
                        this.in_subpod = false;
                    }
                    else if(in_subpod) {
                        if(localName.equals("plaintext")) {
                            this.in_plaintext_result = false;
                        }
                    }
                }
            }
        }

        /** Gets be called on the following structure:
         * <tag>characters</tag> */
        @Override
        public void characters(char ch[], int start, int length) {
            if(this.in_plaintext_result) {
                parsedDataset.addResult(new String(ch, start, length).replaceAll("\\s=\\s", "="));
            }
        }
    }

    @SuppressWarnings("unused")
    private static class ParsedDataset {
        private boolean error = false;
        public boolean getError() {
            return error;
        }

        private ArrayList<String> results = new ArrayList<String>();
        public void addResult(String result) {
            results.add(result);
        }
        public ArrayList<String> getResults() {
            return results;
        }
    }

    public static abstract class ResultsRunnable implements Runnable{
        ArrayList<String> results;

        public void setResults(ArrayList<String> results) {
            this.results = results;
        }
    }
}