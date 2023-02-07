package com.zhuangjie.allwebsitefavicon.util;


import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResponseUtils {
    private static HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();

    public static<T> T r404(Class<T> t) {
        try {
            response.setStatus(404);
        }finally {
            return null;
        }
    }
}
