/**
 * Copyright (c) 2018 LEAP Encryption Access Project and contributers
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

import android.content.Context;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ProgressBar;

import se.leap.bitmaskclient.R;

/**
 * Created by cyberta on 12.02.18.
 */


public class VpnStateImage extends ConstraintLayout {

    ProgressBar progressBar;
    AppCompatImageView stateIcon;

    public VpnStateImage(Context context) {
        super(context);
        initLayout(context);
    }

    public VpnStateImage(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public VpnStateImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }

    void initLayout(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootview = inflater.inflate(R.layout.v_main_button, this, true);
        stateIcon = rootview.findViewById(R.id.vpn_state_key);
        progressBar = rootview.findViewById(R.id.progressBar);
        progressBar.setIndeterminate(true);
    }

    public void showProgress() {
        progressBar.setVisibility(VISIBLE);
    }


    public void stopProgress(boolean animated) {
        if (!animated) {
            progressBar.setVisibility(GONE);
            return;
        }

        AlphaAnimation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
        fadeOutAnimation.setDuration(1000);
        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                progressBar.setVisibility(GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        
        progressBar.startAnimation(fadeOutAnimation);
  }

    public void setStateIcon(int resource) {
        stateIcon.setImageResource(resource);
    }


}
