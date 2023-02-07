package com.zhuangjie.allwebsitefavicon;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@SpringBootTest
class AllWebsiteFaviconApplicationTests {

    @Autowired
    private RestTemplate restTemplate;

    public void downloadFile(String url, String filePath) {
        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, null, byte[].class);
        byte[] data = response.getBody();
        try (FileOutputStream stream = new FileOutputStream(filePath)) {
            stream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void contextLoads() throws ExecutionException, InterruptedException {
//        System.out.println(URLEncoder2.encode("https://性价比机场.com/"));

    }

}
