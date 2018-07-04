package se.leap.bitmaskclient.utils;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.DimenRes;
import android.util.DisplayMetrics;

/**
 * Created by cyberta on 29.06.18.
 */

public class ViewHelper {

    public static int convertDimensionToPx(Context context, @DimenRes int dimension) {
        return context.getResources().getDimensionPixelSize(dimension);
    }

}
