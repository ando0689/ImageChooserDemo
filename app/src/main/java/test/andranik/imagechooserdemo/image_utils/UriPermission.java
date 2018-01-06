package test.andranik.imagechooserdemo.image_utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.List;

/**
 * Created by PC-Comp on 9/30/2016.
 */

public class UriPermission {

    public static void grantUriPermission(Intent intentToQuery, Context context, Uri... fileUrisToGrant) {
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intentToQuery, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            for (int i = 0; i < fileUrisToGrant.length; i++) {
                Uri eachFileUri = fileUrisToGrant[i];
                context.grantUriPermission(packageName, eachFileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

        }
    }

    public static void revokeUriPermission(Intent intentToQuery, Context context, Uri... filesToRevokePermission) {
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intentToQuery, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;

            for (int i = 0; i < filesToRevokePermission.length; i++) {
                Uri eachFileUri = filesToRevokePermission[i];
                context.revokeUriPermission(eachFileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }
    }
}
