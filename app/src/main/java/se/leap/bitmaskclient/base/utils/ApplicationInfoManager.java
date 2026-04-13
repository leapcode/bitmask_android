package se.leap.bitmaskclient.base.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.WorkerThread;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class ApplicationInfoManager {

    public final static String TAG = ApplicationInfoManager.class.getSimpleName();
    final PackageManager packageManager;
    public ApplicationInfoManager(Context context) {
        packageManager = context.getPackageManager();
    }

    @WorkerThread
    public Vector<ApplicationInfo> getApplicationInfos() {
        List<ApplicationInfo> installedPackages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        // Remove apps not using Internet

        int androidSystemUid = 0;
        ApplicationInfo system = null;
        Vector<ApplicationInfo> apps = new Vector<>();

        try {
            system = packageManager.getApplicationInfo("android", PackageManager.GET_META_DATA);
            androidSystemUid = system.uid;
            apps.add(system);
        } catch (PackageManager.NameNotFoundException e) {
        }


        for (ApplicationInfo app : installedPackages) {

            if (packageManager.checkPermission(Manifest.permission.INTERNET, app.packageName) == PackageManager.PERMISSION_GRANTED &&
                    app.uid != androidSystemUid) {

                apps.add(app);
            }
        }

        Collections.sort(apps, new ApplicationInfo.DisplayNameComparator(packageManager));
        return apps;
    }

    public CharSequence loadLabel(ApplicationInfo appInfo) {
        return appInfo.loadLabel(packageManager);
    }

    public Drawable loadDrawable(ApplicationInfo appInfo) {
        return appInfo.loadIcon(packageManager);
    }

    public void onApplicationRemoved(String appPackage) {
        Set<String> excludedApps = PreferenceHelper.getExcludedApps();
        excludedApps.remove(appPackage);
        PreferenceHelper.setExcludedApps(excludedApps);
    }

    @WorkerThread
    public void updateExcludedApps() {
        Set<String> excludedApps = PreferenceHelper.getExcludedApps();
        List<ApplicationInfo> appInfos = getApplicationInfos();
        excludedApps.removeIf(s ->
                appInfos.stream().noneMatch(
                        applicationInfo -> s.equals(applicationInfo.packageName)
                )
        );
        PreferenceHelper.setExcludedApps(excludedApps);
    }
}
