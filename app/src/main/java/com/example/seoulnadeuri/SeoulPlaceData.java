package com.example.seoulnadeuri;

import com.google.gson.annotations.SerializedName;

public class SeoulPlaceData {
    @SerializedName("placeName") public String placeName;
    @SerializedName("temp") public float temp;
    @SerializedName("pmIndex") public float pmIndex;
    @SerializedName("congestion") public String congestion;
    @SerializedName("localEvent") public float localEvent;
    @SerializedName("eventName") public String eventName;

    // 워커에서 안 주는 값 임시 처리용
    public float rain = 0.0f;

    // 기온 정규화 (-20~40도 기준)
    public float getNormalizedTemp() {
        float normTemp = (temp + 20.0f) / 60.0f;
        return Math.max(0.0f, Math.min(1.0f, normTemp));
    }

    // 강수량 정규화 (0~50mm 기준)
    public float getNormalizedRain() {
        float normRain = rain / 50.0f;
        return Math.max(0.0f, Math.min(1.0f, normRain));
    }

    // 미세먼지 정규화 (0~150 기준, 150 이상은 1.0으로 클리핑)
    public float getNormalizedPm() {
        float normPm = pmIndex / 150.0f;
        // 0.0 보다 작아지거나 1.0 보다 커지는 것을 완벽하게 차단 (안전장치)
        return Math.max(0.0f, Math.min(1.0f, normPm));
    }

    // 혼잡도 텍스트를 0.0 ~ 1.0 점수로 변환
    public float getNormalizedCongestion() {
        if (congestion == null) return 0.5f;
        switch (congestion) {
            case "여유": return 0.2f;
            case "보통": return 0.5f;
            case "약간 붐빔": return 0.8f;
            case "붐빔": return 1.0f;
            default: return 0.5f;
        }
    }

    // 실내외 태그 및 20대 비율 (현재 워커에 없으므로 기본값 처리)
    public float getIndoorTag() { return 0.0f; }
    public float getTarget20sRatio() { return 0.5f; }
}