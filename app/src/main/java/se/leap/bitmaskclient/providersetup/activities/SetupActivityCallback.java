package se.leap.bitmaskclient.providersetup.activities;

import androidx.viewpager2.widget.ViewPager2;

import se.leap.bitmaskclient.base.models.Provider;

public interface SetupActivityCallback {

    void onSetupStepValidationChanged(boolean isValid);
     void registerOnPageChangeCallback(ViewPager2.OnPageChangeCallback callback);
     void removeOnPageChangeCallback(ViewPager2.OnPageChangeCallback callback);

     void registerCancelCallback(CancelCallback cancelCallback);

     void removeCancelCallback(CancelCallback cancelCallback);

     void setNavigationButtonHidden(boolean isHidden);

     void setCancelButtonHidden(boolean isHidden);

     void onProviderSelected(Provider provider);

     void onConfigurationSuccess();

     Provider getSelectedProvider();

     int getCurrentPosition();

     void onSetupFinished();

     void onError(String reasonToFail);
}

