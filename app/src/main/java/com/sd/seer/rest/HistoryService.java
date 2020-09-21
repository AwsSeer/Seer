package com.sd.seer.rest;

import com.sd.seer.common.Constants;
import com.sd.seer.model.Tracking;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

@BaseUrl(Constants.HISTORY_SERVICE_URL_BASE)
public interface HistoryService {

    @GET("/v1/users/{email}/tracking")
    Call<Tracking> getTracking(@Path("email") String email, @Query("size") Integer size, @Query("page") Integer page);

    @PUT("/v1/users/{email}/tracking")
    Call<Tracking> putTracking(@Path("email") String email, @Body Tracking tracking);

}
