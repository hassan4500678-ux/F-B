package com.faizanbrothers.claims;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * FB Claims — the Faizan & Brothers claim management system as a standalone
 * Android app. The page (assets/index.html) is built from the Claude artifact
 * source; all data is stored on this phone.
 */
public class MainActivity extends Activity {

    static final String RELEASES_API = "https://api.github.com/repos/hassan4500678-ux/F-B/releases/latest";
    static final String RELEASES_PAGE = "https://github.com/hassan4500678-ux/F-B/releases/latest";
    static final String DATA_FILE = "claims-data.json";
    static final int PICK_BACKUP = 51;

    WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        web.setBackgroundColor(0xFFEEF1F6);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }
        });
        setContentView(web);
        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl("file:///android_asset/index.html");
        checkForUpdate();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (web != null) web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        // Close an open form/report first, then go back through the tabs, then exit.
        web.evaluateJavascript(
                "(function(){var m=document.getElementById('modal');" +
                "if(m&&m.innerHTML.trim()){var b=m.querySelector('button[data-close]');if(b){b.click();return 'closed';}}" +
                "return 'none';})()",
                result -> {
                    if (result != null && result.contains("closed")) return;
                    if (web.canGoBack()) web.goBack();
                    else finish();
                });
    }

    void toast(final String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }

    // ---------------------------------------------------------------- Storage

    File dataFile() { return new File(getFilesDir(), DATA_FILE); }

    static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        return bo.toByteArray();
    }

    static void writeAll(File f, byte[] data) throws Exception {
        FileOutputStream fo = new FileOutputStream(f);
        try {
            fo.write(data);
            fo.getFD().sync();
        } finally {
            fo.close();
        }
    }

    String mimeFor(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".json")) return "application/json";
        if (n.endsWith(".csv")) return "text/csv";
        return "application/octet-stream";
    }

    /** Called from the page. */
    class Bridge {
        @JavascriptInterface
        public String loadData() {
            try {
                File f = dataFile();
                if (!f.exists()) {
                    File prev = new File(getFilesDir(), DATA_FILE + ".bak");
                    if (!prev.exists()) return null;
                    f = prev;
                }
                return new String(readAll(new FileInputStream(f)), "UTF-8");
            } catch (Exception e) {
                return null;
            }
        }

        @JavascriptInterface
        public synchronized String saveData(String json) {
            try {
                File f = dataFile();
                File tmp = new File(getFilesDir(), DATA_FILE + ".tmp");
                writeAll(tmp, json.getBytes("UTF-8"));
                if (f.exists()) {
                    File bak = new File(getFilesDir(), DATA_FILE + ".bak");
                    if (bak.exists()) bak.delete();
                    f.renameTo(bak);
                }
                if (!tmp.renameTo(f)) return "rename failed";
                return "ok";
            } catch (Exception e) {
                return String.valueOf(e.getMessage());
            }
        }

        @JavascriptInterface
        public String saveFile(String name, String base64) {
            try {
                String safe = name.replaceAll("[^A-Za-z0-9._ -]", "-");
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                String mime = mimeFor(safe);

                // Copy kept for sharing on WhatsApp / opening.
                File dir = new File(getCacheDir(), "share");
                dir.mkdirs();
                File shareCopy = new File(dir, safe);
                writeAll(shareCopy, bytes);

                String where;
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentResolver cr = getContentResolver();
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.MediaColumns.DISPLAY_NAME, safe);
                    v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                    v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FB Claims");
                    Uri u = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (u == null) throw new Exception("Downloads mein jagah nahi mili");
                    OutputStream os = cr.openOutputStream(u);
                    try { os.write(bytes); } finally { os.close(); }
                    where = "Downloads / FB Claims";
                } else {
                    File d = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "FB Claims");
                    d.mkdirs();
                    writeAll(new File(d, safe), bytes);
                    where = d.getAbsolutePath();
                }
                final String fWhere = where;
                runOnUiThread(() -> fileSavedDialog(shareCopy, mime, fWhere));
                return "ok";
            } catch (Exception e) {
                return String.valueOf(e.getMessage());
            }
        }

        @JavascriptInterface
        public void pickBackup() {
            runOnUiThread(() -> {
                try {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, PICK_BACKUP);
                } catch (Exception e) {
                    toast("File chunne wali screen nahi khuli");
                }
            });
        }

        @JavascriptInterface
        public void toast(String msg) { MainActivity.this.toast(msg); }

        @JavascriptInterface
        public String versionName() { return MainActivity.this.versionName(); }
    }

    // ---------------------------------------------------------------- Files & WhatsApp

    Uri shareUri(File f) {
        return FileProvider.getUriForFile(this, getPackageName() + ".files", f);
    }

    boolean installed(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    void fileSavedDialog(final File f, final String mime, String where) {
        boolean pdf = mime.equals("application/pdf");
        new AlertDialog.Builder(this)
                .setTitle(pdf ? "PDF save ho gayi" : "File save ho gayi")
                .setMessage(f.getName() + "\n\nPhone mein: " + where)
                .setPositiveButton("WhatsApp par bhejein", (d, w) -> share(f, mime, true))
                .setNeutralButton(pdf ? "Kholein" : "Share", (d, w) -> {
                    if (pdf) open(f, mime); else share(f, mime, false);
                })
                .setNegativeButton("Theek hai", null)
                .show();
    }

    void share(File f, String mime, boolean whatsapp) {
        try {
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType(mime);
            s.putExtra(Intent.EXTRA_STREAM, shareUri(f));
            s.putExtra(Intent.EXTRA_TEXT, "Faizan & Brothers — " + f.getName());
            s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (whatsapp) {
                String wa = installed("com.whatsapp") ? "com.whatsapp"
                        : (installed("com.whatsapp.w4b") ? "com.whatsapp.w4b" : null);
                if (wa != null) {
                    Intent w = new Intent(s);
                    w.setPackage(wa);
                    try {
                        startActivity(w);
                        return;
                    } catch (Exception ignored) { }
                }
            }
            startActivity(Intent.createChooser(s, "Bhejein"));
        } catch (Exception e) {
            toast("Share nahi ho saka");
        }
    }

    void open(File f, String mime) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(shareUri(f), mime);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Exception e) {
            toast("PDF kholne wali app nahi mili — Downloads / FB Claims mein dekhein");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_BACKUP || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try {
            InputStream in = getContentResolver().openInputStream(data.getData());
            String text = new String(readAll(in), "UTF-8");
            web.evaluateJavascript("window.FBBackup.restore(" + JSONObject.quote(text) + ")", null);
        } catch (Exception e) {
            toast("Backup file nahi khul saki");
        }
    }

    // ---------------------------------------------------------------- App update

    int versionCode() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    void checkForUpdate() {
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(RELEASES_API).openConnection();
                c.setRequestProperty("Accept", "application/vnd.github+json");
                c.setRequestProperty("User-Agent", "FB-Claims-App");
                c.setConnectTimeout(10000);
                c.setReadTimeout(15000);
                if (c.getResponseCode() != 200) return;
                BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close();
                c.disconnect();
                JSONObject o = new JSONObject(sb.toString());
                String digits = o.optString("tag_name", "").replaceAll("[^0-9]", "");
                final int latest = digits.isEmpty() ? 0 : Integer.parseInt(digits);
                String url = null;
                JSONArray assets = o.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject a = assets.getJSONObject(i);
                        if (a.optString("name", "").toLowerCase().endsWith(".apk")) {
                            url = a.optString("browser_download_url", null);
                            break;
                        }
                    }
                }
                final String apk = url != null ? url : RELEASES_PAGE;
                if (latest > versionCode()) runOnUiThread(() -> updateDialog(latest, apk));
            } catch (Exception ignored) {
                // No internet: the app keeps working; we check again next time.
            }
        }).start();
    }

    void updateDialog(int latest, final String apkUrl) {
        if (isFinishing()) return;
        new AlertDialog.Builder(this)
                .setTitle("Naya update aaya hai")
                .setMessage("FB Claims 1." + latest + " available hai (abhi " + versionName() + ").\n\n"
                        + "Download karke install karein. Aapka saara data waise hi rahega.")
                .setPositiveButton("Update download karein", (d, w) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)));
                    } catch (Exception e) {
                        toast("Browser nahi khula");
                    }
                })
                .setNegativeButton("Baad mein", null)
                .show();
    }
}
