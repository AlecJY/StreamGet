package com.alebit.sget.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class Utils {
    @Getter
    private static ObjectMapper jsonObjectMapper = new ObjectMapper(new JsonFactory());

    public static String getExtensionFromURI(String uri) {
        try {
            return getExtensionFromURI(new URI(uri));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getExtensionFromURI(URI uri) {
        String filename = uri.getPath();
        String extension = FilenameUtils.getExtension(filename);
        if (extension.length() > 0) {
            return "." + extension;
        }
        return extension;
    }
}
