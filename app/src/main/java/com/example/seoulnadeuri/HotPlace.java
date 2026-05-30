package com.example.seoulnadeuri;

public class HotPlace {
    private String placeName;
    private String congestion;
    private String weatherInfo;
    private String placeInfo;
    private String eventDetail;

    public HotPlace(String placeName, String congestion, String weatherInfo, String placeInfo, String eventDetail) {
        this.placeName = placeName;
        this.congestion = congestion;
        this.weatherInfo = weatherInfo;
        this.placeInfo = placeInfo;
        this.eventDetail = eventDetail;
    }

    public String getPlaceName() { return placeName; }
    public String getCongestion() { return congestion; }
    public String getWeatherInfo() { return weatherInfo; }
    public String getPlaceInfo() { return placeInfo; }
    public String getEventDetail() { return eventDetail; } // Getter 추가
}