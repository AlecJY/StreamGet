package com.alebit.sget.playlist.DASH;

import com.alebit.sget.Main;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Created by alec on 2017/1/16.
 */
public class DASHPlaylistManager {
    private double mediaPresentationDuration;
    private Element audioAdaptationSet;
    private Element videoAdaptationSet;
    private String uriPrefix;
    private Node[] audioRepresentations;
    private Node[] videoRepresentations;
    private Document manifest;

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
            this.manifest = manifest;
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
        uriPrefix = uri.substring(0, uri.lastIndexOf("/"));
    }

    public Representation[] getAudioRepresentations() {
        NodeList representationList = audioAdaptationSet.getElementsByTagName("Representation");
        Representation[] representations = new Representation[representationList.getLength()];
        audioRepresentations = new Node[representationList.getLength()];
        for (int i = 0; i < representations.length; i++) {
            Element representation = (Element)representationList.item(i);
            String id = representation.getAttribute("id");
            String bandwidth = representation.getAttribute("bandwidth");
            String audioSamplingRate = representation.getAttribute("audioSamplingRate");
            representations[i] = new Representation(id, bandwidth, audioSamplingRate);
            audioRepresentations[i] = representation;
        }
        return representations;
    }

    public Representation[] getVideoRepresentations() {
        NodeList representationList = videoAdaptationSet.getElementsByTagName("Representation");
        Representation[] representations = new Representation[representationList.getLength()];
        videoRepresentations = new Node[representationList.getLength()];
        for (int i = 0; i < representations.length; i++) {
            Element representation = (Element)representationList.item(i);
            String id = representation.getAttribute("id");
            String bandwidth = representation.getAttribute("bandwidth");
            String width = representation.getAttribute("width");
            String height = representation.getAttribute("height");
            representations[i] = new Representation(id, bandwidth, width, height);
            videoRepresentations[i] = representation;
        }
        return representations;
    }

    public void chooseAudioRepresentation(int index) {
        for (int i = 0; i < audioRepresentations.length; i++) {
            if (i != index) {
                audioAdaptationSet.removeChild(audioRepresentations[i]);
            }
        }
    }

    public void chooseVideoRepresentation(int index) {
        for (int i = 0; i < videoRepresentations.length; i++) {
            if (i != index) {
                videoAdaptationSet.removeChild(videoRepresentations[i]);
            }
        }
    }

    public int getAudioSegNumber() {
        Element segTemplate = (Element) audioAdaptationSet.getElementsByTagName("SegmentTemplate").item(0);
        return (int) Math.ceil(mediaPresentationDuration / (Integer.parseInt(segTemplate.getAttribute("duration")) / Integer.parseInt(segTemplate.getAttribute("timescale"))));
    }

    public int getVideoSegNumber() {
        Element segTemplate = (Element) audioAdaptationSet.getElementsByTagName("SegmentTemplate").item(0);
        return (int) Math.ceil(mediaPresentationDuration / (Integer.parseInt(segTemplate.getAttribute("duration")) / Integer.parseInt(segTemplate.getAttribute("timescale"))));
    }

    public String getAudioInitializationURI() {
        Element segTemplate = (Element) audioAdaptationSet.getElementsByTagName("SegmentTemplate").item(0);
        String id = ((Element) audioAdaptationSet.getElementsByTagName("Representation").item(0)).getAttribute("id");
        String uri = segTemplate.getAttribute("initialization").replaceAll("\\$RepresentationID\\$", uriPrefix + "/" + id);
        return uri;
    }

    public String getVideoInitializationURI() {
        Element segTemplate = (Element) videoAdaptationSet.getElementsByTagName("SegmentTemplate").item(0);
        String id = ((Element) videoAdaptationSet.getElementsByTagName("Representation").item(0)).getAttribute("id");
        String uri = segTemplate.getAttribute("initialization").replaceAll("\\$RepresentationID\\$", uriPrefix + "/" + id);
        return uri;
    }

    public String getAudioSegURI(int index) {
        Element segTemplate = (Element) audioAdaptationSet.getElementsByTagName("SegmentTemplate").item(0);
        String id = ((Element) audioAdaptationSet.getElementsByTagName("Representation").item(0)).getAttribute("id");
        int startNum = Integer.parseInt(segTemplate.getAttribute("startNumber"));
        String uri = segTemplate.getAttribute("media").replaceAll("\\$RepresentationID\\$", uriPrefix + "/" + id).replaceAll("\\$Number\\$", Integer.toString(index + startNum));
        return uri;
    }

    public String getVideoSegURI(int index) {
        Element segTemplate = (Element) videoAdaptationSet.getElementsByTagName("SegmentTemplate").item(0);
        String id = ((Element) videoAdaptationSet.getElementsByTagName("Representation").item(0)).getAttribute("id");
        int startNum = Integer.parseInt(segTemplate.getAttribute("startNumber"));
        String uri = segTemplate.getAttribute("media").replaceAll("\\$RepresentationID\\$", uriPrefix + "/" + id).replaceAll("\\$Number\\$", Integer.toString(index + startNum));
        return uri;
    }

    public void saveManifest(Path path) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(manifest);
            FileOutputStream fileOutputStream = new FileOutputStream(path.toFile());
            StreamResult streamResult = new StreamResult(fileOutputStream);
            transformer.transform(domSource, streamResult);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (TransformerException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
