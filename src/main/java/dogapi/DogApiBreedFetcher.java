package dogapi;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * BreedFetcher implementation that relies on the dog.ceo API.
 * Note that all failures get reported as BreedNotFoundException
 * exceptions to align with the requirements of the BreedFetcher interface.
 */
public class DogApiBreedFetcher implements BreedFetcher {
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Fetch the list of sub breeds for the given breed from the dog.ceo API.
     * @param breed the breed to fetch sub breeds for
     * @return list of sub breeds for the given breed
     * @throws BreedNotFoundException if the breed does not exist (or if the API call fails for any reason)
     */
    @Override
    public List<String> getSubBreeds(String breed) throws BreedNotFoundException {
        if (breed == null || breed.isBlank()) {
            throw new BreedNotFoundException("Breed name must not be empty.");
        }

        final String normalized = breed.trim().toLowerCase(Locale.ROOT);
        final String url = "https://dog.ceo/api/breed/" + normalized + "/list";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                throw new BreedNotFoundException("Empty response from API for breed: " + breed);
            }

            String body = response.body().string();

            // HTTP error? Treat as not found.
            if (!response.isSuccessful()) {
                throw new BreedNotFoundException("Failed to fetch sub-breeds for '" + breed + "': " + body);
            }

            JSONObject json = new JSONObject(body);
            String status = json.optString("status", "");
            if (!"success".equalsIgnoreCase(status)) {
                String apiMsg = json.optString("message", "Breed not found");
                throw new BreedNotFoundException(apiMsg);
            }

            JSONArray arr = json.getJSONArray("message");
            List<String> result = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                result.add(arr.getString(i));
            }

            return result;

        } catch (IOException | org.json.JSONException e) {
            // Any IO or JSON parsing issue → treat as breed not found
            throw new BreedNotFoundException("Breed not found or request failed for '" + breed + "'.", e);
        }
    }
}
