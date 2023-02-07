package com.zhuangjie.allwebsitefavicon.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesReader {
    private static String defaultFileName = "application.properties";
    public static String getProperty(String key) {
        return getProperty(defaultFileName,key);
    }
    public static String getProperty(String fileName, String key) {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = PropertiesReader.class.getClassLoader().getResourceAsStream(fileName);
            prop.load(input);
            return prop.getProperty(key);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
