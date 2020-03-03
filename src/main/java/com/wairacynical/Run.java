package com.wairacynical;

import org.jetbrains.annotations.NotNull;

import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Run {
    public static void main(@NotNull String[] args) {
        Path path = FileSystems.getDefault().getPath("");
        try {
            String url = args[0];
            scDownloader.Download(url, path.toString());
        } catch (IndexOutOfBoundsException e) {
            System.out.println("provide a soundcloud link");
        }
    }
}
