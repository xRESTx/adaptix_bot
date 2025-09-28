package org.example;

import java.io.IOException;
import java.net.HttpCookie;

import com.google.gson.Gson;
import org.example.jsonModel.Product;
import org.example.jsonModel.Root;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import static org.example.Main.Cookies;

public class Parser {
    public static int hasFeedbackPoints(String url1) throws IOException {
        try {
            String card = "https://card.wb.ru/cards/v4/detail?appType=1&curr=rub&dest=-5923914&spp=30&ab_testing=false&nm=" + url1;
            Connection connection = Jsoup.connect(card)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0")
                    .method(Connection.Method.GET)
                    .ignoreContentType(true);

            for (HttpCookie cookie : Cookies) {
                connection.cookie(cookie.getName(), cookie.getValue());
            }
            Connection.Response response = connection.execute();

            String json = response.body();
            System.out.println(json);

            Gson gson = new Gson();
            Root root = gson.fromJson(json, Root.class);

            for (Product product : root.products) {
                if (product.totalQuantity != null) {
                    return product.totalQuantity;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}