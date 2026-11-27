package com.example.decibelmeter.lite;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Base64;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import org.json.JSONObject;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainActivity extends Activity {
    private static final int RATE = 48000;
    private static final int CHUNK = 4800;
    private static final int MIC_REQUEST = 100;
    private static final String HOME = "file:///android_asset/index.html";
    private WebView web;
    private boolean foreground, wanted, requesting, destroyed, awake = true;
    private volatile Session session;
    private static final class Session {
        final AudioRecord audio;
        final AtomicBoolean pending = new AtomicBoolean();
        volatile boolean active = true;
        Session(AudioRecord audio) { this.audio = audio; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        root.setBackgroundColor(0xff101827);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                    android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets.consumeSystemWindowInsets();
        });
        setContentView(root);
        if (android.os.Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        web.setBackgroundColor(0xff101827);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setBlockNetworkLoads(true);
        web.addJavascriptInterface(new Bridge(), "NativeMeter");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !HOME.equals(request.getUrl().toString());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !HOME.equals(url);
            }
        });
        web.loadUrl(HOME);
    }

    private final class Bridge {
        @JavascriptInterface public void setRunning(boolean value) {
            runOnUiThread(() -> { if (destroyed) return; wanted = value; syncRecording(); });
        }
        @JavascriptInterface public void setAwake(boolean value) {
            runOnUiThread(() -> { if (destroyed) return; awake = value; updateAwake(); });
        }
    }

    @Override protected void onResume() {
        super.onResume(); foreground = true; syncRecording();
    }
    @Override protected void onPause() {
        foreground = false; stopRecording(); updateAwake(); super.onPause();
    }
    @Override protected void onDestroy() {
        destroyed = true; stopRecording();
        if (web != null) {
            web.removeJavascriptInterface("NativeMeter");
            ((android.view.ViewGroup) web.getParent()).removeView(web);
            web.destroy(); web = null;
        }
        super.onDestroy();
    }
    private void updateAwake() {
        if (foreground && wanted && awake)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private void syncRecording() {
        updateAwake();
        if (destroyed || !foreground || !wanted) { stopRecording(); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (!requesting) {
                requesting = true;
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
            }
            return;
        }
        if (session != null) return;
        try {
            int min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (min <= 0) throw new IllegalStateException("Unsupported sample rate");
            AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
            int source = "true".equals(manager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED))
                ? MediaRecorder.AudioSource.UNPROCESSED : MediaRecorder.AudioSource.VOICE_RECOGNITION;
            AudioRecord audio = new AudioRecord(source, RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, Math.max(min, CHUNK * 8));
            try {
                if (audio.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException();
                audio.startRecording();
                if (audio.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) throw new IllegalStateException();
            } catch (RuntimeException e) { audio.release(); throw e; }
            Session next = new Session(audio);
            session = next;
            new Thread(() -> readAudio(next), "decibel-audio").start();
        } catch (RuntimeException e) {
            fail("无法启动麦克风，请检查权限或其他录音应用");
        }
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code != MIC_REQUEST) return;
        requesting = false;
        if (results.length == 0 || results[0] != PackageManager.PERMISSION_GRANTED)
            fail("未授权麦克风，请在系统设置中允许权限后开始检测");
        else syncRecording();
    }
    private void stopRecording() {
        Session old = session; session = null;
        if (old == null) return;
        old.active = false;
        try { old.audio.stop(); } catch (RuntimeException ignored) {}
        // Worker owns release; stop unblocks the blocking read.
    }
    private void readAudio(Session current) {
        short[] samples = new short[CHUNK];
        byte[] bytes = new byte[CHUNK * 2];
        try {
            while (current.active) {
                int n = current.audio.read(samples, 0, samples.length, AudioRecord.READ_BLOCKING);
                if (!current.active) break;
                if (n <= 0) throw new IllegalStateException("AudioRecord read " + n);
                // Bound the UI queue if WebView is busy; never retain audio history.
                if (!current.pending.compareAndSet(false, true)) continue;
                for (int i = 0; i < n; i++) {
                    bytes[i * 2] = (byte) samples[i];
                    bytes[i * 2 + 1] = (byte) (samples[i] >> 8);
                }
                String encoded = Base64.encodeToString(bytes, 0, n * 2, Base64.NO_WRAP);
                runOnUiThread(() -> {
                    if (destroyed || session != current || !current.active || web == null) {
                        current.pending.set(false); return;
                    }
                    web.evaluateJavascript("window.feed(" + JSONObject.quote(encoded) + ")",
                        result -> current.pending.set(false));
                });
            }
        } catch (RuntimeException e) {
            runOnUiThread(() -> { if (session == current) fail("音频采集已中断，请重新开始检测"); });
        } finally {
            try { current.audio.release(); } catch (RuntimeException ignored) {}
        }
    }
    private void fail(String message) {
        wanted = false; stopRecording(); updateAwake();
        if (!destroyed && web != null)
            web.evaluateJavascript("window.meterStatus(" + JSONObject.quote(message) + ",true)", null);
    }
}
