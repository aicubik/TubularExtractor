package org.schabi.newpipe.extractor.services.yandexmusic.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;

public class YandexSearchQueryHandlerFactory extends SearchQueryHandlerFactory {
    public static final String TRACKS = "tracks";
    
    private static final YandexSearchQueryHandlerFactory INSTANCE = new YandexSearchQueryHandlerFactory();

    private YandexSearchQueryHandlerFactory() {}

    public static YandexSearchQueryHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(String searchString) throws ParsingException {
        return "https://music.yandex.ru/search?text=" + searchString;
    }
}
