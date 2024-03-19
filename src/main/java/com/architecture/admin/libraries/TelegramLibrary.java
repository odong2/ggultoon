package com.architecture.admin.libraries;

import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

/*****************************************************
 * 텔레그램 라이브러리
 ****************************************************/
@Component
public class TelegramLibrary {
    private final Boolean TELEGRAM_ENABLE = true;
    private final String BOT_NAME = "nw_bs_bot";
    private final String AUTH_KEY = "1989561465:AAF8KLK9ni-qUOuOMZZkh0_NeI7XOSHkT4g";
    private final String CHAT_ID = "-967347841";

    /**
     * 메세지 전달
     */
    public void sendMessage(String sendMessage) {
        if (TELEGRAM_ENABLE) {
            String url = "https://api.telegram.org/bot" + AUTH_KEY + "/sendMessage";

            try {
                HashMap<String, String> hmObj = new HashMap<>();
                hmObj.put("chat_id", CHAT_ID);
                hmObj.put("text", sendMessage);
                String param = new JSONObject(hmObj).toString();

                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);

                // send the post request
                HttpEntity<String> entity = new HttpEntity<>(param, headers);
                restTemplate.postForEntity(url, entity, String.class);

            } catch (Exception e) {
                System.out.println("Unhandled exception occurred while send Telegram. ==>" + e.getMessage());
            }
        }
    }

    public void sendMessage(String sendMessage, String chatId) {
        String sChatCode;
        String url = "https://api.telegram.org/bot" + AUTH_KEY + "/sendMessage";

        if (TELEGRAM_ENABLE) {
            sChatCode = switch (chatId) {
                case "KDM" -> "371383215";
                case "NHJ" -> "756843550";
                case "LPG" -> "780891809";
                case "LJH" -> "722231502";
                case "KTH" -> "1109991756";
                case "KJW" -> "5166829237";
                case "YJM" -> "5609375886";
                default -> CHAT_ID;
            };

            try {
                HashMap<String, String> hmObj = new HashMap<>();
                hmObj.put("chat_id", sChatCode);
                hmObj.put("text", sendMessage);
                String param = new JSONObject(hmObj).toString();

                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);

                // send the post request
                HttpEntity<String> entity = new HttpEntity<>(param, headers);
                restTemplate.postForEntity(url, entity, String.class);

            } catch (Exception e) {
                System.out.println("Unhandled exception occurred while send Telegram. ==>" + e.getMessage());
            }
        }
    }
}
