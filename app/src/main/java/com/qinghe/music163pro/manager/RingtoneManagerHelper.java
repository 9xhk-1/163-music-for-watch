package com.qinghe.music163pro.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages ringtones that have been set through the app.
 * Stores ringtone records in SharedPreferences.
 */
public class RingtoneManagerHelper {

    private static final String TAG = "RingtoneManagerHelper";
    private static final String PREFS_NAME = "music163_ringtones";
    private static final String KEY_RINGTONES = "ringtones_json";

    private final SharedPreferences prefs;
    private final Context context;

    public static class RingtoneInfo {
        public String title;
        public String filePath;
        public int startSec;
        public int endSec;

        public RingtoneInfo(String title, String filePath) {
            this.title = title;
            this.filePath = filePath;
            this.startSec = -1;
            this.endSec = -1;
        }

        public RingtoneInfo(String title, String filePath, int startSec, int endSec) {
            this.title = title;
            this.filePath = filePath;
            this.startSec = startSec;
            this.endSec = endSec;
        }

        public boolean isClipped() {
            return startSec >= 0 && endSec > startSec;
        }
    }

    public RingtoneManagerHelper(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Record a ringtone that was set.
     */
    public void addRingtone(String title, String filePath) {
        addRingtone(title, filePath, -1, -1);
    }

    /**
     * Record a clipped ringtone with start/end seconds.
     */
    public void addRingtone(String title, String filePath, int startSec, int endSec) {
        List<RingtoneInfo> list = getRingtones();
        // Remove existing entry with same path
        List<RingtoneInfo> updated = new ArrayList<>();
        for (RingtoneInfo r : list) {
            if (!r.filePath.equals(filePath)) {
                updated.add(r);
            }
        }
        updated.add(0, new RingtoneInfo(title, filePath, startSec, endSec));
        saveRingtones(updated);
    }

    /**
     * Remove a ringtone record and delete from MediaStore.
     */
    public boolean removeRingtone(int index) {
        List<RingtoneInfo> list = getRingtones();
        if (index < 0 || index >= list.size()) return false;

        RingtoneInfo info = list.get(index);
        try {
            // Remove from MediaStore
            Uri uri = MediaStore.Audio.Media.getContentUriForPath(info.filePath);
            if (uri != null) {
                context.getContentResolver().delete(uri,
                        MediaStore.MediaColumns.DATA + "=?",
                        new String[]{info.filePath});
            }
        } catch (Exception e) {
            Log.w(TAG, "Error removing ringtone from MediaStore", e);
        }

        list.remove(index);
        saveRingtones(list);
        return true;
    }

    /**
     * Get all recorded ringtones.
     */
    public List<RingtoneInfo> getRingtones() {
        List<RingtoneInfo> list = new ArrayList<>();
        String json = prefs.getString(KEY_RINGTONES, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String title = obj.optString("title", "");
                String filePath = obj.optString("filePath", "");
                int startSec = obj.optInt("startSec", -1);
                int endSec = obj.optInt("endSec", -1);
                if (!title.isEmpty() && !filePath.isEmpty()) {
                    list.add(new RingtoneInfo(title, filePath, startSec, endSec));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error loading ringtones", e);
        }
        return list;
    }

    private void saveRingtones(List<RingtoneInfo> list) {
        try {
            JSONArray arr = new JSONArray();
            for (RingtoneInfo r : list) {
                JSONObject obj = new JSONObject();
                obj.put("title", r.title);
                obj.put("filePath", r.filePath);
                if (r.startSec >= 0) obj.put("startSec", r.startSec);
                if (r.endSec >= 0) obj.put("endSec", r.endSec);
                arr.put(obj);
            }
            prefs.edit().putString(KEY_RINGTONES, arr.toString()).apply();
        } catch (Exception e) {
            Log.w(TAG, "Error saving ringtones", e);
        }
    }
}
