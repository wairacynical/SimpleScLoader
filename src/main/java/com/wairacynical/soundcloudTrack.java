package com.wairacynical;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class soundcloudTrack {
    private String sourceLink;
    private String directLink;
    private String filetype;
    private String filename;
    private Map<String,String> tags = new LinkedHashMap<>();

    //private final String reserveClientId = "O7Bh2WbKtCGHK24fWd3xTguziGannZsb";
    private final String reserveClientId = "FWvCdv5Apc7wvDHUKvfAHngHc2Ai856n ";
    private final String part1="https://api-v2.soundcloud.com/media/soundcloud:tracks:";
    private final String part2="/stream/hls";
    private final String part3="https://cf-hls-opus-media.sndcdn.com/media/";

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
            String mediaLink = WebCalls.getAttributeByQuery(sourceLink);
            //System.out.println(mediaLink);
            mediaLink = mediaLink.substring(mediaLink.lastIndexOf(part1), mediaLink.lastIndexOf(part2));
            StringBuilder str = new StringBuilder()
                    .append(mediaLink)
                    .append("/stream/hls?client_id=")
                    .append(reserveClientId);
            System.out.println("requestLink = " + str.toString());
            mediaLink = WebCalls.getRequestToJsonGetUrl(str.toString());
            System.out.println("mediaLink = " + mediaLink);
            URL website = new URL(mediaLink);
            InputStream in = website.openStream();
            byte[] bytes = in.readAllBytes();
            String playlist = new String(bytes, StandardCharsets.UTF_8);
            //System.out.println("playlist = " + playlist);
            directLink = playlist.substring(playlist.lastIndexOf(part3), playlist.lastIndexOf("#"));
            //System.out.println(directLink);
            directLink = directLink.replaceAll("\\/media\\/.*\\/.*\\/", "/media/0/9000000/");
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
        //https://cf-hls-media.sndcdn.com/media/478562/638221/jTrbUX6VbW8u.128.mp3?Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiKjovL2NmLWhscy1tZWRpYS5zbmRjZG4uY29tL21lZGlhLyovKi9qVHJiVVg2VmJXOHUuMTI4Lm1wMyIsIkNvbmRpdGlvbiI6eyJEYXRlTGVzc1RoYW4iOnsiQVdTOkVwb2NoVGltZSI6MTU4MTU5Njg0Mn19fV19&Signature=N2-FouNfxvkX9iE83ghYqiDN9jz07OPgywSc7UPaHvAzZkYRzLOJOEG88ih1m-v4Wd9AHtS7I1NRQJRH5jsKhcKgDh5CYY5ZA85kujMb8hSBjns51JcVgB~VRq-pwEH6Ypvu~I1BAQ6Wnlqa6ALLf3pdPBFaXyCvYo5JKRTOm6eT75t5R3Ok8NhPf1c7U8lp93MFXXK1E4MaesZw9iFLvdCh2cn23oTrLHtPPxN7pRPrXJ8U6lgjWraUhEQlrUwg0a3B6~zmBQIF0xy8eW5r06J-xKLHrNSVjYBlbOqvTtcBdYoUJ4LDLGIBd0k1~kJOIZ8ONDfBDgJEYNV0UdUdqQ__&Key-Pair-Id=APKAI6TU7MMXM5DG6EPQ
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
}
