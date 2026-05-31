package com.example.seoulnadeuri;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class PlaceMetaLoader {

    private static final String DEFAULT_IMAGE_URL =
            "https://images.unsplash.com/photo-1588668214407-6ea9a6d8c272?q=80&w=800&auto=format&fit=crop";

    private static Map<String, String> imageUrlByPlace;

    private PlaceMetaLoader() {
    }

    public static String getImageUrl(Context context, String placeName) {
        if (placeName == null) {
            return DEFAULT_IMAGE_URL;
        }
        ensureLoaded(context);
        String url = imageUrlByPlace.get(placeName);
        return (url != null && !url.isEmpty()) ? url : DEFAULT_IMAGE_URL;
    }

    private static void ensureLoaded(Context context) {
        if (imageUrlByPlace != null) {
            return;
        }
        imageUrlByPlace = new HashMap<>();
        try {
            InputStream is = context.getAssets().open("place_meta.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            JSONArray arr = new JSONArray(new String(buffer, StandardCharsets.UTF_8));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String imageUrl = obj.optString("imageUrl", "");
                if (!imageUrl.isEmpty()) {
                    imageUrlByPlace.put(obj.getString("placeName"), imageUrl);
                }
            }
        } catch (Exception ignored) {
            imageUrlByPlace = new HashMap<>();
        }
    }
}
