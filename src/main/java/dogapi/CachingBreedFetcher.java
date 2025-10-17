package dogapi;

import java.util.*;

/**
 * This BreedFetcher caches fetch request results to improve performance and
 * lessen the load on the underlying data source. An implementation of BreedFetcher
 * must be provided. The number of calls to the underlying fetcher are recorded.
 *
 * If a call to getSubBreeds produces a BreedNotFoundException, then it is NOT cached
 * in this implementation. The provided tests check for this behaviour.
 *
 * The cache maps the name of a breed to its list of sub breed names.
 */
public class CachingBreedFetcher implements BreedFetcher {
    private final BreedFetcher fetcher;
    private final Map<String, List<String>> cache = new HashMap<>();
    private int callsMade = 0;

    public CachingBreedFetcher(BreedFetcher fetcher) {
        if (fetcher == null) {
            throw new IllegalArgumentException("Underlying fetcher must not be null.");
        }
        this.fetcher = fetcher;
    }

    @Override
    public List<String> getSubBreeds(String breed) {
        // Normalize cache key to avoid missing due to case/whitespace differences
        String key = (breed == null) ? null : breed.trim().toLowerCase(Locale.ROOT);

        // If key is null or empty, just delegate (don’t cache ambiguous input)
        if (key == null || key.isEmpty()) {
            callsMade++;
            return new ArrayList<>(fetcher.getSubBreeds(breed));
        }

        // Cache hit
        List<String> cached = cache.get(key);
        if (cached != null) {
            // Return a defensive copy so callers can’t mutate our cache
            return new ArrayList<>(cached);
        }

        // Cache miss → call underlying fetcher and record the call
        callsMade++;
        List<String> fetched = fetcher.getSubBreeds(breed);

        // Store an unmodifiable copy (or just a copy) to protect cache integrity
        cache.put(key, Collections.unmodifiableList(new ArrayList<>(fetched)));

        // Return a fresh copy to the caller
        return new ArrayList<>(fetched);
    }

    public int getCallsMade() {
        return callsMade;
    }
}