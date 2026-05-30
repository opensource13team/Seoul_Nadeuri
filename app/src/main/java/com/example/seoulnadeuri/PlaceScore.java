package com.example.seoulnadeuri;

public class PlaceScore {
    private String placeName;
    private float score;
    private SeoulPlaceData originData;

    public PlaceScore(String placeName, float score, SeoulPlaceData originData) {
        this.placeName = placeName;
        this.score = score;
        this.originData = originData;
    }

    public float getScore() { return score; }
    public SeoulPlaceData getOriginData() { return originData; }
}