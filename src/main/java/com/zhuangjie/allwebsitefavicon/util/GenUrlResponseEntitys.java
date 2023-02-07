package com.zhuangjie.allwebsitefavicon.util;

import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

public class GenUrlResponseEntitys {
    public static ResponseEntity gen(String url) {
        if (Strings.isEmpty(url)) {
            return null;
        }
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.set("FALLBACK_URL",String.valueOf(true));
        headers.put("Content-Type", Arrays.asList(MediaType.APPLICATION_OCTET_STREAM_VALUE,"URL"));
        ResponseEntity<byte[]> generatedResponseEntity = new ResponseEntity<>(URLEncoder2.encode(url).getBytes(),headers, HttpStatus.OK);
        return generatedResponseEntity;
    }
    public static boolean isUrlResponseEntity(ResponseEntity responseEntity) {
        try {
            return responseEntity.getHeaders().get("Content-Type").contains("URL");
        }catch (Exception e) {
            return false;
        }
    }

}
