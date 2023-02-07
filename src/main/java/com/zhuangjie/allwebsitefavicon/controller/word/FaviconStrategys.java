package com.zhuangjie.allwebsitefavicon.controller.word;

import com.zhuangjie.allwebsitefavicon.util.GenUrlResponseEntitys;
import com.zhuangjie.allwebsitefavicon.util.RUtil;
import com.zhuangjie.allwebsitefavicon.util.RegexMatcher;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FaviconStrategys {

    @Data
    @AllArgsConstructor
    public static class urlModify implements Supplier<ResponseEntity<byte[]>> {
        private String url;
        @Override
        public ResponseEntity<byte[]> get() {
            try {
                String faviconUrl = RUtil.urlRootFetch(url, false, true) + "/favicon.ico";
                RUtil.ResultOperator resultOperator = RUtil.collect(d -> d.setUrl(faviconUrl)).get();
                ResponseEntity<byte[]> responseEntity = resultOperator.getResponseEntity();
                Exception exception = resultOperator.getException();
                if (exception == null ) return responseEntity;
                // 如果出现：“I/O ERROR .. ”这种异常的，就让浏览器打开这个  faviconUrl
                if (
                    (exception.getMessage().trim().toUpperCase().indexOf("I/O ERROR") == 0)
                    ||
                    ( exception instanceof HttpClientErrorException.Forbidden && ((HttpClientErrorException.Forbidden)exception).getRawStatusCode() != 404)
                ) {
                    return  GenUrlResponseEntitys.gen(faviconUrl);
                }
            }catch (Exception e) { }
            return null;
        }
    }
    @Data
    @AllArgsConstructor
    public static class pageText implements Supplier<ResponseEntity<byte[]>> {
        private String url;
        @Override
        public ResponseEntity<byte[]> get() {
            // 如果请求失败，去请求url  // navicat无法获取html
            StringBuffer stringBuffer = new StringBuffer();
            RUtil.collect(d -> d.setUrl(url)).get().text(stringBuffer);
            if (Strings.isEmpty(stringBuffer)) return null;
            final String regex = "<\\s*link[^<>]+rel=\"[^\"<>]*icon[^\"<>]*\"[^<>]+href=\"\\s*([^\"]+)\\s*\"[^<>]*>|<\\s*link[^<>]+href=\"\\s*([^\"]+)\\s*\"[^<>]+rel=\"[^\"<>]*icon[^\"<>]*\"[^<>]*>";
            final String fallbackRegex = "<\\s*link[^<>]+rel=icon\\s+[^<>]+href=([^\"<>]+)\\s*[^<>]*>|<\\s*link[^<>]+href=([^\"<>]+)\\s*[^<>]*rel=icon\\s+[^<>]+>";
            final String string = stringBuffer.toString();
            List<String> faviconUrls = RegexMatcher.matcher(regex, string);
            if (faviconUrls == null || faviconUrls.isEmpty()) {
                // 尝试另一个正则获取
                faviconUrls = RegexMatcher.matcher(fallbackRegex, string);
            }
            System.out.println("TEXT方式:"+faviconUrls);
            // 过滤掉null值
            faviconUrls = faviconUrls.stream().filter(faviconUrl -> Strings.isNotEmpty(faviconUrl)).collect(Collectors.toList());
            if (faviconUrls.size() < 1) {
                return null;
            }
            String iconPath1 = faviconUrls.get(0).trim();
            String iconImgUrl = "";
            // iconPath1 可能有以下几种情况 http://.. , ./..., /..., //xxx
            if(iconPath1.toUpperCase().indexOf("HTTP") == 0) {
                iconImgUrl = iconPath1;
            }else if (iconPath1.indexOf("//") == 0) {
                // 在协议后加 (https?://)
                iconImgUrl = url.substring(0,url.indexOf("://")+1)+iconPath1;
            }else if (iconPath1.indexOf("/") == 0) {
                // 在根位置加
                iconImgUrl = RUtil.urlRootFetch(url,false,true)+iconPath1;
            }else if(iconPath1.indexOf("./") == 0) {
                // 在当前位置上加 : ./xxxx
                iconImgUrl = RUtil.getCurrentDirUrl(url)+iconPath1.substring(1);
            }else {
                // 在当前位置上加 : xxxx
                iconImgUrl = RUtil.getCurrentDirUrl(url)+"/"+iconPath1;
            }
            final String fromPageGetUrl = iconImgUrl;
            ResponseEntity<byte[]> responseEntity = RUtil.collect(d -> d.setUrl(fromPageGetUrl)).get().getResponseEntity();
            if (responseEntity == null ) {
                // 如果无法请求图片，将重定向方式返回
                return GenUrlResponseEntitys.gen(fromPageGetUrl);
            }
            return responseEntity;
        }
    }
}
