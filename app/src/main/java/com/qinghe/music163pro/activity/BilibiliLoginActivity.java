package com.qinghe.music163pro.activity;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.qinghe.music163pro.R;
import com.qinghe.music163pro.api.BilibiliApiHelper;
import com.qinghe.music163pro.util.QrCodeGenerator;
import com.qinghe.music163pro.util.WatchUiUtils;

/**
 * Bilibili QR code login activity.
 * Generates QR code for Bilibili web login and polls for scan status.
 */
public class BilibiliLoginActivity extends BaseWatchActivity {

    private ImageView ivQrCode;
    private TextView tvStatus;
    private TextView btnRefresh;

    private String qrcodeKey = "";
    private final Handler pollHandler = new Handler();
    private boolean polling = false;

    private static final int POLL_INTERVAL_MS = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(getResources().getColor(R.color.bg_dark));
        scrollView.setFillViewport(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(WatchUiUtils.px(this, 12), WatchUiUtils.px(this, 8),
                WatchUiUtils.px(this, 12), WatchUiUtils.px(this, 8));

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tvTitle.setText("B站扫码登录");
        tvTitle.setTextColor(getResources().getColor(R.color.text_primary));
        tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, WatchUiUtils.px(this, 8));
        container.addView(tvTitle);

        // QR code image
        ivQrCode = new ImageView(this);
        int qrSize = WatchUiUtils.px(this, 160);
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(qrSize, qrSize);
        qrParams.gravity = Gravity.CENTER_HORIZONTAL;
        ivQrCode.setLayoutParams(qrParams);
        ivQrCode.setBackgroundColor(Color.WHITE);
        ivQrCode.setPadding(WatchUiUtils.px(this, 4), WatchUiUtils.px(this, 4),
                WatchUiUtils.px(this, 4), WatchUiUtils.px(this, 4));
        container.addView(ivQrCode);

        // Status text
        tvStatus = new TextView(this);
        tvStatus.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tvStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        tvStatus.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setPadding(0, WatchUiUtils.px(this, 8), 0, WatchUiUtils.px(this, 4));
        container.addView(tvStatus);

        // Refresh button
        btnRefresh = new TextView(this);
        btnRefresh.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btnRefresh.setText("刷新二维码");
        btnRefresh.setTextColor(0xFF64B5F6);
        btnRefresh.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        btnRefresh.setGravity(Gravity.CENTER);
        btnRefresh.setPadding(WatchUiUtils.px(this, 16), WatchUiUtils.px(this, 8),
                WatchUiUtils.px(this, 16), WatchUiUtils.px(this, 8));
        btnRefresh.setClickable(true);
        btnRefresh.setFocusable(true);
        btnRefresh.setOnClickListener(v -> startQrLogin());
        container.addView(btnRefresh);

        scrollView.addView(container);
        setContentView(scrollView);

        startQrLogin();
    }

    private void startQrLogin() {
        stopPolling();
        tvStatus.setText("正在获取二维码...");
        ivQrCode.setImageBitmap(null);

        BilibiliApiHelper.generateQrCode(new BilibiliApiHelper.QrCodeCallback() {
            @Override
            public void onResult(String qrUrl, String key) {
                qrcodeKey = key;
                Bitmap qrBitmap = generateQrBitmap(qrUrl, 300);
                if (qrBitmap != null) {
                    ivQrCode.setImageBitmap(qrBitmap);
                    tvStatus.setText("请使用哔哩哔哩App扫码登录");
                    startPolling();
                } else {
                    tvStatus.setText("二维码生成失败");
                }
            }

            @Override
            public void onError(String message) {
                tvStatus.setText("获取二维码失败: " + message);
            }
        });
    }

    private Bitmap generateQrBitmap(String content, int size) {
        try {
            boolean[][] matrix = QrCodeGenerator.encode(content);
            if (matrix == null) return null;

            int matrixSize = matrix.length;
            int cellSize = size / matrixSize;
            int bitmapSize = cellSize * matrixSize;

            Bitmap bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < matrixSize; y++) {
                for (int x = 0; x < matrixSize; x++) {
                    int color = matrix[y][x] ? Color.BLACK : Color.WHITE;
                    for (int dy = 0; dy < cellSize; dy++) {
                        for (int dx = 0; dx < cellSize; dx++) {
                            bitmap.setPixel(x * cellSize + dx, y * cellSize + dy, color);
                        }
                    }
                }
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    private void startPolling() {
        polling = true;
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void stopPolling() {
        polling = false;
        pollHandler.removeCallbacks(pollRunnable);
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!polling || qrcodeKey.isEmpty()) return;

            BilibiliApiHelper.pollQrLogin(qrcodeKey, new BilibiliApiHelper.QrPollCallback() {
                @Override
                public void onResult(int code, String message, String cookie) {
                    switch (code) {
                        case 86101:
                            tvStatus.setText("等待扫码...");
                            if (polling) {
                                pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                            }
                            break;
                        case 86090:
                            tvStatus.setText("已扫码，请在手机上确认登录");
                            if (polling) {
                                pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                            }
                            break;
                        case 0:
                            // Login successful
                            stopPolling();
                            if (cookie == null || cookie.isEmpty()) {
                                tvStatus.setText("登录成功但未获取到Cookie");
                                break;
                            }
                            tvStatus.setText("登录成功!");
                            saveBilibiliCookie(cookie);
                            Toast.makeText(BilibiliLoginActivity.this,
                                    "B站登录成功", Toast.LENGTH_LONG).show();
                            pollHandler.postDelayed(() -> finish(), 1500);
                            break;
                        case 86038:
                            tvStatus.setText("二维码已过期，请刷新");
                            stopPolling();
                            break;
                        default:
                            tvStatus.setText("状态: " + code + " " + message);
                            if (polling) {
                                pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                            }
                            break;
                    }
                }

                @Override
                public void onError(String errMsg) {
                    tvStatus.setText("检查状态失败: " + errMsg);
                    if (polling) {
                        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
                    }
                }
            });
        }
    };

    private void saveBilibiliCookie(String cookie) {
        if (cookie == null || cookie.isEmpty()) return;
        SharedPreferences prefs = getSharedPreferences("music163_settings", MODE_PRIVATE);
        prefs.edit().putString("bilibili_cookie", cookie).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }
}
