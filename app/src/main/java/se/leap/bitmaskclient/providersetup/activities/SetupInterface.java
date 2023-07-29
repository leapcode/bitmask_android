package se.leap.bitmaskclient.providersetup.activities;

import androidx.viewpager2.widget.ViewPager2;

import se.leap.bitmaskclient.base.models.Provider;

public interface SetupInterface {

    void onSetupStepValidationChanged(boolean isValid);
     void registerOnPageChangeCallback(ViewPager2.OnPageChangeCallback callback);
     void removeOnPageChangeCallback(ViewPager2.OnPageChangeCallback callback);
     void setNavigationButtonHidden(boolean isHidden);
     void onCanceled();

     void onProviderSelected(Provider provider);

     Provider getSelectedProvider();

}

