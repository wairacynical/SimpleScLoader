# SimpleScApi
Simpe Java API for SoundCloud, supports downloading and fetching tags

# Features
- fetching tags (artist, title, album, year, album art)
- downloading tracks (mp3, opus support soon)
- autosetting tags to downloaded track (works only with .mp3 files)
# Usage
- .jar:
```
java -jar SimpleScApi.jar <soundcloud track link>
```
- java class
```
scDownloader.Downloader(<soundcloud track link, <download path>);
```
