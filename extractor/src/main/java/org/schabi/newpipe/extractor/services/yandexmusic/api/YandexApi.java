package org.schabi.newpipe.extractor.services.yandexmusic.api;

import com.grack.nanojson.JsonArray;
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

    public static String getStringId(final JsonObject json, final String key) {
        if (json == null) {
            return "0";
        }
        
        // Try the requested key first
        if (json.containsKey(key)) {
            final Object val = json.get(key);
            if (val != null) {
                return String.valueOf(val).replace("\"", "");
            }
        }
        
        // Fallback keys common in Yandex Music API
        final String[] fallbackKeys = {"trackId", "id", "realId"};
        for (final String fallbackKey : fallbackKeys) {
            if (json.containsKey(fallbackKey)) {
                final Object val = json.get(fallbackKey);
                if (val != null) {
                    return String.valueOf(val).replace("\"", "");
                }
            }
        }
        
        return "0";
    }

    public static JsonObject getTrackInfo(String trackId) throws ExtractionException, IOException {
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

    public static JsonObject getTrackDownloadInfo(String trackId) throws ExtractionException, IOException {
        String url = BASE_API_URL + "/tracks/" + trackId + "/download-info";
        return fetchJsonResult(url);
    }

    public static JsonObject getAlbum(String albumId) throws ExtractionException, IOException {
        String url = BASE_API_URL + "/albums/" + albumId + "/with-tracks";
        return fetchJsonResult(url).getObject("result");
    }

    public static JsonObject getPlaylist(String user, String playlistId) throws ExtractionException, IOException {
        String url = BASE_API_URL + "/users/" + org.schabi.newpipe.extractor.utils.Utils.encodeUrlUtf8(user) + "/playlists/" + playlistId + "?rich-tracks=true";
        return fetchJsonResult(url).getObject("result");
    }

    private static JsonObject fetchJsonResult(String url) throws ExtractionException, IOException {
        return fetchJsonResult(url, "GET", null);
    }

    private static JsonObject fetchJsonResult(String url, String method, byte[] body) throws ExtractionException, IOException {
        Map<String, java.util.List<String>> headers = new HashMap<>();
        headers.put("User-Agent", java.util.Collections.singletonList("YandexMusicAndroid/24.12"));
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            headers.put("Authorization", java.util.Collections.singletonList("OAuth " + authorizationToken));
        }

        Downloader downloader = NewPipe.getDownloader();
        Response response;
        if ("POST".equalsIgnoreCase(method)) {
            response = downloader.post(url, headers, body != null ? body : new byte[0]);
        } else {
            response = downloader.get(url, headers);
        }

        try {
            JsonObject root = JsonParser.object().from(response.responseBody());
            if (root.has("error")) {
                if (root.has("invocationInfo")) {
                    JsonObject inv = root.getObject("invocationInfo");
                    if (inv.has("req-id")) {
                        throw new ExtractionException("YandexApi error: " + root.get("error") + " (req-id: " + inv.getString("req-id") + ") at " + url);
                    }
                }
                throw new ExtractionException("YandexApi error: " + root.get("error") + " body: " + response.responseBody());
            }
            return root;
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse Yandex response: " + response.responseBody(), e);
        }
    }

    public static JsonObject getChart() throws ExtractionException, IOException {
        String url = BASE_API_URL + "/landing3/chart";
        return fetchJsonResult(url).getObject("result");
    }
    
    public static JsonObject getAccountStatus() throws ExtractionException, IOException {
        String url = BASE_API_URL + "/account/status";
        return fetchJsonResult(url).getObject("result");
    }

    public static JsonObject getPlaylistLikedAndCreated() throws ExtractionException, IOException {
        String url = BASE_API_URL + "/landing-blocks/collection/playlists-liked-and-playlists-created?count=100";
        return fetchJsonResult(url).getObject("result");
    }

    public static com.grack.nanojson.JsonArray getUserPlaylists(String uid) throws ExtractionException, IOException {
        String url = BASE_API_URL + "/users/" + org.schabi.newpipe.extractor.utils.Utils.encodeUrlUtf8(uid) + "/playlists/list";
        Map<String, java.util.List<String>> headers = new HashMap<>();
        headers.put("User-Agent", java.util.Collections.singletonList("YandexMusicAndroid/24.12"));
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            headers.put("Authorization", java.util.Collections.singletonList("OAuth " + authorizationToken));
        }

        Downloader downloader = NewPipe.getDownloader();
        Response response = downloader.get(url, headers);

        try {
            JsonObject root = JsonParser.object().from(response.responseBody());
            return root.getArray("result");
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse Yandex response", e);
        }
    }

    public static JsonObject searchTracks(String query, int page) throws ExtractionException, IOException {
        String url = BASE_API_URL + "/search?type=track&page=" + page + "&text=" + org.schabi.newpipe.extractor.utils.Utils.encodeUrlUtf8(query);
        return fetchJsonResult(url);
    }

    public static JsonArray getVibeTracks() throws ExtractionException, IOException {
        // Use Rotor API for "My Vibe"
        try {
            // 1. Create session if needed or just request tracks
            String url = BASE_API_URL + "/rotor/station/user:onyourwave/tracks";
            JsonObject response = fetchJsonResult(url);
            JsonObject result = response.getObject("result");
            if (result != null && result.has("sequence")) {
                JsonArray sequence = result.getArray("sequence");
                JsonArray tracks = new JsonArray();
                for (int i = 0; i < sequence.size(); i++) {
                    JsonObject item = sequence.getObject(i);
                    if (item != null && item.has("track")) {
                        tracks.add(item.getObject("track"));
                    }
                }
                if (!tracks.isEmpty()) return tracks;
            }
        } catch (Exception e) {
            // fallback to old radio API if rotor fails
        }

        // Fallback to legacy Radio API
        final String url = BASE_API_URL + "/radio/personal-vibe/tracks";
        final JsonObject root = fetchJsonResult(url, "POST", new byte[0]);
        final JsonObject result = root.getObject("result");
        if (result != null && result.has("sequence")) {
            final JsonArray sequence = result.getArray("sequence");
            final JsonArray tracks = new JsonArray();
            for (int i = 0; i < sequence.size(); i++) {
                final JsonObject item = sequence.getObject(i);
                if (item != null && item.has("track")) {
                    tracks.add(item.getObject("track"));
                }
            }
            return tracks;
        }
        
        return new JsonArray();
    }

    private static com.grack.nanojson.JsonArray fetchJsonArrayResult(String url) throws ExtractionException, IOException {
        Map<String, java.util.List<String>> headers = new HashMap<>();
        headers.put("User-Agent", java.util.Collections.singletonList("YandexMusicAndroid/24.12"));
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            headers.put("Authorization", java.util.Collections.singletonList("OAuth " + authorizationToken));
        }

        Downloader downloader = NewPipe.getDownloader();
        Response response = downloader.get(url, headers);

        try {
            JsonObject root = JsonParser.object().from(response.responseBody());
            return root.getArray("result");
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse Yandex response", e);
        }
    }

    public static String resolveStreamUrl(String downloadInfoUrl) throws ExtractionException, IOException {
        Downloader downloader = NewPipe.getDownloader();
        Map<String, java.util.List<String>> headers = new HashMap<>();
        headers.put("User-Agent", java.util.Collections.singletonList("YandexMusicAndroid/24.12"));
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            headers.put("Authorization", java.util.Collections.singletonList("OAuth " + authorizationToken));
        }
        
        Response response = downloader.get(downloadInfoUrl, headers);
        String xml = response.responseBody();
        
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
