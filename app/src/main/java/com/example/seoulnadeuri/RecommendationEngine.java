package com.example.seoulnadeuri;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.util.Log;

public class RecommendationEngine {
    private Interpreter tflite;

    public RecommendationEngine(Context context) {
        try {
            // 자산(assets) 폴더에 넣으신 tflite 파일 이름과 똑같아야 합니다!
            tflite = new Interpreter(loadModelFile(context, "seoul_outing_lightweight.tflite"));
        } catch (Exception e) {
            e.printStackTrace();
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

        // ==========================================
        // 1. [데이터 전처리] 기온과 미세먼지가 0인 곳은 '서울 평균값'으로 땜빵하기
        // ==========================================
        float sumTemp = 0f, sumPm = 0f;
        int validTempCount = 0, validPmCount = 0;

        // 먼저 정상적인(0이 아닌) 센서들의 평균값을 구함
        for (SeoulPlaceData place : all121Places) {
            if (place.temp != 0) { sumTemp += place.temp; validTempCount++; }
            if (place.pmIndex != 0) { sumPm += place.pmIndex; validPmCount++; }
        }

        // 서울 전체 평균 (정상 데이터가 아예 없으면 기본값 20도, 미세먼지 50으로 세팅)
        float avgTemp = (validTempCount > 0) ? (sumTemp / validTempCount) : 20.0f;
        float avgPm = (validPmCount > 0) ? (sumPm / validPmCount) : 50.0f;

        // 0으로 빵꾸난 데이터에 평균값 주입
        for (SeoulPlaceData place : all121Places) {
            if (place.temp == 0) place.temp = avgTemp;
            if (place.pmIndex == 0) place.pmIndex = avgPm;
        }


        // ==========================================
        // 2. AI 모델 추론 및 점수 기록
        // ==========================================
        List<PlaceScore> resultScores = new ArrayList<>();

        for (SeoulPlaceData place : all121Places) {
            float[][] input = new float[1][8];
            input[0][0] = user20sPref;
            input[0][1] = place.getNormalizedTemp();
            input[0][2] = place.getNormalizedRain();
            input[0][3] = place.getNormalizedPm();
            input[0][4] = place.getNormalizedCongestion();
            input[0][5] = place.localEvent;
            input[0][6] = place.getIndoorTag();
            input[0][7] = place.getTarget20sRatio();

            float[][] output = new float[1][1];

            if (tflite != null) {
                tflite.run(input, output);
            }

            float score = output[0][0];
            resultScores.add(new PlaceScore(place.placeName, score, place));

            // 🔥 [중요] 안드로이드 Logcat 창에서 실제 AI 점수가 어떻게 나오는지 확인!
            Log.d("TFLITE_SCORE", place.placeName + " -> 예측 점수: " + score);
        }

        // ==========================================
        // 3. 내림차순 정렬 및 Top 10 추출
        // ==========================================
        Collections.sort(resultScores, (p1, p2) -> Float.compare(p2.getScore(), p1.getScore()));

        return resultScores.size() > 10 ? resultScores.subList(0, 10) : resultScores;
    }
}