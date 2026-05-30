package com.example.seoulnadeuri;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendationEngine {
    private Interpreter tflite;
    // JSON 파일에서 읽어온 고정 데이터를 담아둘 맵 (Key: 장소이름, Value: [실내태그, 20대비율])
    private Map<String, float[]> placeMetaMap = new HashMap<>();

    public RecommendationEngine(Context context) {
        try {
            // 1. TFLite 모델 로드 (이름 확인!)
            tflite = new Interpreter(loadModelFile(context, "seoul_outing.tflite"));

            // 2. assets 폴더의 place_meta.json 읽어오기
            loadPlaceMeta(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // assets 폴더의 JSON 파일을 읽어서 Map에 저장하는 함수
    private void loadPlaceMeta(Context context) {
        try {
            InputStream is = context.getAssets().open("place_meta.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");

            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String name = obj.getString("placeName");
                float indoor = (float) obj.getDouble("indoorTag");
                float ratio = (float) obj.getDouble("target20sRatio");

                // 이름표(name)를 붙여서 서랍(Map)에 보관
                placeMetaMap.put(name, new float[]{indoor, ratio});
            }
            Log.d("META_LOAD", "JSON 메타데이터 " + placeMetaMap.size() + "개 로드 완료!");
        } catch (Exception e) {
            Log.e("META_LOAD", "JSON 읽기 실패: " + e.getMessage());
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws Exception {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public List<PlaceScore> getTop10Places(List<SeoulPlaceData> all121Places, float user20sPref) {
        // [결측치 평균 보정] 기온과 미세먼지가 0인 곳은 서울 평균값으로 채우기
        float sumTemp = 0f, sumPm = 0f;
        int validTempCount = 0, validPmCount = 0;

        for (SeoulPlaceData place : all121Places) {
            if (place.temp != 0) { sumTemp += place.temp; validTempCount++; }
            if (place.pmIndex != 0) { sumPm += place.pmIndex; validPmCount++; }
        }

        float avgTemp = (validTempCount > 0) ? (sumTemp / validTempCount) : 20.0f;
        float avgPm = (validPmCount > 0) ? (sumPm / validPmCount) : 50.0f;

        for (SeoulPlaceData place : all121Places) {
            if (place.temp == 0) place.temp = avgTemp;
            if (place.pmIndex == 0) place.pmIndex = avgPm;
        }

        // AI 추론 시작
        List<PlaceScore> resultScores = new ArrayList<>();

        for (SeoulPlaceData place : all121Places) {
            float[][] input = new float[1][8];

            // JSON 서랍(Map)에서 현재 장소 이름으로 실내태그와 20대 비율을 꺼냄 (없으면 0.5 기본값)
            float indoorTag = 0.5f;
            float targetRatio = 0.5f;
            if (placeMetaMap.containsKey(place.placeName)) {
                float[] meta = placeMetaMap.get(place.placeName);
                indoorTag = meta[0];
                targetRatio = meta[1];
            }

            place.indoorTag = indoorTag;

            input[0][0] = user20sPref;
            input[0][1] = place.getNormalizedTemp();
            input[0][2] = place.getNormalizedRain();
            input[0][3] = place.getNormalizedPm();
            input[0][4] = place.getNormalizedCongestion();
            input[0][5] = place.localEvent;
            input[0][6] = indoorTag;     // JSON에서 꺼낸 실내외 태그
            input[0][7] = targetRatio;   // JSON에서 꺼낸 20대 비율

            float[][] output = new float[1][1];

            if (tflite != null) {
                tflite.run(input, output);
            }

            float score = output[0][0];
            resultScores.add(new PlaceScore(place.placeName, score, place));

            Log.d("TFLITE_SCORE", place.placeName + " -> 예측 점수: " + score);
        }

        // 내림차순 정렬 및 Top 10 반환
        Collections.sort(resultScores, (p1, p2) -> Float.compare(p2.getScore(), p1.getScore()));
        return resultScores.size() > 10 ? resultScores.subList(0, 10) : resultScores;
    }
}