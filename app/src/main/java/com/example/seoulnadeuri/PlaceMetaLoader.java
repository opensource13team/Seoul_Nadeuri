package com.example.seoulnadeuri;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PlaceMetaLoader {

    private static final String ASSET_IMAGE_DIR = "place_images";
    private static final String DEFAULT_IMAGE_URL =
            "https://images.unsplash.com/photo-1588668214407-6ea9a6d8c272?q=80&w=800&auto=format&fit=crop";

    private static Map<String, String> imageUrlByPlace;
    private static Map<String, String> assetBaseNameByPlace;
    private static Set<String> availableAssetNames;

    private PlaceMetaLoader() {
    }

    /** Glide load()용 — http URL 또는 file:///android_asset/... */
    public static String getImageUrl(Context context, String placeName) {
        if (placeName == null) {
            return DEFAULT_IMAGE_URL;
        }
        ensureLoaded(context);
        String assetBase = assetBaseNameByPlace.get(placeName);
        if (assetBase != null) {
            return "file:///android_asset/" + ASSET_IMAGE_DIR + "/" + assetBase + ".jpg";
        }
        String url = imageUrlByPlace.get(placeName);
        return (url != null && !url.isEmpty()) ? url : DEFAULT_IMAGE_URL;
    }

    private static void ensureLoaded(Context context) {
        if (imageUrlByPlace != null) {
            return;
        }
        imageUrlByPlace = new HashMap<>();
        assetBaseNameByPlace = new HashMap<>();
        availableAssetNames = new HashSet<>();
        try {
            String[] files = context.getAssets().list(ASSET_IMAGE_DIR);
            if (files != null) {
                for (String file : files) {
                    String lower = file.toLowerCase(Locale.ROOT);
                    if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg")) {
                        continue;
                    }
                    int dot = file.lastIndexOf('.');
                    if (dot > 0) {
                        availableAssetNames.add(file.substring(0, dot));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            InputStream is = context.getAssets().open("place_meta.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            JSONArray arr = new JSONArray(new String(buffer, StandardCharsets.UTF_8));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String placeName = obj.getString("placeName");
                String imageUrl = obj.optString("imageUrl", "");
                if (!imageUrl.isEmpty()) {
                    imageUrlByPlace.put(placeName, imageUrl);
                }
                String assetName = resolveAssetBaseName(placeName);
                if (assetName != null) {
                    assetBaseNameByPlace.put(placeName, assetName);
                }
            }
        } catch (Exception ignored) {
            imageUrlByPlace = new HashMap<>();
            assetBaseNameByPlace = new HashMap<>();
        }
    }

    private static String resolveAssetBaseName(String placeName) {
        if (availableAssetNames.contains(placeName)) {
            return placeName;
        }
        String noSpaces = placeName.replace(" ", "");
        if (availableAssetNames.contains(noSpaces)) {
            return noSpaces;
        }
        String dotVariant = noSpaces.replace("·", ".");
        if (availableAssetNames.contains(dotVariant)) {
            return dotVariant;
        }
        if ("DMC(디지털미디어시티)".equals(placeName)
                && availableAssetNames.contains("DMC(디지털미디어센터)")) {
            return "DMC(디지털미디어센터)";
        }
        String underscorePrefix = placeName + "_";
        for (String name : availableAssetNames) {
            if (name.startsWith(underscorePrefix)) {
                return name;
            }
        }
        int middleDot = placeName.indexOf('·');
        if (middleDot > 0) {
            String first = placeName.substring(0, middleDot);
            if (availableAssetNames.contains(first)) {
                return first;
            }
        }
        return null;
    }
    public static float getTarget20sRatio(android.content.Context context, String placeName) {
        try {
            // 주의: "place_meta.json" 부분은 유저님이 assets에 저장하신 실제 파일명으로 맞춰주세요!
            java.io.InputStream is = context.getAssets().open("place_meta.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            org.json.JSONArray jsonArray = new org.json.JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject obj = jsonArray.getJSONObject(i);

                // 내가 누른 장소 이름과 똑같은 데이터를 찾았다면?
                if (obj.getString("placeName").equals(placeName)) {
                    // target20sRatio 값을 꺼내서 float으로 반환! (없으면 0.5f 반환)
                    return obj.has("target20sRatio") ? (float) obj.getDouble("target20sRatio") : 0.5f;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.5f; // 에러가 나거나 못 찾으면 기본값 0.5f(보통) 반환
    }
}
