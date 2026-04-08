package org.schabi.newpipe.extractor.services.yandexmusic.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.utils.Parser;

import java.util.List;

public class YandexPlaylistLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final YandexPlaylistLinkHandlerFactory INSTANCE = new YandexPlaylistLinkHandlerFactory();

    public static YandexPlaylistLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(String url) throws ParsingException {
        if (url.contains("/playlists/")) {
            // Match formats like /users/name/playlists/123 or just /playlists/lk.uuid
            String user = Parser.matchGroup1("/users/(.*?)/playlists/", url);
            String id = Parser.matchGroup1("/playlists/([^/?]+)", url);
            if (user != null && !user.isEmpty()) {
                return user + "/playlists/" + id;
            }
            return "playlists/" + id;
        } else if (url.contains("/album/")) {
            return "album/" + Parser.matchGroup1("/album/([0-9]+)", url);
        }
        throw new ParsingException("Could not get playlist id from url: " + url);
    }

    @Override
    public String getUrl(String id, List<String> contentFilter, String sortFilter) {
        return "https://music.yandex.ru/" + id;
    }

    @Override
    public boolean onAcceptUrl(String url) {
        return url.contains("music.yandex.ru") && (url.contains("/playlists/") || url.contains("/album/"));
    }
}
