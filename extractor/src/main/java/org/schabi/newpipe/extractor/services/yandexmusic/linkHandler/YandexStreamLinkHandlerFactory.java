package org.schabi.newpipe.extractor.services.yandexmusic.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YandexStreamLinkHandlerFactory extends LinkHandlerFactory {

    private static final YandexStreamLinkHandlerFactory INSTANCE = new YandexStreamLinkHandlerFactory();
    private static final String ID_PATTERN = "yandex\\.ru(?:.+)?/track/(\\d+)";

    private YandexStreamLinkHandlerFactory() {}

    public static YandexStreamLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(String url) throws ParsingException {
        Pattern pattern = Pattern.compile(ID_PATTERN);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new ParsingException("Could not extract track ID from URL: " + url);
    }

    @Override
    public String getUrl(String id) throws ParsingException {
        return "https://music.yandex.ru/track/" + id;
    }

    @Override
    public boolean onAcceptUrl(String url) throws ParsingException {
        try {
            getId(url);
            return true;
        } catch (ParsingException e) {
            return false;
        }
    }
}
