package org.schabi.newpipe.extractor.services.yandexmusic.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.utils.Parser;

import java.util.List;

public class YandexKioskLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final YandexKioskLinkHandlerFactory INSTANCE = new YandexKioskLinkHandlerFactory();

    public static YandexKioskLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(String url) throws ParsingException {
        if (url.contains("/chart")) {
            return "Trending"; // use standard trending as chart
        } else if (url.contains("/my/playlists")) {
            return "Local"; // standard local for my playlists
        } else if (url.contains("/radio/personal/wave")) {
            return "MyVibe";
        }
        throw new ParsingException("Could not get kiosk id from url: " + url);
    }

    @Override
    public String getUrl(String id, List<String> contentFilter, String sortFilter) {
        if (id.equals("Trending")) {
            return "https://music.yandex.ru/chart";
        } else if (id.equals("Local")) {
            return "https://music.yandex.ru/my/playlists";
        } else if (id.equals("MyVibe")) {
            return "https://music.yandex.ru/radio/personal/wave";
        }
        return "https://music.yandex.ru/chart";
    }

    @Override
    public boolean onAcceptUrl(String url) {
        return url.contains("music.yandex.ru") && (url.contains("/chart") || url.contains("/my/playlists") || url.contains("/radio/personal/wave"));
    }
}
