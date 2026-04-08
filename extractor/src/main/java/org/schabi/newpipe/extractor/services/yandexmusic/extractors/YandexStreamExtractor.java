package org.schabi.newpipe.extractor.services.yandexmusic.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.services.yandexmusic.api.YandexApi;
import org.schabi.newpipe.extractor.stream.*;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.MediaFormat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class YandexStreamExtractor extends StreamExtractor {

    private JsonObject trackInfo;

    public YandexStreamExtractor(StreamingService service, LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        trackInfo = YandexApi.getTrackInfo(getId());
    }

    @Nonnull
    @Override
    public String getId() {
        return getLinkHandler().getId();
    }

    @Nonnull
    @Override
    public String getName() {
        return trackInfo.getString("title");
    }

    @Nonnull
    @Override
    public String getOriginalUrl() {
        return "https://music.yandex.ru/track/" + getId();
    }

    @Override
    @Nonnull
    public String getUploaderUrl() {
        final JsonArray artists = trackInfo.getArray("artists");
        if (artists != null && !artists.isEmpty()) {
            return "https://music.yandex.ru/artist/" + YandexApi.getStringId(artists.getObject(0), "id");
        }
        return "";
    }

    @Override
    @Nonnull
    public String getUploaderName() {
        JsonArray artists = trackInfo.getArray("artists");
        if (artists != null && !artists.isEmpty()) {
            return artists.getObject(0).getString("name");
        }
        return "Unknown";
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        String coverUri = trackInfo.getString("coverUri");
        if (coverUri != null && !coverUri.isEmpty()) {
            coverUri = coverUri.replace("%%", "400x400");
            return Collections.singletonList(new Image("https://" + coverUri, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, Image.ResolutionLevel.UNKNOWN));
        }
        return Collections.emptyList();
    }

    @Override
    public long getLength() {
        return trackInfo.getLong("durationMs", 0) / 1000;
    }

    @Override
    public long getViewCount() {
        return -1;
    }

    @Nonnull
    @Override
    public Description getDescription() {
        return Description.EMPTY_DESCRIPTION;
    }

    @Override
    @Nullable
    public DateWrapper getUploadDate() {
        return null;
    }

    @Nonnull
    @Override
    public StreamType getStreamType() {
        return StreamType.AUDIO_STREAM;
    }

    @Override
    public List<AudioStream> getAudioStreams() throws IOException, ExtractionException {
        JsonObject downloadInfoResponse = YandexApi.getTrackDownloadInfo(getId());
        if (downloadInfoResponse != null) {
            JsonArray results = downloadInfoResponse.getArray("result");
            if (results == null) results = downloadInfoResponse.getArray(""); // Fallback if result is root
            
            if (results != null && !results.isEmpty()) {
                String downloadInfoUrl = results.getObject(0).getString("downloadInfoUrl");
                String streamUrl = YandexApi.resolveStreamUrl(downloadInfoUrl);
                
                AudioStream stream = new AudioStream.Builder()
                        .setId(getId())
                        .setContent(streamUrl, true)
                        .setMediaFormat(MediaFormat.MP3)
                        .setAverageBitrate(192)
                        .build();
                return Collections.singletonList(stream);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<VideoStream> getVideoStreams() {
        return Collections.emptyList();
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() {
        return Collections.emptyList();
    }
}
