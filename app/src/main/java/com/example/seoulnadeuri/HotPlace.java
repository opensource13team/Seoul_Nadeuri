package com.example.seoulnadeuri;

public class HotPlace {
    private String placeName;   // 장소 이름
    private String congestion;  // 혼잡도 (여유, 붐빔 등)

    // 생성자 (데이터를 처음 넣을 때 사용)
    public HotPlace(String placeName, String congestion) {
        this.placeName = placeName;
        this.congestion = congestion;
    }

    // 데이터를 꺼내볼 때 사용하는 함수들(Getter)
    public String getPlaceName() {
        return placeName;
    }

    public String getCongestion() {
        return congestion;
    }
}