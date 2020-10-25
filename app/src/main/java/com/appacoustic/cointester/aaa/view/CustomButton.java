package com.appacoustic.cointester.aaa.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;

import com.appacoustic.cointester.R;
import com.appacoustic.cointester.aaa.utils.Constants;
import com.appacoustic.cointester.aaa.utils.FontCache;

public class CustomButton extends AppCompatButton {

    public CustomButton(Context context) {
        this(context, null);
    }

    public CustomButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomTextView);
        Typeface typeface;
        switch (typedArray.getInt(R.styleable.CustomTextView_ttf_name, 0)) {
            case 1:
                typeface = FontCache.getTypeface(getContext(), Constants.TYPEFACE_TITILLIUM_WEB_REGULAR);
                break;
            case 2:
                typeface = FontCache.getTypeface(getContext(), Constants.TYPEFACE_TITILLIUM_WEB_SEMI_BOLD);
                break;
            default:
                typeface = FontCache.getTypeface(getContext(), Constants.TYPEFACE_TITILLIUM_WEB_REGULAR);
                break;
        }
        typedArray.recycle();
        setTypeface(typeface);
    }
}