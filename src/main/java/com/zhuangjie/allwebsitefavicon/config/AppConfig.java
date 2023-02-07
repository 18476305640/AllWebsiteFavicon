package com.zhuangjie.allwebsitefavicon.config;

import com.zhuangjie.allwebsitefavicon.util.PropertiesReader;

public class AppConfig {
    public static String  PROXY_HOST = "127.0.0.1";
    public static Integer  PROXY_PORT = 7890;
    public static String  FAVICON_CACHE_PATH = "/AllWebsiteFavicon/";
    static {
        PROXY_HOST = PropertiesReader.getProperty("app.proxy-host");
        PROXY_PORT = Integer.valueOf(PropertiesReader.getProperty("app.proxy-port"));
        FAVICON_CACHE_PATH = PropertiesReader.getProperty("app.favicon-cache-path");
    }
}
