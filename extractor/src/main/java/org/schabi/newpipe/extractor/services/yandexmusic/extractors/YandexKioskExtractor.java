package org.schabi.newpipe.extractor.services.yandexmusic.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.services.yandexmusic.api.YandexApi;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.MultiInfoItemsCollector;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.Image;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

public class YandexKioskExtractor extends KioskExtractor<InfoItem> {

    private JsonObject chartResult;
    private JsonArray myPlaylistsResult;
    private JsonArray vibeResult;
    private final String kioskId;

    public YandexKioskExtractor(StreamingService service, ListLinkHandler linkHandler, String kioskId) {
        super(service, linkHandler, kioskId);
        this.kioskId = kioskId;
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        if ("Trending".equals(kioskId)) {
            chartResult = YandexApi.getChart();
        } else if ("Local".equals(kioskId)) {
            if (YandexApi.authorizationToken == null || YandexApi.authorizationToken.isEmpty()) {
                throw new ExtractionException("You need to be logged in to view your playlists. Go to Settings -> Audio and Video -> Yandex Account.");
            }
            try {
                JsonObject collection = YandexApi.getPlaylistLikedAndCreated();
                myPlaylistsResult = new JsonArray();
                if (collection != null) {
                    JsonObject createdTab = collection.getObject("created_playlist_tab");
                    if (createdTab != null) {
                        JsonArray playlists = createdTab.getArray("playlists");
                        if (playlists != null) {
                            for (int i = 0; i < playlists.size(); i++) {
                                myPlaylistsResult.add(playlists.getObject(i));
                            }
                        }
                    }
                    JsonObject likedTab = collection.getObject("liked_playlist_tab");
                    if (likedTab != null) {
                        JsonArray playlists = likedTab.getArray("playlists");
                        if (playlists != null) {
                            for (int i = 0; i < playlists.size(); i++) {
                                myPlaylistsResult.add(playlists.getObject(i));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExtractionException("Failed to fetch user collection.", e);
            }
        } else if ("MyVibe".equals(kioskId)) {
            vibeResult = YandexApi.getVibeTracks();
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return getId();
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());

        if ("Trending".equals(kioskId) && chartResult != null) {
            JsonObject chart = chartResult.getObject("chart");
            if (chart != null) {
                JsonArray tracks = chart.getArray("tracks");
                if (tracks != null) {
                    for (int i = 0; i < tracks.size(); i++) {
                        JsonObject trackObj = tracks.getObject(i).getObject("track");
                        if (trackObj != null) {
                            collector.commit(new YandexStreamInfoItemExtractor(trackObj));
                        }
                    }
                }
            }
        } else if ("Local".equals(kioskId) && myPlaylistsResult != null) {
            for (int i = 0; i < myPlaylistsResult.size(); i++) {
                JsonObject playlistObj = myPlaylistsResult.getObject(i);
                if (playlistObj != null) {
                    collector.commit(new YandexPlaylistInfoItemExtractor(playlistObj));
                }
            }
        } else if ("MyVibe".equals(kioskId) && vibeResult != null) {
            for (int i = 0; i < vibeResult.size(); i++) {
                JsonObject trackObj = vibeResult.getObject(i);
                if (trackObj != null) {
                    collector.commit(new YandexStreamInfoItemExtractor(trackObj));
                }
            }
        }

        return new InfoItemsPage<>(collector, null);
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(Page page) throws IOException, ExtractionException {
        return InfoItemsPage.emptyPage();
    }
    
    // We need an extractor for PlaylistInfoItem
    public static class YandexPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
        private final JsonObject playlist;

        public YandexPlaylistInfoItemExtractor(JsonObject playlist) {
            this.playlist = playlist;
        }

        @Override
        public String getName() {
            return playlist.getString("title", "Unnamed Playlist");
        }

        @Override
        public String getUrl() {
            JsonObject owner = playlist.getObject("owner");
            String user = owner != null ? owner.getString("login", "") : "";
            long kind = playlist.getLong("kind", 0);
            return "https://music.yandex.ru/users/" + user + "/playlists/" + kind;
        }

        @Override
        public String getUploaderName() {
            JsonObject owner = playlist.getObject("owner");
            return owner != null ? owner.getString("name", "Unknown") : "Unknown";
        }

        @Override
        public String getUploaderUrl() {
            JsonObject owner = playlist.getObject("owner");
            return owner != null ? "https://music.yandex.ru/users/" + owner.getString("login", "") : "";
        }

        @Override
        public long getStreamCount() {
            return playlist.getLong("trackCount", 0);
        }

        @Nonnull
        @Override
        public List<Image> getThumbnails() {
            String coverUri = playlist.getString("coverUri");
            if (coverUri == null || coverUri.isEmpty()) {
                coverUri = playlist.getString("ogImage");
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
        public boolean isUploaderVerified() {
            return false;
        }
    }
}
