package com.wairacynical;

import com.mpatric.mp3agic.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.validator.routines.UrlValidator;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class soundcloudTrack {
    private static final OkHttpClient httpClient = new OkHttpClient();
    private String sourceLink;
    private String directLink;
    private String filename;
    private Path targetPath;
    private String downloadPath;
    private String finalTrackName;
    private Map<String,String> tags = new LinkedHashMap<>();
    private File track;

    private final String reserveClientId = "c58TXg96mhC1ETLDBCdIhbGdzSHdzqXN";
    //private final String reserveClientId = "FWvCdv5Apc7wvDHUKvfAHngHc2Ai856n";
    private final String MAX_LENGHT = "9000000";

    public soundcloudTrack(String sourceLink, String downloadPath) throws Exception {
        this.sourceLink = sourceLink;
        this.downloadPath = downloadPath;
        if (!linkChecker()) {
            throw new Exception("invalid link");
        }
        makeTags();
        makeDirectLink();
        makeFilename();
    }

    public String getDirectLink() {
        return directLink;
    }

    public String getFilename() {
        return filename;
    }

    public File getTrack() {
        track = new File(finalTrackName);
        return track;
    }

    public String getFinalTrackName() {
        return finalTrackName;
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
        Pattern pattern = Pattern.compile("https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
        LinkedList<String> matchedLinks = new LinkedList<>();
        StringBuilder str = new StringBuilder();
        try {
            //get all stream links from page
            Matcher matcher = pattern.matcher(Jsoup.connect(sourceLink)
                                                            .get()
                                                            .getAllElements()
                                                            .toString());
            while (matcher.find()) {
                if(matcher.group().contains("stream/hls"))
                matchedLinks.add(matcher.group());
            }
            //matchedLinks.forEach(System.out::println);
                str.append(matchedLinks.get(0))
                        .append("?client_id=")
                        .append(reserveClientId);
            // TODO: forceMp3 false behaviour
//            else {
//                str.append(matchedLinks.get(1))
//                        .append("?client_id=")
//                        .append(reserveClientId);
//                }
                //get playlist link
                String buffer = getRequestToJsonGetUrl(str.toString());
                //System.out.println("playlistLink = " + buffer);
                //get data from playlist
                URL playlistLink = new URL(buffer);
                InputStream in = playlistLink.openStream();
                byte[] bytes = in.readAllBytes();
                buffer = new String(bytes, StandardCharsets.UTF_8);
                //System.out.println("buffer = " + buffer);
                // get direct link from playlist
                matcher = pattern.matcher(buffer);
                matcher.find();
                buffer = matcher.group();
                //edit direct link
                //System.out.println("directLink = " + buffer);
                directLink = buffer.replaceAll("\\/media\\/.*\\/.*\\/", "/media/0/" + MAX_LENGHT + "/");
                //System.out.println("directLink = " + directLink);
        } catch (IOException e) {
            System.out.println("cant get mediaLink from sourceLink");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("unknown error while getting directLink");
            e.printStackTrace();
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
    public void download() {
        try {
            URL sourceURL = new URL(directLink);
            targetPath = new File(downloadPath + getFilename()).toPath();
            Files.copy(sourceURL.openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (MalformedURLException e) {
            System.out.println("invalid download link");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("cant save file");
            e.printStackTrace();
        }
            try {
                Mp3File mp3file = new Mp3File(targetPath);
                ID3v2 id3v2Tag = new ID3v24Tag();
                id3v2Tag.setArtist(tags.get("artist"));
                id3v2Tag.setTitle(tags.get("title"));
                id3v2Tag.setAlbum(tags.get("album"));
                id3v2Tag.setYear(tags.get("year"));
                URL aurl = new URL(tags.get("albumArtUrl"));
                InputStream in = aurl.openStream();
                byte[] bytes = in.readAllBytes();
                id3v2Tag.setAlbumImage(bytes, "image/jpeg");
                mp3file.setId3v2Tag(id3v2Tag);
                finalTrackName = downloadPath + tags.get("artist") + " - " + tags.get("title") +".mp3";
                mp3file.save(finalTrackName);
                try
                {
                    File f = new File(downloadPath + getFilename());
                    if(f.delete()) {
                        System.out.println("download successful");
                    };
                } catch(Exception e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (UnsupportedTagException e) {
                e.printStackTrace();
            } catch (InvalidDataException e) {
                e.printStackTrace();
            } catch (NotSupportedException e) {
                e.printStackTrace();
            }
    }
    private String getRequestToJsonGetUrl(String link) throws IOException {
        Request r = new Request.Builder()
                .url(link)
                .build();
        Response response = httpClient.newCall(r).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        JSONObject j = new JSONObject(response.body().string());
        return j.get("url").toString();
    }
    private Boolean linkChecker() {
        UrlValidator uv = new UrlValidator();
        if(uv.isValid(sourceLink)) {
            if (sourceLink.contains(".m")) sourceLink = sourceLink.replace("m.","");
            if (!sourceLink.contains("soundcloud.com")) return false;
            return true;
        }
        else return false;
    }
}
