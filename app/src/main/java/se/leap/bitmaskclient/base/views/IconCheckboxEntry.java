package se.leap.bitmaskclient.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import se.leap.bitmaskclient.R;
import se.leap.bitmaskclient.base.fragments.TetheringDialog;
import se.leap.bitmaskclient.databinding.VIconSelectTextListItemBinding;


public class IconCheckboxEntry extends LinearLayout {

    VIconSelectTextListItemBinding binding;

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
        binding = VIconSelectTextListItemBinding.inflate(inflater, this, true);
    }

    public void bind(TetheringDialog.DialogListAdapter.ViewModel model) {
        this.setEnabled(model.enabled);
        binding.text1.setText(model.text);
        binding.text1.setEnabled(model.enabled);

        Drawable checkIcon = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_check_bold)).mutate();
        if (model.enabled) {
            DrawableCompat.setTint(checkIcon, ContextCompat.getColor(getContext(), R.color.colorSuccess));
        } else {
            DrawableCompat.setTint(checkIcon, ContextCompat.getColor(getContext(), R.color.colorDisabled));
        }

        binding.materialIcon.setImageDrawable(model.image);
        binding.checkedIcon.setImageDrawable(checkIcon);
        setChecked(model.checked);
    }

    public void setChecked(boolean checked) {
        binding.checkedIcon.setVisibility(checked ? VISIBLE : GONE);
        binding.checkedIcon.setContentDescription(checked ? "selected" : "unselected");
    }

}
