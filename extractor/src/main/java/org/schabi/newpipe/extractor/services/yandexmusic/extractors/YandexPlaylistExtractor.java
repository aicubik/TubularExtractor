package org.schabi.newpipe.extractor.services.yandexmusic.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.services.yandexmusic.api.YandexApi;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

public class YandexPlaylistExtractor extends PlaylistExtractor {

    private JsonObject result;
    private String id;
    private boolean isAlbum;

    public YandexPlaylistExtractor(StreamingService service, ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        id = getLinkHandler().getId();
        if (id.startsWith("album/")) {
            isAlbum = true;
            result = YandexApi.getAlbum(id.substring(6));
        } else if (id.startsWith("playlists/")) {
            isAlbum = false;
            String playlistId = id.substring(10);
            if (playlistId.startsWith("lk.")) {
                // For liked tracks, try to get current user UID
                try {
                    JsonObject account = YandexApi.getAccountStatus();
                    if (account != null && account.has("account")) {
                        long uid = account.getObject("account").getLong("uid", 0);
                        result = YandexApi.getPlaylist(String.valueOf(uid), "3");
                    } else {
                        result = YandexApi.getPlaylist("playlists", playlistId);
                    }
                } catch (Exception e) {
                    result = YandexApi.getPlaylist("playlists", playlistId);
                }
            } else {
                result = YandexApi.getPlaylist("playlists", playlistId);
            }
        } else {
            isAlbum = false;
            String[] parts = id.split("/playlists/");
            if (parts.length == 2) {
                result = YandexApi.getPlaylist(parts[0], parts[1]);
            } else {
                throw new ParsingException("Invalid playlist id: " + id);
            }
        }
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    @Override
    public String getName() {
        return result.getString("title");
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        String coverUri = result.getString("coverUri");
        if (coverUri == null || coverUri.isEmpty()) {
            coverUri = result.getString("ogImage");
        }
        if (coverUri != null && !coverUri.isEmpty()) {
            coverUri = coverUri.replace("%%", "400x400");
            if (!coverUri.startsWith("http")) {
                coverUri = "https://" + coverUri;
            }
            return Collections.singletonList(new Image(coverUri, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, Image.ResolutionLevel.UNKNOWN));
        }
        return Collections.emptyList();
    }

    @Override
    public String getUploaderUrl() {
        if (isAlbum) {
            JsonArray artists = result.getArray("artists");
            if (artists != null && !artists.isEmpty()) {
                return "https://music.yandex.ru/artist/" + YandexApi.getStringId(artists.getObject(0), "id");
            }
        } else {
            JsonObject owner = result.getObject("owner");
            if (owner != null) {
                return "https://music.yandex.ru/users/" + owner.getString("login");
            }
        }
        return "";
    }

    @Override
    public String getUploaderName() {
         if (isAlbum) {
            JsonArray artists = result.getArray("artists");
            if (artists != null && !artists.isEmpty()) {
                JsonObject artist = artists.getObject(0);
                return artist != null ? artist.getString("name") : "Unknown";
            }
        } else {
            JsonObject owner = result.getObject("owner");
            if (owner != null) {
                String name = owner.getString("name");
                return name != null ? name : owner.getString("login");
            }
        }
        return "Unknown";
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() throws ParsingException {
        return Collections.emptyList();
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Override
    public long getStreamCount() {
        return result.getLong("trackCount", 0);
    }

    @Nonnull
    @Override
    public Description getDescription() throws ParsingException {
        String desc = result.getString("description");
        return desc != null ? new Description(desc, Description.PLAIN_TEXT) : Description.EMPTY_DESCRIPTION;
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        
        JsonArray tracks = null;
        if (isAlbum) {
            JsonArray volumes = result.getArray("volumes");
            if (volumes != null && !volumes.isEmpty()) {
                tracks = volumes.getArray(0);
            }
        } else {
            JsonArray playlistTracks = result.getArray("tracks");
            if (playlistTracks != null) {
                tracks = new JsonArray();
                for (Object obj : playlistTracks) {
                    if (obj instanceof JsonObject) {
                        JsonObject trackWrapper = (JsonObject) obj;
                        if (trackWrapper.has("track")) {
                            tracks.add(trackWrapper.get("track"));
                        }
                    }
                }
            }
        }

        if (tracks != null) {
            for (Object obj : tracks) {
                if (obj instanceof JsonObject) {
                    collector.commit(new YandexStreamInfoItemExtractor((JsonObject) obj));
                }
            }
        }

        return new InfoItemsPage<>(collector, null);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(org.schabi.newpipe.extractor.Page page) throws IOException, ExtractionException {
        return InfoItemsPage.emptyPage();
    }
}
