package com.example.demo.interceptor;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CustomInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_MINUTE = 2;
    private static final Map<String, Pair> requestCounts = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String clientIP = getClientIP(request);
        // Check if the client IP is already present in the requestCounts map
        if (requestCounts.containsKey(clientIP)) {
            Pair pair = requestCounts.get(clientIP);
            long requestCount = pair.getSecond();
            long requestTime = pair.getFirst();

            // Check if the client has exceeded the maximum requests per minute
            if(requestTime < System.currentTimeMillis()){
                requestTime = System.currentTimeMillis()+60000;
                requestCount = 0;
            } else if (requestCount >= MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many requests");
                return false; // Stop the request from proceeding to the controller
            }

            // Increment the request count for the client IP
            requestCounts.put(clientIP, new Pair(requestTime,requestCount+1));
        } else {
            // Add the client IP to the requestCounts map with an initial count of 1
            requestCounts.put(clientIP, new Pair(System.currentTimeMillis()+60000,1L));
        }

        // Continue with the request handling
        return true;
    }

    private String getClientIP(HttpServletRequest request) {
        String clientIP = request.getRemoteAddr();
        return clientIP;
    }

    @Scheduled(fixedRate = 3600000)
    private void refreshRequestCountsMap(){
        requestCounts.clear();
    }
}

