package org.example.tonpad.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.example.tonpad.service.SearchService;
import org.springframework.stereotype.Service;

@Service
public class SearchServiceImpl implements SearchService {

    @Override
    public Session openSession(Supplier<String> textSupplier, IntSupplier versionSupplier) {
        return new SessionImpl(textSupplier, versionSupplier);
    }

    private static final class SessionImpl implements Session
    {
        private Supplier<String> textSupplier;
        private IntSupplier versionSupplier;

        private String cachedLowerText;
        private int cachedVersion = Integer.MIN_VALUE;

        private SessionImpl(Supplier<String> textSupplier, IntSupplier versionSupplier)
        {
            this.textSupplier = textSupplier;
            this.versionSupplier = versionSupplier;
        }

        @Override
        public List<Hit> findAll(String query) {
            if(query == null || query.isEmpty()) return List.of();

            ensureCacheIsFresh();

            final String q = query.toLowerCase(Locale.ROOT);

            return indexOfAll(q);
        }

        private List<Hit> indexOfAll(String query) 
        {
            List<Hit> hits = new ArrayList<>();
            int searchIdx = 0;
            while(true)
            {
                int startHitIdx = cachedLowerText.indexOf(query, searchIdx);
                if(startHitIdx < 0) break;
                hits.add(new Hit(startHitIdx, startHitIdx + query.length()));
                searchIdx = startHitIdx + query.length();
            }

            return hits;
        }

        private void ensureCacheIsFresh()
        {
            int version = versionSupplier.getAsInt();
            if(version != cachedVersion)
            {
                String text = textSupplier.get();
                this.cachedLowerText = text == null ? "" : text.toLowerCase(Locale.ROOT);
                this.cachedVersion = version;
            }
        }

        @Override public void close()
        {
            this.cachedLowerText = null;
        }
    }
}
