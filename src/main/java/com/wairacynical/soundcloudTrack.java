package com.wairacynical;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class soundcloudTrack {
    private static final OkHttpClient httpClient = new OkHttpClient();
    private String sourceLink;
    private String directLink;
    private String filetype;
    private String filename;
    private Map<String,String> tags = new LinkedHashMap<>();

    private final String reserveClientId = "c58TXg96mhC1ETLDBCdIhbGdzSHdzqXN";
    //private final String reserveClientId = "FWvCdv5Apc7wvDHUKvfAHngHc2Ai856n";
    private final String part1="https://api-v2.soundcloud.com/media/soundcloud:tracks:";
    private final String part2="/stream/hls";
    private final String part3="https://cf-hls-media.sndcdn.com/media/";
    private final String MAX_LENGHT = "9000000";

    public soundcloudTrack(String sourceLink) {
        this.sourceLink = sourceLink;
        makeTags();
        makeDirectLink();
        makeFilename();
        makeFiletype();
    }

    public String getDirectLink() {
        return directLink;
    }

    public String getFiletype() {
        return filetype;
    }

    public String getFilename() {
        return filename;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    private void makeTags() {
        tags.put("url", sourceLink);
        try {
            Document doc = Jsoup.connect(sourceLink).get();
            tags.put("albumArtUrl", doc.select("[src]").tagName("img").attr("abs:src"));
            tags.put("scTitle", doc.selectFirst("meta[property=\"og:title\"]").attr("content"));
            tags.put("scArtist", doc.selectFirst("meta[itemprop=\"name\"]").attr("content"));
            tags.put("year", doc.selectFirst("time").toString().replaceAll("[^0-9]","").substring(0,4));
            //get real track name
            if (tags.get("scTitle").contains("-")) {
                String[] parts = tags.get("scTitle").split("-");
                tags.put("artist", parts[0].replace("-", "").trim());
                tags.put("title", parts[1].trim());
            } else if (tags.get("scTitle").contains("—")) {
                String[] parts = tags.get("scTitle").split("—");
                tags.put("artist", parts[0].replace("—", "").trim());
                tags.put("title", parts[1].trim());
            } else {
                tags.put("artist", doc.selectFirst("meta[itemprop=\"name\"]").attr("content"));
                tags.put("title",  doc.selectFirst("meta[property=\"og:title\"]").attr("content"));
            }
            //get album name if present
            if (sourceLink.contains("/sets/")) {
                String album = sourceLink.substring(sourceLink.lastIndexOf("/sets/")).replace("/sets/","").replace("-"," ");
                tags.put("album", album.substring(0, 1).toUpperCase() + album.substring(1));
            }
            else tags.put("album", "Single");
            try {
                tags.put("genre", doc.selectFirst("meta[itemprop=\"genre\"]").attr("content"));
            } catch (NullPointerException e) {
                System.out.println("cant determine genre");
                tags.put("genre","Unknown");
            }
        } catch (IOException e) {
            System.out.println("cant open connection to " + sourceLink);
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("error fetching data");
            e.printStackTrace();
            System.exit(1);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("not a direct soundcloud link");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.out.println("not a link");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("unknown error fetching tags");
            e.printStackTrace();
        }
    }

    private void makeDirectLink() {
        try {
            Document doc = Jsoup.connect(sourceLink).get();
            String mediaLink = doc.getAllElements().toString();
            //System.out.println(mediaLink);
            mediaLink = mediaLink.substring(mediaLink.indexOf(part1), mediaLink.indexOf(part2));
            StringBuilder str = new StringBuilder()
                    .append(mediaLink)
                    .append("/stream/hls?client_id=")
                    .append(reserveClientId);
            //System.out.println("requestLink = " + str.toString());
            mediaLink = getRequestToJsonGetUrl(str.toString());
            //System.out.println("mediaLink = " + mediaLink);
            URL website = new URL(mediaLink);
            InputStream in = website.openStream();
            byte[] bytes = in.readAllBytes();
            String playlist = new String(bytes, StandardCharsets.UTF_8);
            //System.out.println("playlist = " + playlist);
            directLink = playlist.substring(playlist.lastIndexOf(part3), playlist.lastIndexOf("#"));
            //System.out.println(directLink);
            directLink = directLink.replaceAll("\\/media\\/.*\\/.*\\/", "/media/0/" + MAX_LENGHT +"/" );
        } catch (IOException e) {
            System.out.println("cant get mediaLink from sourceLink");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("unknown error while getting directLink");
            e.printStackTrace();
        }
    }

    private void makeFiletype() {
        try {
            filetype = directLink.substring(directLink.lastIndexOf("."), directLink.indexOf("?"));
        } catch (Exception e) {
            System.out.println("cant determine filetype");
            e.printStackTrace();
            filetype = ".media";
        }
    }

    private void makeFilename() {
        try {
            filename = directLink.substring(directLink.lastIndexOf("/")+1, directLink.indexOf("?"));
        } catch (Exception e) {
            System.out.println("cant determine filename");
            e.printStackTrace();
            filename = "track";
        }
    }
    public String getRequestToJsonGetUrl(String link) throws IOException {
        Request r = new Request.Builder()
                .url(link)
               // .addHeader("User-Agent", agent)
                .build();
        Response response = httpClient.newCall(r).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        String json = response.body().string();
        Gson g = new Gson();
        Container container = g.fromJson(json, Container.class);
        return container.url;
    }
}
