package org.schabi.newpipe.extractor.services.yandexmusic.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.services.yandexmusic.api.YandexApi;

import java.io.IOException;

import javax.annotation.Nonnull;

public class YandexSearchExtractor extends SearchExtractor {

    private JsonObject searchResult;

    public YandexSearchExtractor(StreamingService service, SearchQueryHandler linkHandler) {
        super(service, linkHandler);
    }

    @Nonnull
    @Override
    public String getSearchSuggestion() {
        return "";
    }

    @Override
    public boolean isCorrection() {
        return false;
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        String query = getSearchString();
        // Just extract via our simple API wrapper
        searchResult = YandexApi.searchTracks(query, 0);

        InfoItemsPage<InfoItem> page = new InfoItemsPage<>();
        if (searchResult == null) {
            return page;
        }

        JsonArray tracks = null;
        if (searchResult.has("tracks")) {
            JsonObject tracksObj = searchResult.getObject("tracks");
            if (tracksObj != null) {
                tracks = tracksObj.getArray("results");
            }
        }

        if (tracks != null) {
            for (Object obj : tracks) {
                if (obj instanceof JsonObject) {
                    page.addItem(new YandexStreamInfoItemExtractor((JsonObject) obj));
                }
            }
        }

        // next page
        int count = searchResult.getInt("perPage", 20);
        int total = searchResult.getInt("total", 0);
        int pageNum = searchResult.getInt("page", 0);
        
        if ((pageNum + 1) * count < total) {
            page.setNextPage(new Page(String.valueOf(pageNum + 1)));
        }

        return page;
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(Page page) throws IOException, ExtractionException {
        if (page == null || page.getId() == null || page.getId().isEmpty()) {
            throw new IllegalArgumentException("Page is invalid");
        }

        String query = getSearchString();
        int pageNum = Integer.parseInt(page.getId());
        
        JsonObject result = YandexApi.searchTracks(query, pageNum);

        InfoItemsPage<InfoItem> resultPage = new InfoItemsPage<>();
        if (result == null) {
            return resultPage;
        }

        JsonArray tracks = null;
        if (result.has("tracks")) {
            JsonObject tracksObj = result.getObject("tracks");
            if (tracksObj != null) {
                tracks = tracksObj.getArray("results");
            }
        }

        if (tracks != null) {
            for (Object obj : tracks) {
                if (obj instanceof JsonObject) {
                    resultPage.addItem(new YandexStreamInfoItemExtractor((JsonObject) obj));
                }
            }
        }

        int count = result.getInt("perPage", 20);
        int total = result.getInt("total", 0);
        
        if ((pageNum + 1) * count < total) {
            resultPage.setNextPage(new Page(String.valueOf(pageNum + 1)));
        }

        return resultPage;
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        // Nothing here, handled in getInitialPage
    }

    @Nonnull
    @Override
    public String getId() {
        return getSearchString();
    }

    @Nonnull
    @Override
    public String getName() {
        return getSearchString();
    }
}
