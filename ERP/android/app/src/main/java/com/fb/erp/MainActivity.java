package com.fb.erp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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
import java.net.HttpURLConnection;
import java.net.URL;

/** Shows the bundled ERP page (assets/index.html). All data stays on the phone. */
public class MainActivity extends Activity {
    static final String BASE = "https://github.com/hassan4500678-ux/F-B/releases/download/erp-latest/";
    static final String VERSION_URL = BASE + "version.json";
    static final String APK_URL = BASE + "FB-ERP.apk";
    private static final int PICK_PHOTO = 7;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    private int latestCode = 0;
    private String latestName = "";
    private int downloadedCode = 0;
    private volatile boolean downloading = false;
    private boolean waitingForPermission = false;

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
                pick.setType("image/*");
                try {
                    startActivityForResult(Intent.createChooser(pick, "Select photo"), PICK_PHOTO);
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
