package com.example.seoulnadeuri;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface SeoulApiService {
    @GET("api/all-places")
    Call<List<SeoulPlaceData>> getAllPlaces();
}