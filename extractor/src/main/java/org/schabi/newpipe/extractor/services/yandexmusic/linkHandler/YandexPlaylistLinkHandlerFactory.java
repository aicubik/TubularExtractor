package org.schabi.newpipe.extractor.services.yandexmusic.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.utils.Utils;

import java.util.List;

public class YandexPlaylistLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final YandexPlaylistLinkHandlerFactory INSTANCE = new YandexPlaylistLinkHandlerFactory();

    public static YandexPlaylistLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(String url) throws ParsingException {
        // user playlist: https://music.yandex.ru/users/nick/playlists/1000
        // album: https://music.yandex.ru/album/12345
        if (url.contains("/playlists/")) {
            String user = Utils.matchGroup1("/users/(.*?)/playlists/", url);
            String id = Utils.matchGroup1("/playlists/([0-9]+)", url);
            return user + "/playlists/" + id;
        } else if (url.contains("/album/")) {
            return "album/" + Utils.matchGroup1("/album/([0-9]+)", url);
        }
        throw new ParsingException("Could not get playlist id from url: " + url);
    }

    @Override
    public String getUrl(String id) throws ParsingException {
        return "https://music.yandex.ru/" + id;
    }

    @Override
    public boolean onAcceptUrl(String url) {
        return url.contains("music.yandex.ru") && (url.contains("/playlists/") || url.contains("/album/"));
    }
}
