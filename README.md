# SimpleScApi
Simpe Java API for SoundCloud, supports downloading and fetching tags

# Features
- fetching tags (artist, title, album, year, album art)
- downloading tracks from single-track url (mp3, opus support soon)
- autosetting tags to downloaded track (works only with .mp3)
- tested/works on macOS, Windows, Linux
- Telegram bot now available https://t.me/soundcloudownloadbot
# Usage
- .jar:
```
java -jar SimpleScApi.jar <soundcloud track link>
```
- java class
```
soundcloudTrack.download(<soundcloud track link, <download path>);
```
