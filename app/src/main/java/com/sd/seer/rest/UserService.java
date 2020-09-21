package com.sd.seer.rest;

import com.sd.seer.common.Constants;
import com.sd.seer.model.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

@BaseUrl(Constants.USER_SERVICE_URL_BASE)
public interface UserService {

    @GET("/v1/users/{email}")
    Call<User> getUser(@Path("email") String email);

    @POST("/v1/users/")
    Call<User> createUser(@Body User user);

}
