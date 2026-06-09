package com.example.seoulnadeuri;

public class HotPlace {
    private String placeName;
    private String congestion;
    private String weatherInfo;
    private String placeInfo;
    private String eventDetail;
    private String imageUrl; // 6번째 파라미터 (사진 경로)

    // 💡 1번: 데이터가 5개만 들어올 때를 대비한 틀 (검색 화면 등 기존 코드 에러 방지용)
    public HotPlace(String placeName, String congestion, String weatherInfo, String placeInfo, String eventDetail) {
        this.placeName = placeName;
        this.congestion = congestion;
        this.weatherInfo = weatherInfo;
        this.placeInfo = placeInfo;
        this.eventDetail = eventDetail;
        this.imageUrl = ""; // 사진이 안 넘어오면 그냥 빈칸으로 둠
    }

    // 💡 2번: 사진(imageUrl)까지 총 6개가 다 들어올 때 쓰는 틀 (홈 화면, 상세 페이지 용)
    public HotPlace(String placeName, String congestion, String weatherInfo, String placeInfo, String eventDetail, String imageUrl) {
        this.placeName = placeName;
        this.congestion = congestion;
        this.weatherInfo = weatherInfo;
        this.placeInfo = placeInfo;
        this.eventDetail = eventDetail;
        this.imageUrl = imageUrl;
    }

    // 각각의 데이터를 꺼내볼 수 있는 Getter 함수들
    public String getPlaceName() { return placeName; }
    public String getCongestion() { return congestion; }
    public String getWeatherInfo() { return weatherInfo; }
    public String getPlaceInfo() { return placeInfo; }
    public String getEventDetail() { return eventDetail; }
    public String getImageUrl() { return imageUrl; }

    private String aiScore = "";
    public void setAiScore(String aiScore) { this.aiScore = aiScore; }
    public String getAiScore() { return aiScore; }

    private String recommendReason = "";
    public String getRecommendReason() { return recommendReason; }
    public void setRecommendReason(String recommendReason) { this.recommendReason = recommendReason; }
}