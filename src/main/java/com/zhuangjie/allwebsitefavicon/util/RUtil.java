package com.zhuangjie.allwebsitefavicon.util;


import com.zhuangjie.allwebsitefavicon.config.AppConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * 使用方法：RUtil.url().body().header.cookie.build().post()/get().save()/text()
 * @author zhuangjie
 * @date 2023/02/06
 *///
public class RUtil {

    @Data
    public static class RequestData {
        private String url;
        private HttpHeaders httpHeaders;
        private String jsonBody;
        private Map<String,String> cookie;

    }
    public static class RequestAction {

        private static HttpComponentsClientHttpRequestFactory hcRequestFactory;
        private static SimpleClientHttpRequestFactory sRequestFactory;

        static  {
            try {
                sRequestFactory = new SimpleClientHttpRequestFactory();
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(AppConfig.PROXY_HOST,AppConfig.PROXY_PORT));
                sRequestFactory.setProxy(proxy);
                sRequestFactory.setConnectTimeout(4000);
                sRequestFactory.setReadTimeout(4000);
            }catch (Exception e) {
                System.err.println("代理RequestFactory对象创建失败");
            }
            try {
                CloseableHttpClient httpClient = HttpClients.custom()
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, (x509Certificates, s) -> true).build())
                        .build();
                hcRequestFactory =
                        new HttpComponentsClientHttpRequestFactory();
                hcRequestFactory.setHttpClient(httpClient);
                hcRequestFactory.setConnectTimeout(4000);
                hcRequestFactory.setReadTimeout(4000);
            }catch (Exception e) {
                System.err.println("证书解决RequestFactory对象创建失败");
                e.printStackTrace();
            }

        }
        private RUtil.RequestData requestData;

        public RequestAction(RUtil.RequestData requestData) {
            this.requestData = requestData;
        }

        public ResultOperator post() throws IOException {
            //如果传入了head且cookie也传入,就会把cookie加入head中
            if(this.requestData.getHttpHeaders() != null && this.requestData.getCookie() != null) {
                this.requestData.getHttpHeaders().add("Cookie",this.CookieListToString(this.requestData.getCookie()));
            }

            //将body与head加入请求容器中，发送请求
            HttpEntity<String> requestEntity = new HttpEntity<>(this.requestData.getJsonBody(), this.requestData.getHttpHeaders());
            try {
                ResponseEntity<byte[]> result = new RestTemplate().postForEntity(this.requestData.getUrl(), requestEntity,byte[].class);
                //获取请求体
                return new ResultOperator(result,null);
            }catch (Exception e) {
                return new ResultOperator(null,e);
            }

        }



        public ResultOperator get() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36");
