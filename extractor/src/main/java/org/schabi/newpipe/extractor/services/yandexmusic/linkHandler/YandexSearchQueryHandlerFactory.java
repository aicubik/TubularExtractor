package org.schabi.newpipe.extractor.services.yandexmusic.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;

import java.util.List;

public class YandexSearchQueryHandlerFactory extends SearchQueryHandlerFactory {

    private static final YandexSearchQueryHandlerFactory INSTANCE = new YandexSearchQueryHandlerFactory();

    public static YandexSearchQueryHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(String id, List<String> contentFilter, String sortFilter) {
        return "https://music.yandex.ru/search?text=" + id;
    }

    @Override
    public String getId(String url) throws ParsingException {
        return org.schabi.newpipe.extractor.utils.Utils.decodeUrlUtf8(org.schabi.newpipe.extractor.utils.Parser.matchGroup1("text=([^&]+)", url));
    }

    @Override
    public boolean onAcceptUrl(String url) {
        return url.contains("music.yandex.ru/search");
    }
}
