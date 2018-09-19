package com.bifel.datafetcher;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.android.volley.Request;

@SuppressWarnings("ALL")
public class WidgetController {

    private final static String WIDGET_PREF = "widget_pref";
    private final static String BACKGROUND_COLOR = "background_color_";
    private final static String FOREGROUND_COLOR = "foreground_color_";
    private final static String SERVER_URL = "server_url_";
    private final static String POST_DATA = "data_";
    private final static String HTTP_METHOD = "http_method_";
    private final static String RB_HTTP_METHOD_CHECKED = "http_method_checked_";
    private final static String REGEX = "regex_";
    private final static String REGEX_FIND = "regex_find_";
    private final static String UPDATE_PERIOD = "update_period_";
    private final static String CURRENT_TEXT = "current_text_";

    private final RemoteViews widgetView;
    private final AppWidgetManager widgetManager;
    private final Context context;
    private final SharedPreferences sp;
    private final int ID;
    private int updatePeriod = 30;
    private long last_update;
    private boolean neadToUpdate = false;


    public WidgetController(AppWidgetManager widgetManager, Context context, int widgetID) {
        this.widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);
        this.widgetManager = widgetManager;
        this.context = context;
        ID = widgetID;
        sp = context.getSharedPreferences(WIDGET_PREF, Context.MODE_MULTI_PROCESS);
    }

    public WidgetController(Context context, int widgetID) {
        this.context = context;
        ID = widgetID;
        widgetManager = AppWidgetManager.getInstance(context);
        widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);
        sp = context.getSharedPreferences(WIDGET_PREF, Context.MODE_MULTI_PROCESS);
    }

    public void aply() {
        widgetManager.updateAppWidget(ID, widgetView);
    }

    public void clear(){
        Editor editor = sp.edit();
        editor.remove(BACKGROUND_COLOR + ID);
        editor.remove(FOREGROUND_COLOR + ID);
        editor.remove(SERVER_URL + ID);
        editor.remove(POST_DATA + ID);
        editor.remove(REGEX + ID);
        editor.remove(REGEX_FIND + ID);
        editor.remove(HTTP_METHOD + ID);
        editor.remove(UPDATE_PERIOD + ID);
        editor.remove(CURRENT_TEXT + ID);
        editor.remove(RB_HTTP_METHOD_CHECKED + ID);
        editor.apply();
        Widget.activeWidgetControllers.delete(ID);
    }

    public RemoteViews getWidgetView() {
        return widgetView;
    }

    public void setBackground() {
        int bgColor = sp.getInt(BACKGROUND_COLOR + ID, 0x7f000000);
        widgetView.setInt(R.id.background, "setColorFilter", bgColor);
        widgetView.setInt(R.id.background, "setImageAlpha", Color.alpha(bgColor));
    }

    public int getBackground(int defaultColor){
        return sp.getInt(BACKGROUND_COLOR + ID, defaultColor);
    }

    public void putBackground(int color){
        Editor editor = sp.edit();
        editor.putInt(BACKGROUND_COLOR + ID, color);
        editor.apply();
    }

    public void setForeground() {
        int fgColor = sp.getInt(FOREGROUND_COLOR + ID, 0xffffffff);
        widgetView.setTextColor(R.id.txt, fgColor);
        widgetView.setTextColor(R.id.offlineFlag, fgColor);
    }

    public int getForeground(int defaultColor){
        return sp.getInt(FOREGROUND_COLOR + ID, defaultColor);
    }

    public void putForeground(int color){
        Editor editor = sp.edit();
        editor.putInt(FOREGROUND_COLOR + ID, color);
        editor.apply();
    }

    public void setOfflineFlag(boolean visibility) {
        widgetView.setViewVisibility(R.id.offlineFlag, visibility ? View.VISIBLE : View.INVISIBLE);
    }

    public void setProgressBar(boolean visibility) {
        widgetView.setViewVisibility(R.id.progressBar, visibility ? View.VISIBLE : View.INVISIBLE);
    }

    public void setText(String text) {
        widgetView.setTextViewText(R.id.txt, text);
        putText(text);
    }

    public void putText(String text) {
        Editor editor = sp.edit();
        editor.putString(CURRENT_TEXT + ID, text);
        editor.apply();
    }

    public String getText(String defaultText) {
        return sp.getString(CURRENT_TEXT + ID, defaultText);
    }

    public void setForegroundSize(float size) {
        widgetView.setFloat(R.id.txt, "setTextSize", size);
        widgetView.setFloat(R.id.offlineFlag, "setTextSize", size / 4);
    }

    public Bundle getWidgetOptions() {
        return widgetManager.getAppWidgetOptions(ID);
    }

    public Context getContext() {
        return context;
    }

    public AppWidgetManager getAppWidgetManager(){
        return widgetManager;
    }

    public String getURL() {
        return sp.getString(SERVER_URL + ID, "");
    }

    public int getHTTPMethod() {
        return sp.getInt(HTTP_METHOD + ID, Request.Method.POST);
    }

    public String getRegex() {
        return sp.getString(REGEX + ID, "");
    }

    public String getRegexFind() {
        return sp.getString(REGEX_FIND + ID, "");
    }

    public String getPOSTData() {
        return sp.getString(POST_DATA + ID, "");
    }

    public int getID() {
        return ID;
    }

    public int getUpdatePeriod() {
        return Integer.valueOf(sp.getString(UPDATE_PERIOD + ID, "30"));
    }

    public void putUpdatePeriod(String updatePeriod) {
        this.updatePeriod = Integer.valueOf("".equals(updatePeriod) ? "30" : updatePeriod);
        Editor editor = sp.edit();
        editor.putString(UPDATE_PERIOD + ID, String.valueOf(this.updatePeriod));
        editor.apply();
        Widget.UPDATE_PERIOD = this.updatePeriod;
    }

    public void putSelectedRadioButton(int checkedRadioButton) {
        Editor editor = sp.edit();
        editor.putInt(RB_HTTP_METHOD_CHECKED + ID, checkedRadioButton);
        editor.apply();
    }

    public int getSelectedRadioButton(int defaultButton){
        return sp.getInt(RB_HTTP_METHOD_CHECKED + ID, defaultButton);
    }

    public void putHttpMethod(int checkedRadioButton) {
        Editor editor = sp.edit();
        editor.putInt(HTTP_METHOD + ID, checkedRadioButton == R.id.rGET ? Request.Method.GET : Request.Method.POST);
        editor.apply();
    }

    public void putURL(String url) {
        Editor editor = sp.edit();
        editor.putString(SERVER_URL + ID, url);
        editor.apply();
    }

    public void putPostData(String data) {
        Editor editor = sp.edit();
        editor.putString(POST_DATA + ID, data);
        editor.apply();
    }

    public void putRegex(String regex) {
        Editor editor = sp.edit();
        editor.putString(REGEX + ID, regex);
        editor.apply();
    }

    public void putRegexFind(String regex_find) {
        Editor editor = sp.edit();
        editor.putString(REGEX_FIND + ID, regex_find);
        editor.apply();
    }

    public void neadToUpdate(boolean bool) {
        neadToUpdate = bool;
    }

    public boolean isNeadToUpdate() {
        return neadToUpdate;
    }

    public long getLastUpdate() {
        return last_update;
    }

    public void setLastUpdate(long last_update) {
        this.last_update = last_update;
    }
}
