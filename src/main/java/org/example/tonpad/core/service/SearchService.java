package org.example.tonpad.core.service;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public interface SearchService {
    record Hit(int start, int end) {}

    interface Session extends AutoCloseable {
        List<Hit> findAll (String query);
        @Override void close();
    }

    Session openSession(Supplier<String> textSupplier, IntSupplier versionSupplier);
}
