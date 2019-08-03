package se.leap.bitmaskclient.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import se.leap.bitmaskclient.R;


public class IconTextEntry extends LinearLayout {

    private TextView textView;
    private ImageView iconView;

    public IconTextEntry(Context context) {
        super(context);
        initLayout(context, null);
    }

    public IconTextEntry(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initLayout(context, attrs);
    }

    public IconTextEntry(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context, attrs);
    }

    @TargetApi(21)
    public IconTextEntry(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initLayout(context, attrs);
    }

    void initLayout(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootview = inflater.inflate(R.layout.v_icon_text_list_item, this, true);
        textView = rootview.findViewById(android.R.id.text1);
        iconView = rootview.findViewById(R.id.material_icon);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.IconTextEntry);

            String entryText = typedArray.getString(R.styleable.IconTextEntry_text);
            if (entryText != null) {
                textView.setText(entryText);
            }

            Drawable drawable = typedArray.getDrawable(R.styleable.IconTextEntry_icon);
            if (drawable != null) {
                iconView.setImageDrawable(drawable);
            }

            typedArray.recycle();
        }


    }

    public void setText(@StringRes int id) {
        textView.setText(id);
    }

    public void setText(CharSequence text) {
        textView.setText(text);
    }

    public void setIcon(@DrawableRes int id) {
        iconView.setImageResource(id);
    }

}
