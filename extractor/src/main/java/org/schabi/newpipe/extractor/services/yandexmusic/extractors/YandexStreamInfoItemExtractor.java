package org.schabi.newpipe.extractor.services.yandexmusic.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

public class YandexStreamInfoItemExtractor implements StreamInfoItemExtractor {

    private final JsonObject itemObject;

    public YandexStreamInfoItemExtractor(JsonObject itemObject) {
        this.itemObject = itemObject;
    }

    @Override
    public String getUrl() {
        return "https://music.yandex.ru/track/" + itemObject.getLong("id");
    }

    @Override
    public String getName() {
        return itemObject.getString("title");
    }

    @Override
    public long getDuration() {
        return itemObject.getLong("durationMs", 0) / 1000L;
    }

    @Override
    public String getUploaderName() {
        JsonArray artists = itemObject.getArray("artists");
        if (artists != null && !artists.isEmpty()) {
            return artists.getObject(0).getString("name");
        }
        return "Unknown Artist";
    }

    @Override
    public String getUploaderUrl() {
        JsonArray artists = itemObject.getArray("artists");
        if (artists != null && !artists.isEmpty()) {
            return "https://music.yandex.ru/artist/" + artists.getObject(0).getLong("id");
        }
        return "";
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
    public String getTextualUploadDate() {
        return null;
    }

    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return null;
    }

    @Override
    public long getViewCount() {
        return -1;
    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        String coverUri = itemObject.getString("coverUri");
        if (coverUri != null && !coverUri.isEmpty()) {
            coverUri = coverUri.replace("%%", "400x400");
            return Collections.singletonList(new Image("https://" + coverUri));
        }
        return Collections.emptyList();
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.AUDIO_STREAM;
    }

    @Override
    public boolean isAd() {
        return false;
    }
}
