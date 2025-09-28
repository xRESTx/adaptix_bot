package org.example;

import org.example.telegramBots.TelegramBot;
import org.example.telegramBots.TelegramBotLogs;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;

public class Main {

    public static Set<HttpCookie> Cookies;

    public static void main(String[] args) {
        CookieManager cookieManager = new CookieManager();

        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        // Создание HttpClient с поддержкой CookieManager
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();

        try {
            // Отправка запроса к сайту Wildberries
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.wildberries.ru/"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Проверка успешности запроса
            System.out.println(response.statusCode());

            // Извлечение cookies
            Cookies = new HashSet<>(cookieManager.getCookieStore().getCookies());
            Cookies.forEach(System.out::println);
        } catch (Exception e) {
//            e.printStackTrace();
        }
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramBot());
            botsApi.registerBot(new TelegramBotLogs());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}