//            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);
            try {
                // 使用证书解决的 RequestFactory
                ResponseEntity<byte[]> result = new RestTemplate(hcRequestFactory).exchange(this.requestData.getUrl(), HttpMethod.GET, entity,byte[].class);
                System.out.println("GET请求（"+this.requestData.getUrl()+" OK）："+result);
                return new ResultOperator(result,null);
            }catch (Exception e1) {
                // 使用代理 RequestFactory
                try {
                    ResponseEntity<byte[]> result = new RestTemplate(sRequestFactory).exchange(this.requestData.getUrl(), HttpMethod.GET, entity,byte[].class);
                    return  new ResultOperator(result,null);
                }catch (Exception e2) {
                    System.err.println("GET请求callback也失败了。");
                    return new ResultOperator(null,e2);
                }

            }
        }

        private String  CookieListToString(Map<String,String> map) {
            String result = "";
            Set set = map.keySet();
            Iterator iterator = set.iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                String value = map.get(key);
                if(iterator.hasNext()) {
                    result+=(key+"="+value+";");
                }else  {
                    result+=(key+"="+value);
                }

            }

            return result;
        }
    }

    @Data
    @AllArgsConstructor
    public static class RequestErrorHandler {
        private Exception e;
        public void errorHandler(Consumer<Exception> consumer) {
            if (e != null ) {
                consumer.accept(e);
            }
        }
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResultOperator {
        private ResponseEntity<byte[]> responseEntity;
        private Exception exception;

        public static Exception responseEntityAsFile(ResponseEntity<byte[]> responseEntity,File file) {
            byte[] bytes = responseEntity.getBody();
            // 判断是否为svg图片，如果是转png
            List<Boolean> isSvgs = responseEntity.getHeaders().get("Content-Type").stream().map(type -> type.toLowerCase().indexOf("svg") != -1).collect(Collectors.toList());
            boolean isSvg = isSvgs.contains(true);
            if (isSvg) {
                // 转png
                try {
                    bytes = SvgToPngConverter.convertSvgToPng(bytes);
                } catch (Exception ex) {
                    System.err.println("svg转png失败！");
                    throw new RuntimeException(ex);
                }
            }
            try (
                FileOutputStream fos = new FileOutputStream(file);
                ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ){
                byte[] buffer = new byte[1024*10];
                int len = 0;
                while ((len = bis.read(buffer)) > 0) {
                    fos.write(buffer,0,len);
                }
            }catch (Exception e) {
                e.printStackTrace();
                return e;
            }
            return null;
        }

        private Exception checkResponseEntityIsError(ResponseEntity entity) {
            if (exception != null || entity == null ) {
                 return exception;
            }
            return null;
        }
        public RequestErrorHandler save(File file) {
            Exception exception = checkResponseEntityIsError(responseEntity);
            if (exception != null) {
                return new RequestErrorHandler(exception);
            }
            // 如果是svg图片，转png

            exception = ResultOperator.responseEntityAsFile(responseEntity, file);
            return new RequestErrorHandler(exception);
        }
        public RequestErrorHandler text(StringBuffer textBody) {
            Exception exception = checkResponseEntityIsError(responseEntity);
            if (exception != null) {
                // 说明出现问题了
                return new RequestErrorHandler(exception);
            }
            // 没有出现问题，将数据放入传进来的容器中
            byte[] bytes = responseEntity.getBody()==null?new byte[0]: responseEntity.getBody();
            textBody.append(new String(bytes));
            return new RequestErrorHandler(null);
        }
        public ResponseEntity<byte[]> result() {
            return responseEntity;
        }


    }
    public static RequestAction collect(Consumer<RequestData> requestInfoCollector) {
        RequestData requestData = new RequestData();
        requestInfoCollector.accept(requestData);
        return new RequestAction(requestData);
    }

    // URL解析方法
    public static String urlRootFetch(String url,boolean isRemovePrefix,boolean isRemoveSuffix) {
        if (Strings.isEmpty(url)) {
            return null;
        }
        url = url.trim().toLowerCase();
        String prefix = "";
        String root = "";
        String suffix = "";
        final String regex = "^(https?:\\/\\/)?([^\\/]+)(.*)$";

        final Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String g = matcher.group(i);
                // 如果匹配的是空的，那就下一个
                if (Strings.isEmpty(g)) {
                    continue;
                }
                if (g.indexOf("http") == 0) {
                    // 是前缀
                    prefix = g;
                }else if (g.indexOf("/") >= 0) {
                    // 是后缀
                    suffix = g;
                }else {
                    root = g;
                }
            }
            // 组装并返回
            return ((!isRemovePrefix && prefix != "")?prefix:"") + root + (isRemoveSuffix?"":suffix);
        }
        return null;

    }
    public static String getCurrentDirUrl(String url) {
        if (url == null) {
            return null;
        }
        final String regex = "(https?:\\/\\/[^\\/\\s]+[^?\\s#]+)\\/([^\\/?#\\s]+(\\?[^\\s]+)?)?";

        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(url);

        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                return matcher.group(i);
            }
        }
        return null;
    }







}