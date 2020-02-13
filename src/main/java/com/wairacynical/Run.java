package com.wairacynical;

import org.jetbrains.annotations.NotNull;

import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Run {
    public static void main(@NotNull String[] args) {
        String url = args[0];
        Path path = FileSystems.getDefault().getPath("");
        scDownloader.Download(url,path.toString());
    }
}
