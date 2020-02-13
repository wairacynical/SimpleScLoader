package com.wairacynical;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class WebCalls {
    private static final OkHttpClient httpClient = new OkHttpClient();

    public static String getRequestToJsonGetUrl(String link) throws IOException {
        Request r = new Request.Builder()
                .url(link)
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:72.0) Gecko/20100101 Firefox/72.0")
                .build();
        Response response = httpClient.newCall(r).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        String json = response.body().string();
        Gson g = new Gson();
        Container container = g.fromJson(json, Container.class);
        return container.url;
    }

    public static String getAttributeByQuery(String query, String link) throws IOException, IllegalArgumentException {
        Document doc = Jsoup.connect(link).get();
        String attribute = doc.selectFirst(query).attr("content");
        return attribute;
    }
    public static String getAttributeByQuery(String link) throws IOException, IllegalArgumentException {
        Document doc = Jsoup.connect(link).get();
        String attribute = doc.getAllElements().toString();
        return attribute;
    }
    public static String getAttributeByQuery(String query, String link, String customAttr) throws IOException, IllegalArgumentException {
        Document doc = Jsoup.connect(link).get();
        String attribute = doc.selectFirst(query).attr(customAttr);
        return attribute;
    }
}
