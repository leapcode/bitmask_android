/**
 * Copyright (c) 2021 LEAP Encryption Access Project and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.leap.bitmaskclient.base.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import se.leap.bitmaskclient.R;

public class IconSwitchEntry extends LinearLayout {

    private TextView textView;
    private TextView subtitleView;
    private AppCompatImageView iconView;
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
        textView = rootview.findViewById(R.id.title);
        subtitleView = rootview.findViewById(R.id.subtitle);
        iconView = rootview.findViewById(R.id.material_icon);
        switchView = rootview.findViewById(R.id.option_switch);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.IconSwitchEntry);

            String entryText = typedArray.getString(R.styleable.IconTextEntry_text);
            if (entryText != null) {
                textView.setText(entryText);
            }

            String subtitle = typedArray.getString(R.styleable.IconTextEntry_subtitle);
            if (subtitle != null) {
                subtitleView.setText(subtitle);
                subtitleView.setVisibility(VISIBLE);
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

    public void setSubtitle(CharSequence text) {
        subtitleView.setText(text);
    }

    public void setSingleLine(boolean singleLine) {
        textView.setSingleLine(singleLine);
        subtitleView.setSingleLine(singleLine);
    }

    public void showSubtitle(boolean show) {
        subtitleView.setVisibility(show ? VISIBLE : GONE);
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

    public boolean isChecked() {
        return switchView.isChecked();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        switchView.setVisibility(enabled ? VISIBLE : GONE);
        textView.setTextColor(getResources().getColor(enabled ? android.R.color.black : R.color.colorDisabled));
        iconView.setImageAlpha(enabled ? 255 : 128);
    }
}
