package se.leap.bitmaskclient.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import butterknife.BindView;
import butterknife.ButterKnife;
import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.fragments.TetheringDialog;


public class IconCheckboxEntry extends LinearLayout {

    @BindView(android.R.id.text1)
    TextView textView;

    @BindView(R.id.material_icon)
    AppCompatImageView iconView;

    @BindView(R.id.checked_icon)
    AppCompatImageView checkedIcon;

    public IconCheckboxEntry(Context context) {
        super(context);
        initLayout(context, null);
    }

    public IconCheckboxEntry(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initLayout(context, attrs);
    }

    public IconCheckboxEntry(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context, attrs);
    }

    @TargetApi(21)
    public IconCheckboxEntry(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initLayout(context, attrs);
    }

    void initLayout(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootview = inflater.inflate(R.layout.v_icon_select_text_list_item, this, true);
        ButterKnife.bind(this, rootview);
    }

    public void bind(TetheringDialog.DialogListAdapter.ViewModel model) {
        this.setEnabled(model.enabled);
        textView.setText(model.text);
        textView.setEnabled(model.enabled);

        Drawable checkIcon = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_check_bold)).mutate();
        if (model.enabled) {
            DrawableCompat.setTint(checkIcon, ContextCompat.getColor(getContext(), R.color.colorSuccess));
        } else {
            DrawableCompat.setTint(checkIcon, ContextCompat.getColor(getContext(), R.color.colorDisabled));
        }

        iconView.setImageDrawable(model.image);
        checkedIcon.setImageDrawable(checkIcon);
        setChecked(model.checked);
    }

    public void setChecked(boolean checked) {
        checkedIcon.setVisibility(checked ? VISIBLE : GONE);
        checkedIcon.setContentDescription(checked ? "selected" : "unselected");
    }

}
