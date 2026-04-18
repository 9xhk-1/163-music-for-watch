package com.qinghe.music163pro.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.qinghe.music163pro.R;
import com.qinghe.music163pro.util.WatchUiUtils;

/**
 * Bilibili feature menu - lists available functions:
 * - 从BV号打开 (open by BV ID)
 * - 登录 (Bilibili QR login)
 */
public class BilibiliActivity extends BaseWatchActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(getResources().getColor(R.color.bg_dark));
        scrollView.setFillViewport(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, WatchUiUtils.px(this, 8), 0, WatchUiUtils.px(this, 8));

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tvTitle.setText("听bilibili");
        tvTitle.setTextColor(getResources().getColor(R.color.text_primary));
        tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, WatchUiUtils.px(this, 4), 0, WatchUiUtils.px(this, 8));
        tvTitle.setLetterSpacing(0.1f);
        container.addView(tvTitle);

        // Menu item: 从BV号打开
        container.addView(createMenuItem(R.drawable.ic_video_library, "从BV号打开",
                () -> startActivity(new Intent(this, BilibiliBvidActivity.class))));

        // Menu item: 登录
        container.addView(createMenuItem(R.drawable.ic_qr_code, "登录B站",
                () -> startActivity(new Intent(this, BilibiliLoginActivity.class))));

        // Show login status
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(WatchUiUtils.px(this, 16), WatchUiUtils.px(this, 12),
                WatchUiUtils.px(this, 16), WatchUiUtils.px(this, 4));

        TextView tvLoginStatus = new TextView(this);
        tvLoginStatus.setId(android.R.id.text1);
        tvLoginStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        tvLoginStatus.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
        updateLoginStatus(tvLoginStatus);
        statusRow.addView(tvLoginStatus);
        container.addView(statusRow);

        scrollView.addView(container);
        setContentView(scrollView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView tvStatus = findViewById(android.R.id.text1);
        if (tvStatus != null) {
            updateLoginStatus(tvStatus);
        }
    }

    private void updateLoginStatus(TextView tv) {
        SharedPreferences prefs = getSharedPreferences("music163_settings", MODE_PRIVATE);
        String cookie = prefs.getString("bilibili_cookie", "");
        if (cookie != null && !cookie.isEmpty() && cookie.contains("SESSDATA")) {
            tv.setText("已登录B站");
        } else {
            tv.setText("未登录B站（不影响大部分功能）");
        }
    }

    private LinearLayout createMenuItem(int iconRes, String label, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, WatchUiUtils.px(this, 48)));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(WatchUiUtils.px(this, 16), 0, WatchUiUtils.px(this, 16), 0);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        row.setOnClickListener(v -> onClick.run());

        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                WatchUiUtils.px(this, 20), WatchUiUtils.px(this, 20));
        icon.setLayoutParams(iconParams);
        icon.setImageResource(iconRes);
        icon.setAlpha(0.7f);
        row.addView(icon);

        TextView tv = new TextView(this);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvParams.setMarginStart(WatchUiUtils.px(this, 12));
        tv.setLayoutParams(tvParams);
        tv.setText(label);
        tv.setTextColor(getResources().getColor(R.color.text_primary));
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        row.addView(tv);

        return row;
    }
}
