package com.bifel.datafetcher;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Widget extends AppWidgetProvider {

    public static String DEFAULT_TEXT = "Something going wrong!";
    public static String NO_SUCCESSFUL_MATCH = "No successful match";
    public static int UPDATE_PERIOD = 30;

    private static final String CURRENT_TEXT = "current_text_";
    private static final String ACTION_ALARM = "action_alarm";
    private static final String ACTION_UPDATE = "action_update";

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        SharedPreferences sp = context.getSharedPreferences(ConfigActivity.WIDGET_PREF, Context.MODE_MULTI_PROCESS);
        trimToSize(sp.getString(CURRENT_TEXT + appWidgetId, DEFAULT_TEXT), appWidgetId, new RemoteViews(context.getPackageName(), R.layout.widget), appWidgetManager);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        System.out.println("---------------------------------------------------------------------------------OnUpdate");
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);
        SharedPreferences sp = context.getSharedPreferences(ConfigActivity.WIDGET_PREF, Context.MODE_MULTI_PROCESS);

        for (int widgetID : appWidgetIds) {
            runUpdate(context, appWidgetManager, widgetID);
            updateWidgetViews(false, DEFAULT_TEXT, widgetID, sp, widgetView, appWidgetManager);
        }
    }

    public static void runUpdate(Context context, AppWidgetManager appWidgetManager, int widgetID) {
        System.out.println("---------------------------------------------------------------------------------RunUpdate");
        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);
        SharedPreferences sp = context.getSharedPreferences(ConfigActivity.WIDGET_PREF, Context.MODE_MULTI_PROCESS);
        Intent configIntent = new Intent(context, ConfigActivity.class);
        configIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        PendingIntent pIntent = PendingIntent.getActivity(context, widgetID, configIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.imgConfig, pIntent);

        Intent updateIntent = new Intent(context, Widget.class);
        updateIntent.setAction(ACTION_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {widgetID});
        pIntent = PendingIntent.getBroadcast(context, widgetID, updateIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.layout, pIntent);

        Widget.updateWidgetFont(widgetID, sp, widgetView, appWidgetManager);
        Widget.updateAlarm(true, context);
    }

    public static void updateAlarm(boolean enable, Context context) {
        Intent intent = new Intent(context, Widget.class);
        intent.setAction(ACTION_ALARM);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (enable) {
            Objects.requireNonNull(alarmManager).setRepeating(AlarmManager.RTC, System.currentTimeMillis(), UPDATE_PERIOD * 60 * 1000, pIntent);
        } else {
            Objects.requireNonNull(alarmManager).cancel(pIntent);
        }
    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        Editor editor = context.getSharedPreferences(ConfigActivity.WIDGET_PREF, Context.MODE_PRIVATE).edit();
        for (int widgetID : appWidgetIds) {
            editor.remove(ConfigActivity.BACKGROUND_COLOR + widgetID);
            editor.remove(ConfigActivity.FOREGROUND_COLOR + widgetID);
            editor.remove(ConfigActivity.SERVER_URL + widgetID);
            editor.remove(ConfigActivity.DATA + widgetID);
            editor.remove(ConfigActivity.REGEX + widgetID);
            editor.remove(ConfigActivity.REGEX_FIND + widgetID);
            editor.remove(ConfigActivity.HTTP_METHOD + widgetID);
            editor.remove(ConfigActivity.RB_HTTP_METHOD_CHECKED + widgetID);
            editor.remove(ConfigActivity.UPDATE_PERIOD + widgetID);
            editor.remove(CURRENT_TEXT + widgetID);
        }
        editor.apply();

    }

    @Override
    public void onEnabled(Context context) {
        System.out.println("---------------------------------------------------------------------------------OnEnabled");
        super.onEnabled(context);
        updateAlarm(true, context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        updateAlarm(false, context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("---------------------------------------------------------------------------------OnReceive");
        super.onReceive(context, intent);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int ids[] = appWidgetManager.getAppWidgetIds(new ComponentName(context.getPackageName(), getClass().getName()));
        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);
        SharedPreferences sp = context.getSharedPreferences(ConfigActivity.WIDGET_PREF, Context.MODE_MULTI_PROCESS);

        for (int id : ids) {
            if (ACTION_ALARM.equalsIgnoreCase(Objects.requireNonNull(intent.getAction()))) {
                sendRequest(id, sp, widgetView, appWidgetManager, context);
            }
            if (intent.getAction().equalsIgnoreCase(ACTION_UPDATE)) {
                sendRequest(id, sp, widgetView, appWidgetManager, context);
            }
        }
    }

    public static void updateWidgetFont(final int widgetID, SharedPreferences sp, RemoteViews widgetView, AppWidgetManager widgetManager) {
        System.out.println("---------------------------------------------------------------------------------UpdateFont");
        int bgColor = sp.getInt(ConfigActivity.BACKGROUND_COLOR + widgetID, 0x7f000000);
        int fgColor = sp.getInt(ConfigActivity.FOREGROUND_COLOR + widgetID, 0xffffffff);
        widgetView.setInt(R.id.background, "setColorFilter", bgColor);
        widgetView.setInt(R.id.background, "setImageAlpha", Color.alpha(bgColor));
        widgetView.setTextColor(R.id.txt, fgColor);
        widgetView.setTextColor(R.id.offlineFlag, fgColor);

        widgetManager.updateAppWidget(widgetID, widgetView);
    }

    public static void updateWidgetViews(boolean isSuccess, String response, final int widgetID, SharedPreferences sp, RemoteViews widgetView, AppWidgetManager widgetManager) {
        System.out.println("---------------------------------------------------------------------------------UpdateViews");
        if (isSuccess) {
            sp.edit().putString(CURRENT_TEXT + widgetID, response).apply();
            widgetView.setViewVisibility(R.id.offlineFlag, View.INVISIBLE);
        } else {
            response = sp.getString(CURRENT_TEXT + widgetID, DEFAULT_TEXT);
            widgetView.setViewVisibility(R.id.offlineFlag, View.VISIBLE);
        }
        widgetView.setTextViewText(R.id.txt, response);
        widgetView.setViewVisibility(R.id.progressBar, View.INVISIBLE);
        widgetManager.updateAppWidget(widgetID, widgetView);
        trimToSize(response, widgetID, widgetView, widgetManager);
    }

    private static void trimToSize(String response, int widgetID, RemoteViews widgetView, AppWidgetManager widgetManager) {
        System.out.println("---------------------------------------------------------------------------------TrimToSize");
        Bundle options = widgetManager.getAppWidgetOptions(widgetID);
        final int maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        final int maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        int currentTextSize = 400;
        int maxTextLen = (maxWidth / currentTextSize) * (maxHeight / currentTextSize);
        while (maxTextLen < response.length() + 9 && currentTextSize > 10) {
            currentTextSize -= 2;
            maxTextLen = (maxWidth / currentTextSize) * (maxHeight / currentTextSize);
        }
        widgetView.setFloat(R.id.txt, "setTextSize", currentTextSize);
        widgetView.setFloat(R.id.offlineFlag, "setTextSize", currentTextSize / 4);
        widgetManager.updateAppWidget(widgetID, widgetView);
    }

    public static void sendRequest(final int widgetID, final SharedPreferences sp, final RemoteViews widgetView, final AppWidgetManager widgetManager, Context context) {
        System.out.println("---------------------------------------------------------------------------------SendRequest");
        final String serverURL = sp.getString(ConfigActivity.SERVER_URL + widgetID, "");
        if ("".equals(serverURL)) {
            return;
        }
        widgetView.setViewVisibility(R.id.progressBar, View.VISIBLE);
        widgetManager.updateAppWidget(widgetID, widgetView);
        final int HTTPMethod = sp.getInt(ConfigActivity.HTTP_METHOD + widgetID, Request.Method.POST);
        final RequestQueue myRequestQueue = Volley.newRequestQueue(context);

        Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                final String regex = sp.getString(ConfigActivity.REGEX + widgetID, "");
                String find_field = sp.getString(ConfigActivity.REGEX_FIND + widgetID, "");
                find_field = find_field.equals("") ? "1" : find_field;
                final int find = Integer.valueOf(find_field);

                if (regex.equals("")) {
                    response = response.trim();
                } else {
                    Matcher matcher = Pattern.compile(regex).matcher(response);
                    response = NO_SUCCESSFUL_MATCH;
                    for (int i = 0; i < find; i++) {
                        if (matcher.find()) {
                            response = matcher.group();
                        }
                    }

                }
                updateWidgetViews(true, response, widgetID, sp, widgetView, widgetManager);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                updateWidgetViews(false, "", widgetID, sp, widgetView, widgetManager);
            }
        };

        Request request = new StringRequest(HTTPMethod, serverURL, listener, errorListener) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "text");
                return params;
            }

            @Override
            public byte[] getBody() {
                try {
                    final String data = sp.getString(ConfigActivity.DATA + widgetID, "");
                    return data.getBytes(getParamsEncoding());
                } catch (UnsupportedEncodingException uee) {
                    throw new RuntimeException("Encoding not supported: " + getParamsEncoding(), uee);
                }
            }
        };

        myRequestQueue.add(request);
    }
}
