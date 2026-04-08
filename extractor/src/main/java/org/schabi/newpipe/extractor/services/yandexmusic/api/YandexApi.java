package org.schabi.newpipe.extractor.services.yandexmusic.api;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.services.yandexmusic.YandexMusicService;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YandexApi {
    private static final String BASE_API_URL = "https://api.music.yandex.net";
    private static final String SALT = "XGRlBW9FXlekgbPrRHuAleOkk";
    
    // We will inject the OAuth token from the Android app layer.
    public static String authorizationToken = "";

    public static JsonObject getTrackDownloadInfo(String trackId) throws ExtractionException, IOException {
        String url = BASE_API_URL + "/tracks/" + trackId + "/download-info";
        
        Map<String, java.util.List<String>> headers = new HashMap<>();
        headers.put("User-Agent", java.util.Collections.singletonList("YandexMusicAndroid/24.12"));
        if (authorizationToken != null && !authorizationToken.isEmpty()) {
            headers.put("Authorization", java.util.Collections.singletonList("OAuth " + authorizationToken));
        }

        Downloader downloader = NewPipe.getDownloader();
        Response response = downloader.get(url, headers);
        
        try {
            return JsonParser.object().from(response.responseBody());
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse Yandex download_info response", e);
        }
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
        
    public static JsonObject getAlbum(String albumId) throws ExtractionException, IOException {
        String url = BASE_API_URL + "/albums/" + albumId + "/with-tracks";
        return fetchJson(url);
    }

    public static JsonObject getPlaylist(String user, String playlistId) throws ExtractionException, IOException {
        String url = BASE_API_URL + "/users/" + org.schabi.newpipe.extractor.utils.Utils.encodeUrlUtf8(user) + "/playlists/" + playlistId;
        return fetchJson(url);
    }

    private static JsonObject fetchJson(String url) throws ExtractionException, IOException {
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
            throw new ParsingException("Could not parse Yandex search response", e);
        }
    }

    public static String resolveStreamUrl(String downloadInfoUrl) throws ExtractionException, IOException {
        Downloader downloader = NewPipe.getDownloader();
        Map<String, java.util.List<String>> headers = new HashMap<>();
        headers.put("User-Agent", java.util.Collections.singletonList("YandexMusicAndroid/24.12"));
        
        Response response = downloader.get(downloadInfoUrl, headers);
        String xml = response.responseBody();
        
        // Simple XML parsing since we only need a few fields
        String host = extractXmlTag(xml, "host");
        String path = extractXmlTag(xml, "path");
        String ts = extractXmlTag(xml, "ts");
        String s = extractXmlTag(xml, "s");
        
        if (host == null || path == null || ts == null || s == null) {
            throw new ParsingException("Missing fields in Yandex download-info XML:\n" + xml);
        }
        
        // sign = MD5(salt + path[1:] + s)
        String contentToHash = SALT + path.substring(1) + s;
        String sign = md5(contentToHash);
        
        return "https://" + host + "/get-mp3/" + sign + "/" + ts + path;
    }

    private static String extractXmlTag(String xml, String tag) {
        Pattern p = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">");
        Matcher m = p.matcher(xml);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static String md5(String md5) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte anArray : array) {
                sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            // Should not happen
        }
        return null;
    }
}
