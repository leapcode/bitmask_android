package se.leap.bitmaskclient.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import se.leap.bitmaskclient.R;

public class IconSwitchEntry extends LinearLayout {

    private TextView textView;
    private ImageView iconView;
    private SwitchCompat switchView;
    private CompoundButton.OnCheckedChangeListener checkedChangeListener;

    public IconSwitchEntry(Context context) {
        super(context);
        initLayout(context, null);
    }

    public IconSwitchEntry(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initLayout(context, attrs);
    }

    public IconSwitchEntry(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context, attrs);
    }

    @TargetApi(21)
    public IconSwitchEntry(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initLayout(context, attrs);
    }

    void initLayout(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootview = inflater.inflate(R.layout.v_switch_list_item, this, true);
        textView = rootview.findViewById(android.R.id.text1);
        iconView = rootview.findViewById(R.id.material_icon);
        switchView = rootview.findViewById(R.id.option_switch);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.IconSwitchEntry);

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

    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        checkedChangeListener = listener;
        switchView.setOnCheckedChangeListener(checkedChangeListener);
    }

    public void setText(@StringRes int id) {
        textView.setText(id);
    }

    public void setIcon(@DrawableRes int id) {
        iconView.setImageResource(id);
    }

    public void setChecked(boolean isChecked) {
        switchView.setChecked(isChecked);
    }

    public void setCheckedQuietly(boolean isChecked) {
        switchView.setOnCheckedChangeListener(null);
        switchView.setChecked(isChecked);
        switchView.setOnCheckedChangeListener(checkedChangeListener);
    }
}
