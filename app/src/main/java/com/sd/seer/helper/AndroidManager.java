package com.sd.seer.helper;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.sd.seer.common.Constants;

import java.util.Arrays;

public class AndroidManager {

    public static boolean checkAndRequestPermissions(Activity activity) {
        if (!Arrays.stream(Constants.PERMISSIONS).allMatch(permission -> ActivityCompat.checkSelfPermission(
                activity, permission) ==
                PackageManager.PERMISSION_GRANTED)) {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            activity.requestPermissions(Constants.PERMISSIONS, Constants.REQUEST_CODE_ANDROID);
            return false;
        } else {
            return true;
        }
    }

}
