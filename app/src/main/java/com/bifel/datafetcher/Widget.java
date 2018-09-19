package com.bifel.datafetcher;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.SparseArray;
import android.widget.RemoteViews;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Intent.*;

public class Widget extends AppWidgetProvider {

    public static String DEFAULT_TEXT = "Something going wrong!";
    public static String NO_SUCCESSFUL_MATCH = "No successful match";

    private static final String ACTION_ALARM = "action_alarm";
    private static final String ACTION_UPDATE = "action_update";
    public static int UPDATE_PERIOD = 1;

    public static SparseArray<WidgetController> activeWidgetControllers = new SparseArray<>();

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        trimToSize(activeWidgetControllers.get(appWidgetId));
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        System.out.println("---------------------------------------------------------------------------------OnUpdate");
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int widgetID : appWidgetIds) {
            WidgetController controller = Widget.activeWidgetControllers.get(widgetID);
            if (controller == null) {
                continue;
            }
            updateIntents(controller);
            updateWidgetFont(controller);
            updateWidgetViews(false, DEFAULT_TEXT, controller);
        }
    }


    public static void updateIntents(WidgetController controller) {
        Context context = controller.getContext();

        System.out.println("---------------------------------------------------------------------------------RunUpdate");
        int widgetID = controller.getID();
        RemoteViews widgetView = controller.getWidgetView();

        Intent configIntent = new Intent(context, ConfigActivity.class);
        configIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        PendingIntent pIntent = PendingIntent.getActivity(context, widgetID, configIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.imgConfig, pIntent);

        Intent updateIntent = new Intent(context, Widget.class);
        updateIntent.setAction(ACTION_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        pIntent = PendingIntent.getBroadcast(context, widgetID, updateIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.layout, pIntent);

        setAlarmIntent(true, context);
        controller.getAppWidgetManager().updateAppWidget(widgetID, widgetView);
    }

    public static void setAlarmIntent(boolean enable, Context context) {
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
        for (int widgetID : appWidgetIds) {
            WidgetController controller = activeWidgetControllers.get(widgetID);
            if (controller != null) {
                controller.clear();
                setAlarmIntent(false, context);
            }
            activeWidgetControllers.remove(widgetID);
        }
    }

    @Override
    public void onEnabled(Context context) {
        System.out.println("---------------------------------------------------------------------------------OnEnabled");
        super.onEnabled(context);
        setAlarmIntent(true, context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        setAlarmIntent(false, context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("---------------------------------------------------------------------------------OnReceive");
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        super.onReceive(context, intent);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int ids[] = appWidgetManager.getAppWidgetIds(new ComponentName(context.getPackageName(), getClass().getName()));
        System.out.println(Arrays.toString(ids));
        for (int id : ids) {
            WidgetController widget = activeWidgetControllers.get(id);
            if (widget == null) {
                widget = new WidgetController(appWidgetManager, context, id);
                activeWidgetControllers.put(id, widget);
                updateIntents(widget);
                updateWidgetFont(widget);
                updateWidgetFont(widget);
                sendRequest(widget);
            }
            //noinspection deprecation
            if (!Objects.requireNonNull(pm).isScreenOn()) {
                System.out.println("Screen is off");
                widget.neadToUpdate(true);
                return;
            }
            if (Calendar.getInstance().getTimeInMillis() - widget.getLastUpdate() < 10000) {
                System.out.println("-------------------------------Update blocked");
                return;
            }
            if (ACTION_ALARM.equalsIgnoreCase(intent.getAction())) {
                System.out.println("-------------------------------Received because of alarm");
                sendRequest(widget);
            }
            if (ACTION_UPDATE.equalsIgnoreCase(intent.getAction())) {
                System.out.println("-------------------------------Received because of touch");
                sendRequest(widget);
            }
            if (ACTION_USER_PRESENT.equals(intent.getAction()) && widget.isNeadToUpdate()) {
                System.out.println("-------------------------------Received because of unlock");
                widget.neadToUpdate(false);
                sendRequest(widget);
            }
        }
    }

    public static void updateWidgetFont(WidgetController controller) {
        System.out.println("---------------------------------------------------------------------------------UpdateFont");
        controller.setBackground();
        controller.setForeground();
        controller.aply();
    }

    public static void updateWidgetViews(boolean isSuccess, String response, WidgetController controller) {
        System.out.println("---------------------------------------------------------------------------------UpdateViews");
        controller.setText(response);
        if (isSuccess) {
            controller.setOfflineFlag(false);
        } else {
            controller.setOfflineFlag(true);
        }
        controller.setProgressBar(false);
        controller.aply();
        trimToSize(controller);
    }

    public static void sendRequest(final WidgetController controller) {
        System.out.println("---------------------------------------------------------------------------------SendRequest");
        final String serverURL = controller.getURL();
        if ("".equals(serverURL)) {
            return;
        }
        controller.setProgressBar(true);
        controller.aply();
        final int HTTPMethod = controller.getHTTPMethod();
        final RequestQueue myRequestQueue = Volley.newRequestQueue(controller.getContext());

        Response.Listener<String> listener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                final String regex = controller.getRegex();
                String find_field = controller.getRegexFind();
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
                updateWidgetViews(true, response, controller);
                controller.setLastUpdate(Calendar.getInstance().getTimeInMillis());
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse != null) {
                    int codeError = error.networkResponse.statusCode;
                    if (400 <= codeError && codeError < 500) {
                        updateWidgetViews(true, new String(error.networkResponse.data, StandardCharsets.UTF_8), controller);
                        return;
                    }
                }
                updateWidgetViews(false, controller.getText(DEFAULT_TEXT), controller);

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
                    final String data = controller.getPOSTData();
                    return data.getBytes(getParamsEncoding());
                } catch (UnsupportedEncodingException uee) {
                    throw new RuntimeException("Encoding not supported: " + getParamsEncoding(), uee);
                }
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(6000, 5, 1));
        myRequestQueue.add(request);
    }

    private static void trimToSize(WidgetController controller) {
        System.out.println("---------------------------------------------------------------------------------TrimToSize");
        String text = controller.getText(DEFAULT_TEXT);
        Bundle options = controller.getWidgetOptions();
        final int maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        final int maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        int currentTextSize = 400;
        int maxTextLen = (maxWidth / currentTextSize) * (maxHeight / currentTextSize);
        while (maxTextLen < text.length() + 9 && currentTextSize > 10) {
            currentTextSize -= 2;
            maxTextLen = (maxWidth / currentTextSize) * (maxHeight / currentTextSize);
        }
        controller.setForegroundSize(currentTextSize);
        controller.aply();
    }
}
