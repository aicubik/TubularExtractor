package org.schabi.newpipe.extractor.services.yandexmusic.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.localization.DateWrapper;

import java.util.Collections;
import java.util.List;

import org.schabi.newpipe.extractor.services.yandexmusic.api.YandexApi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class YandexStreamInfoItemExtractor implements StreamInfoItemExtractor {

    private final JsonObject track;

    public YandexStreamInfoItemExtractor(JsonObject track) {
        this.track = track;
    }

    @Override
    public String getUrl() throws ParsingException {
        return "https://music.yandex.ru/track/" + YandexApi.getStringId(track, "id");
    }

    @Override
    public String getName() throws ParsingException {
        return track.getString("title");
    }

    @Override
    public long getDuration() throws ParsingException {
        return track.getLong("durationMs", 0) / 1000;
    }

    @Override
    public String getUploaderName() throws ParsingException {
        JsonArray artists = track.getArray("artists");
        if (artists != null && !artists.isEmpty()) {
            return artists.getObject(0).getString("name");
        }
        return "Unknown";
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        JsonArray artists = track.getArray("artists");
        if (artists != null && !artists.isEmpty()) {
            return "https://music.yandex.ru/artist/" + YandexApi.getStringId(artists.getObject(0), "id");
        }
        return "";
    }

    @Override
    public String getTextualUploadDate() throws ParsingException {
        return "";
    }

    @Override
    @Nullable
    public DateWrapper getUploadDate() throws ParsingException {
        return null;
    }

    @Override
    public long getViewCount() throws ParsingException {
        return -1;
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        String coverUri = track.getString("coverUri");
        if (coverUri != null && !coverUri.isEmpty()) {
            coverUri = coverUri.replace("%%", "400x400");
            return Collections.singletonList(new Image("https://" + coverUri, Image.HEIGHT_UNKNOWN, Image.WIDTH_UNKNOWN, Image.ResolutionLevel.UNKNOWN));
        }
        return Collections.emptyList();
    }

    @Override
    public StreamType getStreamType() throws ParsingException {
        return StreamType.AUDIO_STREAM;
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Override
    public boolean isAd() {
        return false;
    }
}
