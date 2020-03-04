package com.wairacynical;

import com.mpatric.mp3agic.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class scDownloader {

    private static Path targetPath;
    private static boolean forceMp3 = true;
    private static String finalTrackName;

    public static File getTrackFile() {
        File trackFile = new File(finalTrackName);
        return trackFile;
    }

    public static void Download(String sourceLink, String downloadPath) {
        soundcloudTrack track = new soundcloudTrack(sourceLink, forceMp3);
        try {
            URL sourceURL = new URL(track.getDirectLink());
            targetPath = new File(downloadPath + track.getFilename()).toPath();
            Files.copy(sourceURL.openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (MalformedURLException e) {
            System.out.println("invalid download link");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("cant save file");
            e.printStackTrace();
        }
        if (forceMp3 == true) {
            try {
                Mp3File mp3file = new Mp3File(targetPath);
                ID3v2 id3v2Tag = new ID3v24Tag();
                id3v2Tag.setArtist(track.getTags().get("artist"));
                id3v2Tag.setTitle(track.getTags().get("title"));
                id3v2Tag.setAlbum(track.getTags().get("album"));
                id3v2Tag.setYear(track.getTags().get("year"));
                URL aurl = new URL(track.getTags().get("albumArtUrl"));
                InputStream in = aurl.openStream();
                byte[] bytes = in.readAllBytes();
                id3v2Tag.setAlbumImage(bytes, "image/jpeg");
                mp3file.setId3v2Tag(id3v2Tag);
                finalTrackName = downloadPath + track.getTags().get("artist") + " - " + track.getTags().get("title") + track.getFiletype();
                mp3file.save(finalTrackName);
                try
                {
                    File f = new File(downloadPath + track.getFilename());
                    f.delete();
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
    }
}


