package com.bifel.datafetcher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RemoteViews;

import com.android.volley.Request;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

public class ConfigActivity extends Activity {

    private int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    private Intent resultValue;
    private EditText editTextURL;
    private EditText editTextData;
    private EditText editTextRegex;
    private EditText editTextRegexFind;
    private EditText editTextUpdatePeriod;
    private RadioGroup radioGroup;

    public final static String WIDGET_PREF = "widget_pref";
    public final static String BACKGROUND_COLOR = "background_color_";
    public final static String FOREGROUND_COLOR = "foreground_color_";
    public final static String SERVER_URL = "server_url_";
    public final static String DATA = "data_";
    public final static String HTTP_METHOD = "http_method_";
    public final static String RB_HTTP_METHOD_CHECKED = "http_method_checked_";
    public final static String REGEX = "regex_";
    public final static String REGEX_FIND = "regex_find_";
    public final static String UPDATE_PERIOD = "update_period_";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
        resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        setResult(RESULT_CANCELED, resultValue);

        setContentView(R.layout.config);

        SharedPreferences sp = getSharedPreferences(WIDGET_PREF, MODE_MULTI_PROCESS);
        editTextURL = findViewById(R.id.editTextURL);
        editTextURL.setText(sp.getString(SERVER_URL + widgetID, ""));

        editTextData = findViewById(R.id.editTextData);
        editTextData.setText(sp.getString(DATA + widgetID, ""));

        editTextRegex = findViewById(R.id.editTextRegex);
        editTextRegex.setText(sp.getString(REGEX + widgetID, ""));

        editTextRegexFind = findViewById(R.id.editTextFind);
        editTextRegexFind.setText(sp.getString(REGEX_FIND + widgetID, ""));

        editTextUpdatePeriod = findViewById(R.id.editTextUpdatePeriod);
        editTextUpdatePeriod.setText(sp.getString(UPDATE_PERIOD + widgetID, "30"));

        radioGroup = findViewById(R.id.radioHTTPMethod);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rGET) {
                    editTextData.setEnabled(false);
                } else {
                    editTextData.setEnabled(true);
                }
            }
        });
        radioGroup.check(sp.getInt(RB_HTTP_METHOD_CHECKED + widgetID, R.id.rPOST));
    }


    public void onApply(View v) {
        SharedPreferences sp = getSharedPreferences(WIDGET_PREF, MODE_MULTI_PROCESS);
        int checkedRadioButton = radioGroup.getCheckedRadioButtonId();
        String updatePeriod = editTextUpdatePeriod.getText().toString();
        updatePeriod = updatePeriod.equals("") ? "30" : updatePeriod;
        Widget.UPDATE_PERIOD = Integer.valueOf(updatePeriod);
        Editor editor = sp.edit();
        editor.putInt(RB_HTTP_METHOD_CHECKED + widgetID, checkedRadioButton);
        editor.putInt(HTTP_METHOD + widgetID, checkedRadioButton == R.id.rGET ? Request.Method.GET : Request.Method.POST);
        editor.putString(SERVER_URL + widgetID, editTextURL.getText().toString());
        editor.putString(DATA + widgetID, editTextData.getText().toString());
        editor.putString(REGEX + widgetID, editTextRegex.getText().toString());
        editor.putString(REGEX_FIND + widgetID, editTextRegexFind.getText().toString());
        editor.putString(UPDATE_PERIOD + widgetID, updatePeriod);
        editor.apply();
        Widget.runUpdate(this, AppWidgetManager.getInstance(this), widgetID);
        setResult(RESULT_OK, resultValue);
        finish();

    }

    public void onSelectForeground(View v) {
        onSelectColor(FOREGROUND_COLOR + widgetID, "Select foreground color");
    }

    public void onSelectBackground(View v) {
        onSelectColor(BACKGROUND_COLOR + widgetID, "Select background color");
    }

    public void onSelectColor(final String target, String title) {
        final SharedPreferences sp = getSharedPreferences(WIDGET_PREF, MODE_MULTI_PROCESS);
        ColorPickerDialogBuilder.with(this).setTitle(title).initialColor(sp.getInt(target, 0x7f00ff00)).wheelType(ColorPickerView.WHEEL_TYPE.FLOWER).density(10).setPositiveButton("Apply", new ColorPickerClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                Editor editor = sp.edit();
                editor.putInt(target, selectedColor);
                editor.apply();
            }
        }).build().show();
    }
}