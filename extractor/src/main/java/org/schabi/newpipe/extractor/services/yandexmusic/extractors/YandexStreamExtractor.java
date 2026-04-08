package org.schabi.newpipe.extractor.services.yandexmusic.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.services.yandexmusic.api.YandexApi;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.util.ArrayList;
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
        // ID should be just trackId
        trackInfo = YandexApi.getTrackInfo(getId());
        
        if (trackInfo == null) {
            throw new ContentNotAvailableException("Track is null");
        }
        if (!trackInfo.getBoolean("available", true)) {
            throw new ContentNotAvailableException("Track is not available");
        }
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

    @Nullable
    @Override
    public String getTextualUploadDate() {
        return null;
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return null;
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() {
        String coverUri = trackInfo.getString("coverUri");
        if (coverUri != null && !coverUri.isEmpty()) {
            coverUri = coverUri.replace("%%", "400x400");
            return Collections.singletonList(new Image("https://" + coverUri));
        }
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Description getDescription() {
        return Description.EMPTY_DESCRIPTION;
    }

    @Override
    public long getLength() {
        return trackInfo.getLong("durationMs", 0) / 1000L;
    }

    @Override
    public long getTimeStamp() {
        return 0;
    }

    @Override
    public long getViewCount() {
        return -1;
    }

    @Override
    public long getLikeCount() {
        return -1;
    }

    @Nonnull
    @Override
    public String getUploaderUrl() {
        JsonArray artists = trackInfo.getArray("artists");
        if (artists != null && !artists.isEmpty()) {
            long artistId = artists.getObject(0).getLong("id");
            return "https://music.yandex.ru/artist/" + artistId;
        }
        return "";
    }

    @Nonnull
    @Override
    public String getUploaderName() {
        JsonArray artists = trackInfo.getArray("artists");
        if (artists != null && !artists.isEmpty()) {
            return artists.getObject(0).getString("name");
        }
        return "Unknown Artist";
    }

    @Nonnull
    @Override
    public List<Image> getUploaderAvatars() {
        return Collections.emptyList();
    }

    @Override
    public List<AudioStream> getAudioStreams() throws IOException, ExtractionException {
        List<AudioStream> audioStreams = new ArrayList<>();
        
        // Fetch download info
        JsonObject downloadInfoResponse = YandexApi.getTrackDownloadInfo(getId());
        JsonArray results = downloadInfoResponse.getArray("result");
        if (results == null || results.isEmpty()) {
            return audioStreams;
        }
        
        // We will just grab the highest bitrate one
        JsonObject bestInfo = results.getObject(0);
        String downloadInfoUrl = bestInfo.getString("downloadInfoUrl");
        String finalUrl = YandexApi.resolveStreamUrl(downloadInfoUrl);
        
        AudioStream.Builder builder = new AudioStream.Builder();
        builder.setId("yandex-mp3-" + bestInfo.getInt("bitrateInKbps"));
        builder.setContent(finalUrl, true);
        builder.setMediaFormat(MediaFormat.MP3);
        builder.setAverageBitrate(bestInfo.getInt("bitrateInKbps"));
        builder.setDeliveryMethod(DeliveryMethod.PROGRESSIVE_HTTP);
        
        audioStreams.add(builder.build());

        return audioStreams;
    }

    @Override
    public List<VideoStream> getVideoStreams() {
        return Collections.emptyList();
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() {
        return Collections.emptyList();
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.AUDIO_STREAM;
    }

    @Nullable
    @Override
    public StreamInfoItemsCollector getRelatedItems() {
        return null; // Not implemented for now
    }
}
