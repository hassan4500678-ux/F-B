package com.fb.erp;

import android.app.Activity;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.widget.Toast;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Shows the bundled ERP page (assets/index.html). All data stays on the phone. */
public class MainActivity extends Activity {
    static final String BASE = "https://github.com/hassan4500678-ux/F-B/releases/download/erp-latest/";
    static final String VERSION_URL = BASE + "version.json";
    static final String APK_URL = BASE + "FB-ERP.apk";
    private static final int PICK_PHOTO = 7;
    private static final int REQ_LOCATION = 21;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    private int latestCode = 0;
    private String latestName = "";
    private int downloadedCode = 0;
    private volatile boolean downloading = false;
    private boolean waitingForPermission = false;

    // GPS
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location bestFix;
    private boolean locating = false;
    private boolean quickFix = false;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#F4F6FA"));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);   // keeps employees and attendance
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        web.addJavascriptInterface(new Bridge(), "ErpApp");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("file:")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) { }
                return true;
            }
        });

        // Lets "Upload photo" open the phone's gallery.
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                String type = "*/*";
                try {
                    String[] acc = params.getAcceptTypes();
                    if (acc != null) for (String t : acc) if (t != null && t.startsWith("image")) type = "image/*";
                } catch (Exception ignored) { }
                pick.setType(type);
                try {
                    startActivityForResult(Intent.createChooser(pick, "image/*".equals(type) ? "Select photo" : "Select backup file"), PICK_PHOTO);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/index.html");
    }

    /** Called from the page as window.ErpApp. */
    class Bridge {
        @JavascriptInterface
        public String info() {
            try {
                JSONObject o = new JSONObject();
                o.put("versionName", versionName());
                o.put("versionCode", versionCode());
                return o.toString();
            } catch (Exception e) {
                return "{}";
            }
        }

        @JavascriptInterface
        public void checkUpdate() { checkForUpdate(); }

        @JavascriptInterface
        public void requestLocation() { runOnUiThread(new Runnable() { public void run() { quickFix = false; startLocation(); } }); }

        /** Faster, less precise fix used by the Daily Form for the area map. */
        @JavascriptInterface
        public void requestLocationQuick() { runOnUiThread(new Runnable() { public void run() { quickFix = true; startLocation(); } }); }

        @JavascriptInterface
        public void shareText(final String text) {
            runOnUiThread(new Runnable() { public void run() {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, text);
                try { startActivity(Intent.createChooser(i, "Share report")); } catch (Exception ignored) { }
            } });
        }

        @JavascriptInterface
        public void shareImage(final String base64, final String name) {
            new Thread(new Runnable() { public void run() {
                try {
                    File dir = new File(getCacheDir(), "share");
                    dir.mkdirs();
                    File f = new File(dir, safeName(name));
                    FileOutputStream fo = new FileOutputStream(f);
                    fo.write(Base64.decode(base64, Base64.DEFAULT));
                    fo.close();
                    final Uri uri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".files", f);
                    runOnUiThread(new Runnable() { public void run() {
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("image/png");
                        i.putExtra(Intent.EXTRA_STREAM, uri);
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try { startActivity(Intent.createChooser(i, "Share report card")); } catch (Exception ignored) { }
                    } });
                } catch (Exception e) {
                    toast("Could not share the image");
                }
            } }).start();
        }

        /** Shares a text file (used for data backups). */
        @JavascriptInterface
        public void shareFile(final String text, final String name) {
            new Thread(new Runnable() { public void run() {
                try {
                    File dir = new File(getCacheDir(), "share");
                    dir.mkdirs();
                    String n = (name == null ? "backup.json" : name).replaceAll("[^A-Za-z0-9._-]", "_");
                    File f = new File(dir, n);
                    FileOutputStream fo = new FileOutputStream(f);
                    fo.write(text.getBytes("UTF-8"));
                    fo.close();
                    final Uri uri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".files", f);
                    runOnUiThread(new Runnable() { public void run() {
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("application/json");
                        i.putExtra(Intent.EXTRA_STREAM, uri);
                        i.putExtra(Intent.EXTRA_SUBJECT, "FB ERP backup");
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try { startActivity(Intent.createChooser(i, "Save backup")); } catch (Exception ignored) { }
                    } });
                } catch (Exception e) {
                    toast("Could not create the backup");
                }
            } }).start();
        }

        /** Saves to Pictures/FB ERP (Android 10+); older phones get the share sheet. */
        @JavascriptInterface
        public void saveImage(final String base64, final String name) {
            if (Build.VERSION.SDK_INT < 29) { shareImage(base64, name); return; }
            new Thread(new Runnable() { public void run() {
                try {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Images.Media.DISPLAY_NAME, safeName(name));
                    v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                    v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FB ERP");
                    Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    os.write(Base64.decode(base64, Base64.DEFAULT));
                    os.close();
                    toast("Saved to Pictures / FB ERP");
                } catch (Exception e) {
                    toast("Could not save the image");
                }
            } }).start();
        }

        @JavascriptInterface
        public void installUpdate() { runOnUiThread(new Runnable() { public void run() { startUpdate(); } }); }
    }

    // ---------------------------------------------------------------- Update

    int versionCode() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode; } catch (Exception e) { return 0; }
    }

    String versionName() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception e) { return ""; }
    }

    /** Sends {status:...} to the page's update banner. */
    void push(String status, int pct) {
        try {
            JSONObject o = new JSONObject();
            o.put("status", status);
            o.put("pct", pct);
            o.put("latestName", latestName);
            final String js = "window.erpUpdate&&window.erpUpdate(" + o.toString() + ")";
            runOnUiThread(new Runnable() { public void run() { web.evaluateJavascript(js, null); } });
        } catch (Exception ignored) { }
    }

    static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        return bo.toByteArray();
    }

    static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setInstanceFollowRedirects(true);
        c.setUseCaches(false);
        c.setRequestProperty("User-Agent", "FB-ERP-App");
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        return c;
    }

    void checkForUpdate() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    HttpURLConnection c = open(VERSION_URL + "?t=" + System.currentTimeMillis());
                    if (c.getResponseCode() != 200) { push("error", 0); return; }
                    JSONObject o = new JSONObject(new String(readAll(c.getInputStream()), "UTF-8"));
                    c.disconnect();
                    latestCode = o.optInt("versionCode", 0);
                    latestName = o.optString("versionName", "");
                    push(latestCode > versionCode() ? "available" : "none", 0);
                } catch (Exception e) {
                    push("error", 0);
                }
            }
        }).start();
    }

    File apkFile() {
        File dir = new File(getCacheDir(), "update");
        dir.mkdirs();
        return new File(dir, "FB-ERP.apk");
    }

    void startUpdate() {
        if (downloading) return;
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            waitingForPermission = true;
            push("permission", 0);
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) { }
            return;
        }
        if (downloadedCode > 0 && downloadedCode == latestCode && apkFile().exists()) {
            launchInstaller();
            return;
        }
        downloading = true;
        push("downloading", 0);
        new Thread(new Runnable() {
            public void run() {
                File out = apkFile();
                File tmp = new File(out.getParentFile(), "download.tmp");
                try {
                    HttpURLConnection c = open(APK_URL + "?t=" + System.currentTimeMillis());
                    if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
                    int total = c.getContentLength();
                    InputStream in = c.getInputStream();
                    FileOutputStream fo = new FileOutputStream(tmp);
                    byte[] buf = new byte[16384];
                    int n, done = 0, last = -1;
                    while ((n = in.read(buf)) > 0) {
                        fo.write(buf, 0, n);
                        done += n;
                        if (total > 0) {
                            int pct = (int) (done * 100L / total);
                            if (pct != last) { last = pct; push("downloading", pct); }
                        }
                    }
                    fo.getFD().sync();
                    fo.close();
                    in.close();
                    c.disconnect();
                    if (done < 50000 || (total > 0 && done != total)) throw new Exception("incomplete");
                    if (out.exists()) out.delete();
                    if (!tmp.renameTo(out)) throw new Exception("rename");
                    downloadedCode = latestCode;
                    downloading = false;
                    push("ready", 100);
                    runOnUiThread(new Runnable() { public void run() { launchInstaller(); } });
                } catch (Exception e) {
                    downloading = false;
                    tmp.delete();
                    push("failed", 0);
                }
            }
        }).start();
    }

    void launchInstaller() {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", apkFile());
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            push("ready", 100);
        } catch (Exception e) {
            // Fallback: let the browser download it.
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(APK_URL))); } catch (Exception ignored) { }
        }
    }

    static String safeName(String n) {
        String s = (n == null ? "image.png" : n).replaceAll("[^A-Za-z0-9._-]", "_");
        return s.endsWith(".png") ? s : s + ".png";
    }

    void toast(final String msg) {
        runOnUiThread(new Runnable() { public void run() { Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show(); } });
    }

    // ---------------------------------------------------------------- GPS

    boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT < 23) return true;
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    void sendLocation(String json) {
        final String js = "window.erpLocation&&window.erpLocation(" + json + ")";
        runOnUiThread(new Runnable() { public void run() { web.evaluateJavascript(js, null); } });
    }

    void sendLocationError(String msg) {
        try {
            JSONObject o = new JSONObject();
            o.put("ok", false);
            o.put("error", msg);
            sendLocation(o.toString());
        } catch (Exception ignored) { }
    }

    void startLocation() {
        if (locating) return;
        if (!hasLocationPermission()) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            return;
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps = false, net = false;
        try { gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER); } catch (Exception ignored) { }
        try { net = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER); } catch (Exception ignored) { }
        if (!gps && !net) {
            sendLocationError("Location (GPS) is turned off. Please turn it on and try again.");
            if (!quickFix) try { startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); } catch (Exception ignored) { }
            return;
        }
        locating = true;
        bestFix = null;
        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location loc) {
                if (loc == null) return;
                if (bestFix == null || loc.getAccuracy() < bestFix.getAccuracy()) bestFix = loc;
                if (loc.getAccuracy() <= (quickFix ? 60 : 25)) finishLocation();
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override public void onProviderEnabled(String provider) { }
            @Override public void onProviderDisabled(String provider) { }
        };
        try {
            if (gps) locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, Looper.getMainLooper());
            if (net) locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener, Looper.getMainLooper());
        } catch (SecurityException e) {
            locating = false;
            sendLocationError("Location permission is required for check in.");
            return;
        }
        // Use the best fix found in time (8 s quick, 20 s normal).
        handler.postDelayed(new Runnable() { public void run() { finishLocation(); } }, quickFix ? 8000 : 20000);
    }

    void finishLocation() {
        if (!locating) return;
        locating = false;
        handler.removeCallbacksAndMessages(null);
        try { locationManager.removeUpdates(locationListener); } catch (Exception ignored) { }
        Location loc = bestFix;
        if (loc == null) {
            sendLocationError("Could not get your location. Move near a window or outside and try again.");
            return;
        }
        try {
            boolean mock = Build.VERSION.SDK_INT >= 31 ? loc.isMock() : (Build.VERSION.SDK_INT >= 18 && loc.isFromMockProvider());
            JSONObject o = new JSONObject();
            o.put("ok", true);
            o.put("lat", loc.getLatitude());
            o.put("lng", loc.getLongitude());
            o.put("acc", loc.getAccuracy());
            o.put("mock", mock);
            sendLocation(o.toString());
        } catch (Exception e) {
            sendLocationError("Could not read your location.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != REQ_LOCATION) return;
        if (hasLocationPermission()) startLocation();
        else if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            sendLocationError("Please allow Precise location for FB ERP (phone Settings > Apps > FB ERP > Location).");
        else sendLocationError("Location permission was denied. Allow location for FB ERP in phone Settings > Apps.");
    }

    // ---------------------------------------------------------------- Activity

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request == PICK_PHOTO && fileCallback != null) {
            Uri[] picked = null;
            if (result == RESULT_OK && data != null && data.getData() != null) picked = new Uri[]{data.getData()};
            fileCallback.onReceiveValue(picked);
            fileCallback = null;
            return;
        }
        super.onActivityResult(request, result, data);
    }

    // Back closes the menu or returns to the dashboard before leaving the app.
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.erpBack && window.erpBack()) ? 'yes' : 'no'", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String handled) {
                if (handled == null || !handled.contains("yes")) finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        if (waitingForPermission && (Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls())) {
            waitingForPermission = false;
            startUpdate();
        }
    }
}
