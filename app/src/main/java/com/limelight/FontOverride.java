package com.limelight;

import android.graphics.Typeface;
import java.lang.reflect.Field;

public final class FontOverride {
    public static void setDefaultFont(String staticTypefaceFieldName, String fontAssetName) {
        try {
            final Typeface customFontTypeface = Typeface.createFromAsset(
                    App.getContext().getAssets(), "fonts/" + fontAssetName);

            final Field staticField = Typeface.class.getDeclaredField(staticTypefaceFieldName);
            staticField.setAccessible(true);
            staticField.set(null, customFontTypeface);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
