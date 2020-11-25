package com.appacoustic.cointester.aaa.utils;

import android.content.SharedPreferences;

/**
 * Data management.
 * Created by Gabriel Moreno on 2017-07-01.
 */
public class DataManager {

    private static final String LAST_MODIFIED = "lastModified";
    private static final String URL_BASE = "urlBase";
    private static final String CONTACT_EMAIL = "contactEmail";

    private SharedPreferences prefs;

    private static DataManager ourInstance = new DataManager();

    public static DataManager getInstance() {
        return ourInstance;
    }

    private DataManager() {
    }

    public void setPrefs(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public long getLastModified() {
        return prefs.getLong(LAST_MODIFIED, -1);
    }

    public void setLastModified(long lastModified) {
        prefs.edit().putLong(LAST_MODIFIED, lastModified).apply();
    }

    public String getUrlBase() {
        return prefs.getString(URL_BASE, "");
    }

    public void setUrlBase(String urlBase) {
        prefs.edit().putString(URL_BASE, urlBase).apply();
    }

    public String getContactEmail() {
        return prefs.getString(CONTACT_EMAIL, "");
    }

    public void setContactEmail(String contactEmail) {
        prefs.edit().putString(CONTACT_EMAIL, contactEmail).apply();
    }
}
