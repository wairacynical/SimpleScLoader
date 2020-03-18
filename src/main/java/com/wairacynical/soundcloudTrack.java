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
    private File track;
    private ID3v2 id3v2Tag = new ID3v24Tag();
    private final String MAX_LENGHT = "9000000";

    private final String reserveClientId = "c58TXg96mhC1ETLDBCdIhbGdzSHdzqXN";
    //private final String reserveClientId = "FWvCdv5Apc7wvDHUKvfAHngHc2Ai856n";
    public soundcloudTrack(String sourceLink, String downloadPath) throws Exception {
        this.sourceLink = sourceLink;
        this.downloadPath = downloadPath;
        if (!linkChecker()) {
            throw new Exception("invalid link");
        }
    }

    public File getTrack() {
        track = new File("new" + filename);
        return track;
    }

    public void download() {
        Pattern pattern = Pattern.compile("https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
        LinkedList<String> matchedLinks = new LinkedList<>();
        StringBuilder str = new StringBuilder();
        try {
            Document doc = Jsoup.connect(sourceLink).get();
            String albumArtUrl = doc.select("[src]").tagName("img").attr("abs:src");
            String scTitle = doc.selectFirst("meta[property=\"og:title\"]").attr("content");
            String scArtist = doc.selectFirst("meta[itemprop=\"name\"]").attr("content");
            id3v2Tag.setYear(doc.selectFirst("time").toString().replaceAll("[^0-9]", "").substring(0, 4));
            //get real track name
            if (scTitle.contains(" - ")) {
                String[] parts = scTitle.split(" - ");
                id3v2Tag.setArtist(parts[0].replace(" - ", "").trim());
                id3v2Tag.setTitle((parts[1].trim()));
            } else if (scTitle.contains(" — ")) {
                String[] parts = scTitle.split(" — ");
                id3v2Tag.setArtist(parts[0].replace(" — ", "").trim());
                id3v2Tag.setTitle(parts[1].trim());
            } else {
                id3v2Tag.setArtist(scArtist);
                id3v2Tag.setTitle(scTitle);
            }
            //get album name if present
            if (sourceLink.contains("/sets/")) {
                String album = sourceLink.substring(sourceLink.lastIndexOf("/sets/")).replace("/sets/", "").replace("-", " ");
                id3v2Tag.setAlbum(album.substring(0, 1).toUpperCase() + album.substring(1));
            } else id3v2Tag.setAlbum("Single");
            try {
                id3v2Tag.setComment(doc.selectFirst("meta[itemprop=\"genre\"]").attr("content"));
            } catch (NullPointerException e) {
                System.out.println("cant determine genre");
            }
            URL aurl = new URL(albumArtUrl);
            InputStream in = aurl.openStream();
            byte[] bytes = in.readAllBytes();
            id3v2Tag.setAlbumImage(bytes, "image/jpeg");
            Matcher matcher = pattern.matcher(Jsoup.connect(sourceLink)
                    .get()
                    .getAllElements()
                    .toString());
            while (matcher.find()) {
                if (matcher.group().contains("stream/hls"))
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
            //get data from playlist
            URL playlistLink = new URL(buffer);
            in = playlistLink.openStream();
            bytes = in.readAllBytes();
            buffer = new String(bytes, StandardCharsets.UTF_8);
            // get direct link from playlist
            matcher = pattern.matcher(buffer);
            matcher.find();
            buffer = matcher.group();
            //edit direct link
            directLink = buffer.replaceAll("\\/media\\/.*\\/.*\\/", "/media/0/" + MAX_LENGHT + "/");
            URL sourceURL = new URL(directLink);
            filename = directLink.substring(directLink.lastIndexOf("/") + 1, directLink.indexOf("?"));
            targetPath = new File(downloadPath + filename).toPath();
            Files.copy(sourceURL.openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            Mp3File mp3file = new Mp3File(targetPath);
            mp3file.setId3v2Tag(id3v2Tag);
            mp3file.save("new" + filename);
            File f = new File(downloadPath + filename);
            if (f.delete()) {
                System.out.println("download successful");
            }
        } catch (MalformedURLException e) {
            System.out.println("invalid download link");
            e.printStackTrace();
        }
        catch (IOException e) {
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
        if (uv.isValid(sourceLink)) {
            if (sourceLink.contains("m.")) sourceLink = sourceLink.replace("m.", "");
            if (!sourceLink.contains("soundcloud.com")) return false;
            return true;
        } else return false;
    }
}