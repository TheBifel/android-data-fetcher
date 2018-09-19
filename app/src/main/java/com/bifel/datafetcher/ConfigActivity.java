package com.bifel.datafetcher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

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

        WidgetController controller = new WidgetController(AppWidgetManager.getInstance(this), this, widgetID);
        Widget.activeWidgetControllers.put(widgetID, controller);

        editTextURL = findViewById(R.id.editTextURL);
        editTextURL.setText(controller.getURL());

        editTextData = findViewById(R.id.editTextData);
        editTextData.setText(controller.getPOSTData());

        editTextRegex = findViewById(R.id.editTextRegex);
        editTextRegex.setText(controller.getRegex());

        editTextRegexFind = findViewById(R.id.editTextFind);
        editTextRegexFind.setText(controller.getRegexFind());

        editTextUpdatePeriod = findViewById(R.id.editTextUpdatePeriod);
        editTextUpdatePeriod.setText(String.valueOf(controller.getUpdatePeriod()));

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
        radioGroup.check(controller.getSelectedRadioButton(R.id.rPOST));
    }

    @Override
    public void finish() {
//        Intent intent = new Intent(this, ConfigActivity.class);
//        intent.setAction()
//        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
//        sendOrderedBroadcast(intent, null);
        System.out.println("----------------------------------------------------Config activity finished");
        super.finish();
    }

    public void onApply(View v) {
        WidgetController controller = Widget.activeWidgetControllers.get(widgetID);
        int checkedRadioButton = radioGroup.getCheckedRadioButtonId();
        controller.putUpdatePeriod(editTextUpdatePeriod.getText().toString());
        controller.putSelectedRadioButton(checkedRadioButton);
        controller.putHttpMethod(checkedRadioButton);
        controller.putURL(editTextURL.getText().toString());
        controller.putPostData(editTextData.getText().toString());
        controller.putRegex(editTextRegex.getText().toString());
        controller.putRegexFind(editTextRegexFind.getText().toString());

        setResult(RESULT_OK, resultValue);
        Widget.updateIntents(controller);
        Widget.updateWidgetFont(controller);
        finish();

    }

    public void onSelectForeground(View v) {
        onSelectColor("foreground", "Select foreground color");
    }

    public void onSelectBackground(View v) {
        onSelectColor("background", "Select background color");
    }

    public void onSelectColor(final String method, String title) {
        final WidgetController controller = Widget.activeWidgetControllers.get(widgetID);
        ColorPickerDialogBuilder
                .with(this).setTitle(title)
                .initialColor("background".equals(method) ? controller.getBackground(0x7f00ff00) : controller.getForeground(0x7f00ff00))
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(10)
                .setPositiveButton("Apply", new ColorPickerClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                if ("background".equals(method)) {
                    controller.putBackground(selectedColor);
                } else {
                    controller.putForeground(selectedColor);
                }
            }
        }).build().show();
    }
}