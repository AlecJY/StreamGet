package com.alebit.sget.playlist.DASH;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by alec on 2017/1/16.
 */
public class DASHPlaylistManager {
    private double mediaPresentationDuration;
    private Element audioAdaptationSet;
    private Element videoAdaptationSet;
    private String uriPrefix;
    private Representation[] audioRepresentations;
    private Representation[] videoRepresentations;

    public DASHPlaylistManager(InputStream inputStream) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document manifest = documentBuilder.parse(inputStream);
            NodeList mpdList = manifest.getElementsByTagName("MPD");
            if (mpdList.getLength() != 1) {
                throw new DASHPlaylistParseException();
            }
            String durationString = ((Element) mpdList.item(0)).getAttribute("mediaPresentationDuration");
            mediaPresentationDuration = parseTime(durationString);
            NodeList adaptationList = ((Element) mpdList.item(0)).getElementsByTagName("AdaptationSet");
            for (int i = 0; i < adaptationList.getLength(); i++) {
                NodeList representationList = ((Element) adaptationList.item(i)).getElementsByTagName("Representation");
                if (representationList.getLength() == 0) {
                    throw new DASHPlaylistParseException();
                }
                if (((Element) representationList.item(0)).getAttribute("width").equals("")) {
                    audioAdaptationSet = (Element) adaptationList.item(i);
                } else {
                    videoAdaptationSet = (Element) adaptationList.item(i);
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DASHPlaylistParseException e) {
            e.printStackTrace();
        }
    }

    private double parseTime(String timeString) {
        double time = 0;
        int begin = timeString.indexOf("PT") +2;
        int end = timeString.indexOf("H");
        if (end != -1) {
            time += Double.parseDouble(timeString.substring(begin, end)) * 3600;
            begin = end + 1;
        }
        end = timeString.indexOf("M");
        if (end != -1) {
            time += Double.parseDouble(timeString.substring(begin, end)) * 60;
            begin = end + 1;
        }
        end = timeString.indexOf("S");
        if (end != -1) {
            time += Double.parseDouble(timeString.substring(begin, end));
        }
        return time;
    }

    public void setURI(String uri) {
        uriPrefix = uri.substring(0, uri.lastIndexOf("/") + 1);
    }

    public Representation[] getAudioRepresentations() {
        NodeList representationList = audioAdaptationSet.getElementsByTagName("Representation");
        Representation[] representations = new Representation[representationList.getLength()];
        for (int i = 0; i < representations.length; i++) {
            Element representation = (Element)representationList.item(i);
            String id = representation.getAttribute("id");
            String bandwidth = representation.getAttribute("bandwidth");
            String audioSamplingRate = representation.getAttribute("audioSamplingRate");
            representations[i] = new Representation(id, bandwidth, audioSamplingRate);
        }
        audioRepresentations = representations;
        return representations;
    }

    public Representation[] getVideoRepresentations() {
        NodeList representationList = videoAdaptationSet.getElementsByTagName("Representation");
        Representation[] representations = new Representation[representationList.getLength()];
        for (int i = 0; i < representations.length; i++) {
            Element representation = (Element)representationList.item(i);
            String id = representation.getAttribute("id");
            String bandwidth = representation.getAttribute("bandwidth");
            String width = representation.getAttribute("width");
            String height = representation.getAttribute("height");
            representations[i] = new Representation(id, bandwidth, width, height);
        }
        videoRepresentations = representations;
        return representations;
    }
}
