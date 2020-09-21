package com.sd.seer.common;

import android.content.res.Resources;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CommonUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

}
