package com.architecture.admin.libraries;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/*****************************************************
 * Curl 라이브러리
 ****************************************************/
@Component
public class CurlLibrary {
    static WebClient client = WebClient.create();

    /**
     * GET
     */
    public static String get(String url,String header) {
        return client.get()
                .uri(url)
                .header("Authorization",header)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * POST
     * Content-Type : application/json
     * @param url
     * @param dataSet
     * @return
     */
    public static String post(String url, Map<String, String> dataSet) {
        return client.post()
                .uri(url)
                .body(BodyInserters.fromValue(dataSet))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
    /**
     * POST
     * Content-Type : application/json
     * @param url 호출URL
     * @param jsonData 전달 데이터 JSON
     * @return
     */
    public static String post(String url, String jsonData,String bearer) {
        return client.post()
                .uri(url)
                .header("Authorization","Bearer " + bearer)
                .header("Content-Type","application/json")
                .body(BodyInserters.fromValue(jsonData))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * POST
     * Content-Type : application/x-www-form-urlencoded
     * @param url
     * @param dataSet
     * @return
     */
    public static String post(String url, MultiValueMap<String, String> dataSet) {
        return client.post()
                .uri(url)
                .body(BodyInserters.fromFormData(dataSet))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
