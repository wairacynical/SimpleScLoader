package com.wairacynical;

import org.jetbrains.annotations.NotNull;

import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Run {
    public static void main(@NotNull String[] args) throws Exception {
        Path path = FileSystems.getDefault().getPath("");
        try {
            String url = args[0];
            soundcloudTrack sc = new soundcloudTrack(url, path.toString());
            sc.download();
            System.out.println(sc.getArtist());
            System.out.println(sc.getTitle());
        } catch (IndexOutOfBoundsException e) {
            System.out.println("provide a soundcloud link");
        }
    }
}
