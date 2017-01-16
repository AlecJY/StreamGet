package com.alebit.sget.playlist;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
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
    private double duration;

    public DASHPlaylistManager(InputStream inputStream) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document manifest = documentBuilder.parse(inputStream);
            NodeList mpdList = manifest.getElementsByTagName("MPD");
            if (mpdList.getLength() != 1) {
                throw new DASHPlaylistParseException();
            }
            String durationString = mpdList.item(0).getAttributes().getNamedItem("mediaPresentationDuration").getTextContent();
            duration = parseTime(durationString);





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
}
