package com.qojqva.toolkit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.view.Surface;

import com.qojqva.entity.Permission;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author #Suyghur.
 * Created on 2021/10/29
 */
public class PermissionKit {
    /**
     * Android 命名空间
     */
    public static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    /**
     * 是否是 Android 12 及以上版本
     */
    @SuppressWarnings("all")
    public static boolean isAndroid12() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    /**
     * 是否是 Android 11 及以上版本
     */
    public static boolean isAndroid11() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    /**
     * 是否是 Android 10 及以上版本
     */
    public static boolean isAndroid10() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    /**
     * 是否是 Android 9.0 及以上版本
     */
    @SuppressWarnings("all")
    public static boolean isAndroid9() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    /**
     * 是否是 Android 8.0 及以上版本
     */
    public static boolean isAndroid8() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /**
     * 是否是 Android 7.0 及以上版本
     */
    public static boolean isAndroid7() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    /**
     * 是否是 Android 6.0 及以上版本
     */
    public static boolean isAndroid6() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * 返回应用程序在清单文件中注册的权限
     */
    public static HashMap<String, Integer> getManifestPermissions(Context context) {
        HashMap<String, Integer> manifestPermissions = new HashMap<>();

        XmlResourceParser parser = parseAndroidManifest(context);

        if (parser != null) {
            try {

                do {
                    // 当前节点必须为标签头部
                    if (parser.getEventType() != XmlResourceParser.START_TAG) {
                        continue;
                    }

                    // 当前标签必须为 uses-permission
                    if (!"uses-permission".equals(parser.getName())) {
                        continue;
                    }

                    manifestPermissions.put(parser.getAttributeValue(ANDROID_NAMESPACE, "name"),
                            parser.getAttributeIntValue(ANDROID_NAMESPACE, "maxSdkVersion", Integer.MAX_VALUE));

                } while (parser.next() != XmlResourceParser.END_DOCUMENT);

            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
            } finally {
                parser.close();
            }
        }

        if (manifestPermissions.isEmpty()) {
            try {
                // 当清单文件没有注册任何权限的时候，那么这个数组对象就是空的
                // https://github.com/getActivity/XXPermissions/issues/35
                String[] requestedPermissions = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), PackageManager.GET_PERMISSIONS).requestedPermissions;
                if (requestedPermissions != null) {
                    for (String permission : requestedPermissions) {
                        manifestPermissions.put(permission, Integer.MAX_VALUE);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        return manifestPermissions;
    }

    /**
     * 是否有存储权限
     */
    public static boolean isGrantedStoragePermission(Context context) {
        if (isAndroid11()) {
            return Environment.isExternalStorageManager();
        }
        return isGrantedPermissions(context, asArrayList(Permission.Group.STORAGE));
    }

    /**
     * 是否有安装权限
     */
    public static boolean isGrantedInstallPermission(Context context) {
        if (isAndroid8()) {
            return context.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }

    /**
     * 是否有悬浮窗权限
     */
    public static boolean isGrantedWindowPermission(Context context) {
        if (isAndroid6()) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    /**
     * 是否有通知栏权限
     */
    @SuppressWarnings("ConstantConditions")
    public static boolean isGrantedNotifyPermission(Context context) {
        if (isAndroid7()) {
            return context.getSystemService(NotificationManager.class).areNotificationsEnabled();
        }

        if (isAndroid6()) {
            // 参考 Support 库中的方法： NotificationManagerCompat.from(context).areNotificationsEnabled()
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            try {
                Method method = appOps.getClass().getMethod("checkOpNoThrow",
                        Integer.TYPE, Integer.TYPE, String.class);
                Field field = appOps.getClass().getDeclaredField("OP_POST_NOTIFICATION");
                int value = (int) field.get(Integer.class);
                return ((int) method.invoke(appOps, value, context.getApplicationInfo().uid,
                        context.getPackageName())) == AppOpsManager.MODE_ALLOWED;
            } catch (NoSuchMethodException | NoSuchFieldException | InvocationTargetException |
                    IllegalAccessException | RuntimeException e) {
                e.printStackTrace();
                return true;
            }
        }

        return true;
    }

    /**
     * 是否有系统设置权限
     */
    public static boolean isGrantedSettingPermission(Context context) {
        if (isAndroid6()) {
            return Settings.System.canWrite(context);
        }
        return true;
    }

    /**
     * 判断某个权限集合是否包含特殊权限
     */
    public static boolean containsSpecialPermission(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        for (String permission : permissions) {
            if (isSpecialPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断某个权限是否是特殊权限
     */
    public static boolean isSpecialPermission(String permission) {
        return Permission.MANAGE_EXTERNAL_STORAGE.equals(permission) ||
                Permission.REQUEST_INSTALL_PACKAGES.equals(permission) ||
                Permission.SYSTEM_ALERT_WINDOW.equals(permission) ||
                Permission.NOTIFICATION_SERVICE.equals(permission) ||
                Permission.WRITE_SETTINGS.equals(permission);
    }

    /**
     * 判断某些权限是否全部被授予
     */
    public static boolean isGrantedPermissions(Context context, List<String> permissions) {
        // 如果是安卓 6.0 以下版本就直接返回 true
        if (!isAndroid6()) {
            return true;
        }

        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        for (String permission : permissions) {
            if (!isGrantedPermission(context, permission)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取没有授予的权限
     */
    public static List<String> getDeniedPermissions(Context context, List<String> permissions) {
        List<String> deniedPermission = new ArrayList<>(permissions.size());

        // 如果是安卓 6.0 以下版本就默认授予
        if (!isAndroid6()) {
            return deniedPermission;
        }

        for (String permission : permissions) {
            if (!isGrantedPermission(context, permission)) {
                deniedPermission.add(permission);
            }
        }
        return deniedPermission;
    }

    /**
     * 判断某个权限是否授予
     */
    public static boolean isGrantedPermission(Context context, String permission) {
        // 如果是安卓 6.0 以下版本就默认授予
        if (!isAndroid6()) {
            return true;
        }

        // 检测存储权限
        if (Permission.MANAGE_EXTERNAL_STORAGE.equals(permission)) {
            return isGrantedStoragePermission(context);
        }

        // 检测安装权限
        if (Permission.REQUEST_INSTALL_PACKAGES.equals(permission)) {
            return isGrantedInstallPermission(context);
        }

        // 检测悬浮窗权限
        if (Permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
            return isGrantedWindowPermission(context);
        }

        // 检测通知栏权限
        if (Permission.NOTIFICATION_SERVICE.equals(permission)) {
            return isGrantedNotifyPermission(context);
        }

        // 检测系统权限
        if (Permission.WRITE_SETTINGS.equals(permission)) {
            return isGrantedSettingPermission(context);
        }

        // 检测 Android 12 的三个新权限
        if (!isAndroid12()) {

            if (Permission.BLUETOOTH_SCAN.equals(permission)) {
                return context.checkSelfPermission(Permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            }

            if (Permission.BLUETOOTH_CONNECT.equals(permission) ||
                    Permission.BLUETOOTH_ADVERTISE.equals(permission)) {
                return true;
            }
        }

        // 检测 Android 10 的三个新权限
        if (!isAndroid10()) {

            if (Permission.ACCESS_BACKGROUND_LOCATION.equals(permission)) {
                return context.checkSelfPermission(Permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            }

            if (Permission.ACTIVITY_RECOGNITION.equals(permission)) {
                return context.checkSelfPermission(Permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED;
            }

            if (Permission.ACCESS_MEDIA_LOCATION.equals(permission)) {
                return true;
            }
        }

        // 检测 Android 9.0 的一个新权限
        if (!isAndroid9()) {

            if (Permission.ACCEPT_HANDOVER.equals(permission)) {
                return true;
            }
        }

        // 检测 Android 8.0 的两个新权限
        if (!isAndroid8()) {

            if (Permission.ANSWER_PHONE_CALLS.equals(permission)) {
                return true;
            }

            if (Permission.READ_PHONE_NUMBERS.equals(permission)) {
                return context.checkSelfPermission(Permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
            }
        }

        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 优化权限回调结果
     */
    public static void optimizePermissionResults(Activity activity, String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {

            boolean recheck = false;

            String permission = permissions[i];

            // 如果这个权限是特殊权限，那么就重新进行权限检测
            if (isSpecialPermission(permission)) {
                recheck = true;
            }

            // 重新检查 Android 12 的三个新权限
            if (!isAndroid12() && (Permission.BLUETOOTH_SCAN.equals(permission)
                    || Permission.BLUETOOTH_CONNECT.equals(permission)
                    || Permission.BLUETOOTH_ADVERTISE.equals(permission))) {
                recheck = true;
            }

            // 重新检查 Android 10.0 的三个新权限
            if (!isAndroid10() && (Permission.ACCESS_BACKGROUND_LOCATION.equals(permission)
                    || Permission.ACTIVITY_RECOGNITION.equals(permission)
                    || Permission.ACCESS_MEDIA_LOCATION.equals(permission))) {
                recheck = true;
            }

            // 重新检查 Android 9.0 的一个新权限
            if (!isAndroid9() && Permission.ACCEPT_HANDOVER.equals(permission)) {
                recheck = true;
            }

            // 重新检查 Android 8.0 的两个新权限
            if (!isAndroid8() && (Permission.ANSWER_PHONE_CALLS.equals(permission)
                    || Permission.READ_PHONE_NUMBERS.equals(permission))) {
                recheck = true;
            }

            if (recheck) {
                grantResults[i] = isGrantedPermission(activity, permission) ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED;
            }
        }
    }

    /**
     * 在权限组中检查是否有某个权限是否被永久拒绝
     *
     * @param activity    Activity对象
     * @param permissions 请求的权限
     */
    public static boolean isPermissionPermanentDenied(Activity activity, List<String> permissions) {
        for (String permission : permissions) {
            if (isPermissionPermanentDenied(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断某个权限是否被永久拒绝
     *
     * @param activity   Activity对象
     * @param permission 请求的权限
     */
    public static boolean isPermissionPermanentDenied(Activity activity, String permission) {
        if (!isAndroid6()) {
            return false;
        }

        // 特殊权限不算，本身申请方式和危险权限申请方式不同，因为没有永久拒绝的选项，所以这里返回 false
        if (isSpecialPermission(permission)) {
            return false;
        }

        // 检测 Android 12 的三个新权限
        if (!isAndroid12()) {

            if (Permission.BLUETOOTH_SCAN.equals(permission)) {
                return !isGrantedPermission(activity, Permission.ACCESS_COARSE_LOCATION) && !activity.shouldShowRequestPermissionRationale(Permission.ACCESS_COARSE_LOCATION);
            }

            if (Permission.BLUETOOTH_CONNECT.equals(permission) ||
                    Permission.BLUETOOTH_ADVERTISE.equals(permission)) {
                return false;
            }
        }

        if (isAndroid10()) {

            // 重新检测后台定位权限是否永久拒绝
            if (Permission.ACCESS_BACKGROUND_LOCATION.equals(permission) && !isGrantedPermission(activity, Permission.ACCESS_BACKGROUND_LOCATION) && !isGrantedPermission(activity, Permission.ACCESS_FINE_LOCATION)) {
                return !activity.shouldShowRequestPermissionRationale(Permission.ACCESS_FINE_LOCATION);
            }
        }

        // 检测 Android 10 的三个新权限
        if (!isAndroid10()) {

            if (Permission.ACCESS_BACKGROUND_LOCATION.equals(permission)) {
                return !isGrantedPermission(activity, Permission.ACCESS_FINE_LOCATION) && !activity.shouldShowRequestPermissionRationale(Permission.ACCESS_FINE_LOCATION);
            }

            if (Permission.ACTIVITY_RECOGNITION.equals(permission)) {
                return !isGrantedPermission(activity, Permission.BODY_SENSORS) && !activity.shouldShowRequestPermissionRationale(Permission.BODY_SENSORS);
            }

            if (Permission.ACCESS_MEDIA_LOCATION.equals(permission)) {
                return false;
            }
        }

        // 检测 Android 9.0 的一个新权限
        if (!isAndroid9()) {
            if (Permission.ACCEPT_HANDOVER.equals(permission)) {
                return false;
            }
        }

        // 检测 Android 8.0 的两个新权限
        if (!isAndroid8()) {
            if (Permission.ANSWER_PHONE_CALLS.equals(permission)) {
                return false;
            }
            if (Permission.READ_PHONE_NUMBERS.equals(permission)) {
                return !isGrantedPermission(activity, Permission.READ_PHONE_STATE) && !activity.shouldShowRequestPermissionRationale(Permission.READ_PHONE_STATE);
            }
        }

        return !isGrantedPermission(activity, permission) && !activity.shouldShowRequestPermissionRationale(permission);
    }

    /**
     * 获取没有授予的权限
     *
     * @param permissions  需要请求的权限组
     * @param grantResults 允许结果组
     */
    public static List<String> getDeniedPermissions(String[] permissions, int[] grantResults) {
        List<String> deniedPermissions = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            // 把没有授予过的权限加入到集合中
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                deniedPermissions.add(permissions[i]);
            }
        }
        return deniedPermissions;
    }

    /**
     * 获取已授予的权限
     *
     * @param permissions  需要请求的权限组
     * @param grantResults 允许结果组
     */
    public static List<String> getGrantedPermissions(String[] permissions, int[] grantResults) {
        List<String> grantedPermissions = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            // 把授予过的权限加入到集合中
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(permissions[i]);
            }
        }
        return grantedPermissions;
    }

    /**
     * 将数组转换成 ArrayList
     * <p>
     * 这里解释一下为什么不用 Arrays.asList
     * 第一是返回的类型不是 java.util.ArrayList 而是 java.util.Arrays.ArrayList
     * 第二是返回的 ArrayList 对象是只读的，也就是不能添加任何元素，否则会抛异常
     */
    @SuppressWarnings("all")
    public static <T> ArrayList<T> asArrayList(T... array) {
        if (array == null || array.length == 0) {
            return null;
        }
        ArrayList<T> list = new ArrayList<>(array.length);
        for (T t : array) {
            list.add(t);
        }
        return list;
    }

    @SafeVarargs
    public static <T> ArrayList<T> asArrayLists(T[]... arrays) {
        ArrayList<T> list = new ArrayList<>();
        if (arrays == null || arrays.length == 0) {
            return list;
        }
        for (T[] ts : arrays) {
            list.addAll(asArrayList(ts));
        }
        return list;
    }

    /**
     * 寻找上下文中的 Activity 对象
     */
    public static FragmentActivity findActivity(Context context) {
        do {
            if (context instanceof FragmentActivity) {
                return (FragmentActivity) context;
            } else if (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
            } else {
                return null;
            }
        } while (context != null);
        return null;
    }

    /**
     * 获取当前应用 Apk 在 AssetManager 中的 Cookie
     */
    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("PrivateApi")
    public static Integer findApkPathCookie(Context context) {
        AssetManager assets = context.getAssets();
        String path = context.getApplicationInfo().sourceDir;
        try {
            // 为什么不直接通过反射 AssetManager.findCookieForPath 方法来判断？因为这个 API 属于反射黑名单，反射执行不了
            // 为什么不直接通过反射 AssetManager.addAssetPathInternal 这个非隐藏的方法来判断？因为这个也反射不了
            Method method = assets.getClass().getDeclaredMethod("addOverlayPath", String.class);
            // Android 9.0 以下获取到的结果会为零
            // Android 9.0 及以上获取到的结果会大于零
            return (Integer) method.invoke(assets, path);
        } catch (Exception e) {
            // NoSuchMethodException
            // IllegalAccessException
            // InvocationTargetException
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析清单文件
     */
    public static XmlResourceParser parseAndroidManifest(Context context) {
        Integer cookie = findApkPathCookie(context);
        if (cookie == null) {
            // 如果 cookie 为 null，证明获取失败，直接 return
            return null;
        }

        try {
            return context.getAssets().openXmlResourceParser(cookie, "AndroidManifest.xml");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 判断是否适配了分区存储
     */
    public static boolean isScopedStorage(Context context) {
        try {
            String metaKey = "ScopedStorage";
            Bundle metaData = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            if (metaData != null && metaData.containsKey(metaKey)) {
                return Boolean.parseBoolean(String.valueOf(metaData.get(metaKey)));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断 Activity 是否反方向旋转了
     */
    public static boolean isActivityReverse(Activity activity) {
        // 获取 Activity 旋转的角度
        int activityRotation;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activityRotation = activity.getDisplay().getRotation();
        } else {
            activityRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        }
        switch (activityRotation) {
            case Surface.ROTATION_180:
            case Surface.ROTATION_270:
                return true;
            case Surface.ROTATION_0:
            case Surface.ROTATION_90:
            default:
                return false;
        }
    }
}
