package com.sd.seer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.sd.seer.common.CommonUtil;
import com.sd.seer.helper.GoogleManager;
import com.sd.seer.model.User;
import com.sd.seer.rest.ServiceFactory;
import com.sd.seer.rest.UserService;

import lombok.SneakyThrows;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText mName;
    private EditText mPhone;

    private Button mProceed;

    private UserService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mName = findViewById(R.id.name);
        mPhone = findViewById(R.id.phone);

        mProceed = findViewById(R.id.proceed);

        mService = ServiceFactory.getServiceInstance(UserService.class);
    }

    public void registerUser(View view) {
        mProceed.setEnabled(false);
        mName.setEnabled(false);
        mPhone.setEnabled(false);
        mService.createUser(new User(GoogleManager.getAccount().getEmail(),
                mName.getText().toString(), mPhone.getText().toString(), null, null))
                .enqueue(new Callback<User>() {
                    @SneakyThrows
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful()) {
                            Snackbar.make(findViewById(R.id.content_register), "User created", Snackbar.LENGTH_LONG);
                            Intent i = new Intent();
                            i.putExtra("user", CommonUtil.MAPPER.writeValueAsString(response.body()));
                            setResult(Activity.RESULT_OK, i);
                            finish();
                        } else onFailure(call, new Exception());
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        Snackbar.make(findViewById(R.id.content_register), "User not created", Snackbar.LENGTH_LONG);
                        mProceed.setEnabled(true);
                        mName.setEnabled(true);
                        mPhone.setEnabled(true);
                    }
                });
    }

}