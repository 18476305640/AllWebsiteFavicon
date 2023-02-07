package com.zhuangjie.allwebsitefavicon.util;

import org.yaml.snakeyaml.util.UriEncoder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;

public class URLEncoder2 {
    public static String encode(String url)  {
        if (url == null) return null;
        char[] chars = url.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c >= 0 && c <= 127) {
                stringBuilder.append(c);
                continue;
            }
            try {
                stringBuilder.append(URLEncoder.encode(String.valueOf(c),"UTF-8"));
            } catch (UnsupportedEncodingException e) {
                System.out.println("ERROR URL编码失败！");
                e.printStackTrace();
            }
        }
        return stringBuilder.toString();
    }
}
