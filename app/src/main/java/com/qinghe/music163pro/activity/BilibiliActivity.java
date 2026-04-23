package com.qinghe.music163pro.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
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

    private LinearLayout cloudFavoritesItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(getResources().getColor(R.color.bg_dark));
        scrollView.setFillViewport(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(px(10), px(8), px(10), px(10));

        container.addView(createTitleBar());

        container.addView(createMenuItem(R.drawable.ic_search, "搜索视频",
                () -> startActivity(new Intent(this, BilibiliSearchActivity.class))));

        // Menu item: 从BV号打开
        container.addView(createMenuItem(R.drawable.ic_video_library, "从BV号打开",
                () -> startActivity(new Intent(this, BilibiliBvidActivity.class))));

        container.addView(createMenuItem(R.drawable.ic_favorite_border, "收藏",
                () -> startActivity(new Intent(this, BilibiliFavoritesActivity.class))));

        // Menu item: 登录
        container.addView(createMenuItem(R.drawable.ic_qr_code, "登录B站",
                () -> startActivity(new Intent(this, BilibiliLoginActivity.class))));

        cloudFavoritesItem = createMenuItem(R.drawable.ic_cloud, "云端收藏",
                () -> startActivity(new Intent(this, BilibiliCloudFavoritesActivity.class)));
        container.addView(cloudFavoritesItem);

        // Show login status
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = px(8);
        statusRow.setLayoutParams(statusParams);
        statusRow.setBackgroundColor(getResources().getColor(R.color.surface_elevated));
        statusRow.setPadding(px(10), px(8), px(10), px(8));

        TextView tvLoginStatus = new TextView(this);
        tvLoginStatus.setId(android.R.id.text1);
        tvLoginStatus.setTextColor(getResources().getColor(R.color.text_secondary));
        tvLoginStatus.setTextSize(11);
        updateLoginStatus(tvLoginStatus);
        updateCloudFavoritesVisibility();
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
        updateCloudFavoritesVisibility();
    }

    private void updateLoginStatus(TextView tv) {
        SharedPreferences prefs = getSharedPreferences("music163_settings", MODE_PRIVATE);
        String cookie = prefs.getString("bilibili_cookie", "");
        if (!TextUtils.isEmpty(cookie) && cookie.contains("SESSDATA")) {
            tv.setText("已登录B站");
        } else {
            tv.setText("未登录B站（不影响大部分功能）");
        }
    }

    private void updateCloudFavoritesVisibility() {
        if (cloudFavoritesItem == null) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences("music163_settings", MODE_PRIVATE);
        String cookie = prefs.getString("bilibili_cookie", "");
        cloudFavoritesItem.setVisibility(
                !TextUtils.isEmpty(cookie) && cookie.contains("SESSDATA") ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private LinearLayout createTitleBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, px(38)));
        bar.setGravity(Gravity.CENTER_VERTICAL);

        ImageView back = new ImageView(this);
        back.setLayoutParams(new LinearLayout.LayoutParams(px(22), px(22)));
        back.setImageResource(R.drawable.ic_arrow_back);
        back.setColorFilter(getResources().getColor(R.color.text_primary));
        back.setOnClickListener(v -> finish());
        bar.addView(back);

        TextView title = new TextView(this);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleParams);
        title.setGravity(Gravity.CENTER);
        title.setText("听bilibili");
        title.setTextColor(getResources().getColor(R.color.text_primary));
        title.setTextSize(14);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        bar.addView(title);

        ImageView placeholder = new ImageView(this);
        placeholder.setLayoutParams(new LinearLayout.LayoutParams(px(22), px(22)));
        bar.addView(placeholder);
        return bar;
    }

    private LinearLayout createMenuItem(int iconRes, String label, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, px(52));
        params.topMargin = px(6);
        row.setLayoutParams(params);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(px(14), 0, px(14), 0);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackgroundColor(getResources().getColor(R.color.surface_elevated));
        row.setOnClickListener(v -> onClick.run());

        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                px(22), px(22));
        icon.setLayoutParams(iconParams);
        icon.setImageResource(iconRes);
        icon.setAlpha(0.7f);
        row.addView(icon);

        TextView tv = new TextView(this);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvParams.setMarginStart(px(10));
        tv.setLayoutParams(tvParams);
        tv.setText(label);
        tv.setTextColor(getResources().getColor(R.color.text_primary));
        tv.setTextSize(14);
        row.addView(tv);

        return row;
    }
}
