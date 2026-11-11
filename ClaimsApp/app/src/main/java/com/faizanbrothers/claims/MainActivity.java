package com.faizanbrothers.claims;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * FB Claims — opens the live Faizan & Brothers claim system (the same page and
 * the same saved data as on claude.ai) and adds phone-only helpers:
 * app self-update from GitHub Releases, WhatsApp sharing of PDF reports,
 * and quick access to downloaded files.
 */
public class MainActivity extends Activity {

    static final String CLAIMS_URL = "https://claude.ai/artifact/QY87vqYhLM9r33ebgyNmv4";
    static final String RELEASES_API = "https://api.github.com/repos/hassan4500678-ux/F-B/releases/latest";
    static final String RELEASES_PAGE = "https://github.com/hassan4500678-ux/F-B/releases/latest";

    static final int PICK_PDF = 41;
    static final int NAVY = 0xFF1F3864, BLUE = 0xFF2E75B6, GREEN = 0xFF2E7D32,
            ORANGE = 0xFFE65100, INK = 0xFF16223A, MUTED = 0xFF5A6680, CARD = 0xFFFFFFFF, LINE = 0xFFD6DCE8;

    Button updateBtn;
    TextView statusTv;
    String apkUrl;
    int latestVersion = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        checkForUpdate(false);
        if (savedInstanceState == null) openClaims();
    }

    // ---------------------------------------------------------------- UI

    int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setBackgroundColor(NAVY);
        head.setPadding(dp(20), dp(26), dp(20), dp(22));
        TextView t1 = text("FAIZAN & BROTHERS", 22, Color.WHITE, true);
        t1.setLetterSpacing(0.03f);
        TextView t2 = text("VIP Claim Management · EBM / Peak Freans", 13, 0xCCFFFFFF, false);
        head.addView(t1);
        head.addView(t2);
        root.addView(head);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(18), dp(16), dp(24));
        root.addView(body);

        body.addView(button("📋  Claims App kholein", BLUE, v -> openClaims()));
        body.addView(note("Wahi page aur wahi data jo claude.ai par hai. Jo yahan save karein woh wahan dikhega, aur wahan ka yahan."));

        body.addView(button("🔄  Taaza karein (latest changes)", NAVY, v -> {
            toast("Latest version khul raha hai…");
            openClaims();
        }));
        body.addView(note("Claims page mein jo bhi naya change ho, woh har dafa khud aa jata hai."));

        updateBtn = button("⬇  Naya app update install karein", ORANGE, v -> downloadUpdate());
        updateBtn.setVisibility(View.GONE);
        body.addView(updateBtn);
        body.addView(button("🔍  App update check karein", 0xFF455A7A, v -> checkForUpdate(true)));

        body.addView(button("🟢  WhatsApp par PDF report bhejein", GREEN, v -> pickPdf()));
        body.addView(note("Pehle Claims App mein \"Report / Export PDF\" se PDF download karein, phir yahan se file chun kar WhatsApp par bhejein."));

        body.addView(button("📁  Downloads / PDF files dekhein", 0xFF006F8A, v -> openDownloads()));

        statusTv = text("", 13, MUTED, false);
        statusTv.setPadding(0, dp(14), 0, 0);
        body.addView(statusTv);
        setStatus("App version " + versionName());

        TextView info = note("Pehli dafa Chrome mein claude.ai par apne account se login karna hoga. Uske baad app seedha khulegi.");
        info.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD);
        g.setStroke(dp(1), LINE);
        g.setCornerRadius(dp(8));
        info.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(16);
        info.setLayoutParams(lp);
        body.addView(info);

        return scroll;
    }

    TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    TextView note(String s) {
        TextView t = text(s, 13, MUTED, false);
        t.setPadding(dp(2), dp(4), dp(2), dp(10));
        return t;
    }

    Button button(String label, int color, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        b.setPadding(dp(18), dp(14), dp(18), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(8));
        b.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) b.setStateListAnimator(null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        b.setLayoutParams(lp);
        b.setOnClickListener(l);
        return b;
    }

    void setStatus(String s) {
        if (statusTv != null) statusTv.setText(s);
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    // ---------------------------------------------------------------- Claims page

    void openClaims() {
        Uri uri = Uri.parse(CLAIMS_URL);
        String pkg = null;
        try { pkg = CustomTabsClient.getPackageName(this, null); } catch (Exception ignored) { }
        if (pkg != null) {
            try {
                CustomTabColorSchemeParams colors = new CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(NAVY)
                        .setNavigationBarColor(NAVY)
                        .build();
                CustomTabsIntent tab = new CustomTabsIntent.Builder()
                        .setDefaultColorSchemeParams(colors)
                        .setShowTitle(true)
                        .setUrlBarHidingEnabled(true)
                        .build();
                // Open in the browser itself so sign-in, PDF downloads and saved
                // data all work exactly as on the website.
                tab.intent.setPackage(pkg);
                tab.launchUrl(this, uri);
                return;
            } catch (Exception ignored) { }
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            toast("Browser nahi mila. Google Chrome install karein.");
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
            return "?";
        }
    }

    void checkForUpdate(final boolean manual) {
        if (manual) setStatus("Update check ho raha hai…");
        new Thread(() -> {
            String err = null;
            int latest = 0;
            String url = null;
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(RELEASES_API).openConnection();
                c.setRequestProperty("Accept", "application/vnd.github+json");
                c.setRequestProperty("User-Agent", "FB-Claims-App");
                c.setConnectTimeout(12000);
                c.setReadTimeout(15000);
                int code = c.getResponseCode();
                if (code != 200) {
                    err = "Update server ne jawab nahi diya (" + code + ")";
                } else {
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    r.close();
                    JSONObject o = new JSONObject(sb.toString());
                    String tag = o.optString("tag_name", "");
                    String digits = tag.replaceAll("[^0-9]", "");
                    latest = digits.isEmpty() ? 0 : Integer.parseInt(digits);
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
                }
                c.disconnect();
            } catch (Exception e) {
                err = "Internet check karein — update check nahi hua";
            }
            final String fErr = err;
            final int fLatest = latest;
            final String fUrl = url;
            runOnUiThread(() -> onUpdateResult(manual, fErr, fLatest, fUrl));
        }).start();
    }

    void onUpdateResult(boolean manual, String err, int latest, String url) {
        int mine = versionCode();
        if (err != null) {
            if (manual) setStatus(err + "  ·  App version " + versionName());
            return;
        }
        if (latest > mine && url != null) {
            latestVersion = latest;
            apkUrl = url;
            updateBtn.setText("⬇  Naya app update (1." + latest + ") install karein");
            updateBtn.setVisibility(View.VISIBLE);
            setStatus("Naya update available hai: 1." + latest + "  ·  Abhi: " + versionName());
            if (!manual) toast("FB Claims ka naya update aaya hai — app mein \"Naya app update\" dabayein");
        } else {
            updateBtn.setVisibility(View.GONE);
            setStatus("App up to date hai  ·  Version " + versionName());
            if (manual) toast("App up to date hai");
        }
    }

    void downloadUpdate() {
        if (apkUrl == null) {
            checkForUpdate(true);
            return;
        }
        // Open the new APK link in the browser; the phone downloads it and the
        // user installs it from the download notification (data stays safe).
        Uri uri = Uri.parse(apkUrl);
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        String pkg = null;
        try { pkg = CustomTabsClient.getPackageName(this, null); } catch (Exception ignored) { }
        if (pkg != null) i.setPackage(pkg);
        try {
            startActivity(i);
            setStatus("Naya APK download ho raha hai. Notification se kholein aur \"Update\" dabayein.");
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_PAGE)));
            } catch (Exception ignored) {
                toast("Browser nahi khula");
            }
        }
    }

    // ---------------------------------------------------------------- WhatsApp & files

    void pickPdf() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/pdf");
            startActivityForResult(i, PICK_PDF);
        } catch (Exception e) {
            try {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/pdf");
                startActivityForResult(i, PICK_PDF);
            } catch (Exception e2) {
                toast("File chunne wali screen nahi khuli");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PDF || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        sharePdf(data.getData());
    }

    boolean installed(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    void sharePdf(Uri uri) {
        Intent s = new Intent(Intent.ACTION_SEND);
        s.setType("application/pdf");
        s.putExtra(Intent.EXTRA_STREAM, uri);
        s.putExtra(Intent.EXTRA_TEXT, "Faizan & Brothers — Claim Report");
        s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String wa = installed("com.whatsapp") ? "com.whatsapp" : (installed("com.whatsapp.w4b") ? "com.whatsapp.w4b" : null);
        if (wa != null) {
            try {
                Intent w = new Intent(s);
                w.setPackage(wa);
                startActivity(w);
                return;
            } catch (Exception ignored) { }
        }
        try {
            startActivity(Intent.createChooser(s, "Report bhejein"));
        } catch (Exception e) {
            toast("Share nahi ho saka");
        }
    }

    void openDownloads() {
        try {
            Intent i = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            pickPdf();
        }
    }
}
