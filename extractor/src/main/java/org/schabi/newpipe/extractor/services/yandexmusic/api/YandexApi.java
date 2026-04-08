package org.schabi.newpipe.extractor.services.yandexmusic.api;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class YandexApi {
    private static final String BASE_API_URL = "https://api.music.yandex.net";
    public static String authorizationToken = "";

    public static JsonObject getTrackInfo(String trackId) throws ExtractionException, IOException {
        String url = BASE_API_URL + "/tracks/" + trackId;
        JsonObject result = fetchJson(url);
        if (result != null && result.getArray("") != null) {
             // Sometimes it returns an array as result, or object.
             // Usually /tracks/{id} returns array of tracks in 'result'
        }
        // Let's adjust fetchJson to return the whole 'result' object/array
        return result;
    }

    public static JsonObject getTrackInfoV2(String trackId) throws ExtractionException, IOException {
        String url = BASE_API_URL + "/tracks/" + trackId;
        Map<String, java.util.List<String>> headers = new HashMap<>();
        headers.put("User-Agent", java.util.Collections.singletonList("YandexMusicAndroid/24.12"));
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            headers.put("Authorization", java.util.Collections.singletonList("OAuth " + authorizationToken));
        }

        Downloader downloader = NewPipe.getDownloader();
        Response response = downloader.get(url, headers);
        
        try {
            JsonObject root = JsonParser.object().from(response.responseBody());
            return root.getArray("result").getObject(0);
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse Yandex track info response", e);
        }
    }

    public static JsonObject getAlbum(String albumId) throws ExtractionException, IOException {
        String url = BASE_API_URL + "/albums/" + albumId + "/with-tracks";
        return fetchJsonSimple(url);
    }

    public static JsonObject getPlaylist(String user, String playlistId) throws ExtractionException, IOException {
        String url = BASE_API_URL + "/users/" + org.schabi.newpipe.extractor.utils.Utils.encodeUrlUtf8(user) + "/playlists/" + playlistId;
        return fetchJsonSimple(url);
    }

    private static JsonObject fetchJsonSimple(String url) throws ExtractionException, IOException {
        Map<String, java.util.List<String>> headers = new HashMap<>();
        headers.put("User-Agent", java.util.Collections.singletonList("YandexMusicAndroid/24.12"));
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            headers.put("Authorization", java.util.Collections.singletonList("OAuth " + authorizationToken));
        }

        Downloader downloader = NewPipe.getDownloader();
        Response response = downloader.get(url, headers);

        try {
            JsonObject root = JsonParser.object().from(response.responseBody());
            return root.getObject("result");
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse Yandex response", e);
        }
    }

    public static JsonObject searchTracks(String query, int page) throws ExtractionException, IOException {
        String url = BASE_API_URL + "/search?type=track&page=" + page + "&text=" + org.schabi.newpipe.extractor.utils.Utils.encodeUrlUtf8(query);
        return fetchJsonSimple(url);
    }

    public static String resolveStreamUrl(String downloadInfoUrl) throws ExtractionException, IOException {
        Downloader downloader = NewPipe.getDownloader();
        Map<String, java.util.List<String>> headers = new HashMap<>();
        headers.put("User-Agent", java.util.Collections.singletonList("YandexMusicAndroid/24.12"));
        
        Response response = downloader.get(downloadInfoUrl, headers);
        String xml = response.responseBody();
        
        // Simple XML parsing for host, path, ts, s
        String host = getXmlTag(xml, "host");
        String path = getXmlTag(xml, "path");
        String ts = getXmlTag(xml, "ts");
        String s = getXmlTag(xml, "s");
        
        String sign = md5("XGRlNCpWAVERXX18" + path.substring(1) + s);
        
        return "https://" + host + "/get-mp3/" + sign + "/" + ts + path;
    }

    private static String getXmlTag(String xml, String tag) {
        String start = "<" + tag + ">";
        String end = "</" + tag + ">";
        int s = xml.indexOf(start);
        int e = xml.indexOf(end);
        if (s != -1 && e != -1) {
            return xml.substring(s + start.length(), e);
        }
        return "";
    }

    private static String md5(String md5) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
