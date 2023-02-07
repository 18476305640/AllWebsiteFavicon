package com.zhuangjie.allwebsitefavicon.controller;

import com.zhuangjie.allwebsitefavicon.config.AppConfig;
import com.zhuangjie.allwebsitefavicon.controller.word.FaviconStrategys;
import com.zhuangjie.allwebsitefavicon.util.GenUrlResponseEntitys;
import com.zhuangjie.allwebsitefavicon.util.PropertiesReader;
import com.zhuangjie.allwebsitefavicon.util.RUtil;
import com.zhuangjie.allwebsitefavicon.util.ResponseUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/v1")
public class FaviconController {
    private static Map<String,String> redirectHistory = new HashMap<>(16);

    private static final String CACHE_PATH;

    static {
        // 从配置中获取存储路径
        CACHE_PATH = AppConfig.FAVICON_CACHE_PATH;
        File file = new File(CACHE_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    @GetMapping("/favicon")
    public ResponseEntity<byte[]> getWebsiteFaviconMain(@RequestParam("url") String url, HttpServletResponse response) throws ExecutionException, InterruptedException, IOException {
        // 如果url为空，那就返回null
        if (Strings.isEmpty(url)) return ResponseUtils.r404(ResponseEntity.class);
        // 看一下本地是否已经有了，如果有直接返回
        ResponseEntity<byte[]> localResponseEntity = tryReadLocalFile(url);
        // 看一下缓存中是否有重定向记录，如果有，直接重定向
        String redirectUrl = redirectHistory.get(RUtil.urlRootFetch(url, true, true));
        if (redirectUrl != null ) {
            System.out.println(url+", 将直接重定向");
            response.sendRedirect(redirectUrl);
            return null;
        }

        if (localResponseEntity != null ) return localResponseEntity;
        // 判断请求的url是否带有协议头
        if(url.toLowerCase().indexOf("http") == -1) {
            // 找不到，需要分http与https
            CompletableFuture<List<ResponseEntity<byte[]>>> httpFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return getWebsiteFavicon("http://" + url);
                } catch (Exception e) {
                    return null;
                }
            });
            CompletableFuture<List<ResponseEntity<byte[]>>> httpsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return getWebsiteFavicon("https://" + url);
                } catch (Exception e) {
                    return null;
                }
            });
            CompletableFuture.allOf(httpFuture,httpsFuture).get();
            // 合并为一个List<ResponseEntity<byte[]>>
            List<ResponseEntity<byte[]>> allResponseEntities = new ArrayList<>();
            List<ResponseEntity<byte[]>> httpResponseEntities = httpFuture.get();
            List<ResponseEntity<byte[]>> httpsResponseEntities = httpsFuture.get();
            if (httpResponseEntities != null ) allResponseEntities.addAll(httpResponseEntities);
            if (httpsResponseEntities != null ) allResponseEntities.addAll(httpsResponseEntities);
            // 过滤过为null ResponseEntity<byte[]>
            allResponseEntities = allResponseEntities.stream().filter(responseEntity -> responseEntity != null).collect(Collectors.toList());
            return responseByUrl(url,allResponseEntities,response);

        }
        // 是带协议头的
        try {
            List<ResponseEntity<byte[]>> allResponseEntities = getWebsiteFavicon(url);
            return responseByUrl(url,allResponseEntities,response);
        } catch (Exception e) {}
        return ResponseUtils.r404(ResponseEntity.class);

    }

    private ResponseEntity<byte[]> responseByUrl(String url, List<ResponseEntity<byte[]>> responseEntityArray,HttpServletResponse response) throws IOException {
        // 分类
        List<ResponseEntity<byte[]>> imgResponseEntity = new ArrayList<>();
        List<ResponseEntity<byte[]>> urlResponseEntity = new ArrayList<>();
        responseEntityArray.stream().forEach(responseEntity -> {
            if (GenUrlResponseEntitys.isUrlResponseEntity(responseEntity)) {
                urlResponseEntity.add(responseEntity);
            }else {
                imgResponseEntity.add(responseEntity);
            }
        });
        // 从本地响应
        if (imgResponseEntity.size() > 0) {
            File file = readLocalFileContainerByUrl(url);
            // 如果文件不存在，写入
            if (!file.exists())  RUtil.ResultOperator.responseEntityAsFile(imgResponseEntity.get(0),file);
            // 读取返回
            return tryReadLocalFile(file);
        }
        // 重定向
        if (urlResponseEntity.size() > 0) {
            String redirectUrl = new String(urlResponseEntity.get(0).getBody());
            // 记录一下重定向
            redirectHistory.put(RUtil.urlRootFetch(url,true,true),redirectUrl);
            response.sendRedirect(redirectUrl);
            return null;
        }
        // 读取默认图片返回
        return responseDefaultImg();
    }

    public List<ResponseEntity<byte[]>> getWebsiteFavicon(String url) throws ExecutionException, InterruptedException, IOException {
        // 如果url不合法，那下面根据url获取favicon是没法进行的
        if (Strings.isEmpty(url)) {
            return null;
        }
        // 根据URL进行指定规则的拼接，得到存储在本地的文件，如果有，直接返回影响对象
        ResponseEntity<byte[]> localResponseEntity = tryReadLocalFile(url);
        if (localResponseEntity != null ) return Arrays.asList(localResponseEntity);
        // 如果没有, 只有进行抓取了
        CompletableFuture<ResponseEntity<byte[]>> t1 = CompletableFuture.supplyAsync(new FaviconStrategys.urlModify(url));
        CompletableFuture<ResponseEntity<byte[]>> t2 = CompletableFuture.supplyAsync(new FaviconStrategys.pageText(url));
        CompletableFuture.allOf(t1,t2).get();
        // 抓取的全部资源，有重定向URL、直接图片资源，都包装为 ResponseEntity<byte[]>
        List<ResponseEntity<byte[]>> responseEntitys = Arrays.asList(t1.get(),t2.get());
        // 过滤掉null值
        responseEntitys = responseEntitys.stream().filter(responseEntity -> {
            // 过滤掉null
            if (responseEntity == null) {
                return false;
            }
            // 过滤掉body为null的
            byte[] body = responseEntity.getBody();
            if (body == null || body.length <= 0) {
                return false;
            }
            // 过滤掉内容为html的
            String type = responseEntity.getHeaders().getContentType().getType();
            if ( type.toLowerCase().indexOf("text") != -1) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        // 通过过滤，得到的都是有效的 ResponseEntity<byte[]>
        if (responseEntitys != null && responseEntitys.size() > 0) return responseEntitys;
        return null;
    }

    private ResponseEntity<byte[]> tryReadLocalFile(String url) {
        String localFileName = RUtil.urlRootFetch(url,true,true) + ".ico";
        File file = new File(CACHE_PATH + localFileName);
        return tryReadLocalFile(file);
    }
    private ResponseEntity<byte[]> tryReadLocalFile(File file) {
        if (file.exists()) {
            // 文件已存在了
            return  readAndResponse(file);
        }
        return null;
    }
    private File readLocalFileContainerByUrl(String url) {
        String localFileName = RUtil.urlRootFetch(url,true,true) + ".ico";
        File file = new File(CACHE_PATH + localFileName);
        if (file.exists()) {
            return null;
        }
        return file;
    }


    private ResponseEntity<byte[]> responseDefaultImg() {
        File defaultFile = null;
        try {
            defaultFile = new File(new ClassPathResource("static/img/default.png").getFile().getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[ERROR] 请检查默认图片是否存在，位于 resources/static/img/default.png");
        }
        return readAndResponse(defaultFile);
    }
    private ResponseEntity<byte[]> readAndResponse(File file) {
        // 读取本地图片并返回
        try (
                InputStream inputStream = new FileInputStream(file);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ){
            byte[] buffer = new byte[1024];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            byte[] imageBytes = outputStream.toByteArray();
            inputStream.close();
            outputStream.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(imageBytes.length);

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        }catch (Exception exception) {
            System.err.println("[ERROR] 请检查所读取的位置是否存在");
            System.err.println(file.getPath());
        }
        return null;
    }

}
