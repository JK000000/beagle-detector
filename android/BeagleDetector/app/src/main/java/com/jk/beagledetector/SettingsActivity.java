package com.jk.beagledetector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    SwitchCompat mswitch;
    EditText threshlod;
    Spinner model_select;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mswitch = findViewById(R.id.switch1);
        threshlod = findViewById(R.id.editTextNumberDecimal2);
        model_select = findViewById(R.id.spinner);

        mswitch.setChecked(TmpAppData.getInstance().debug_mode);
        threshlod.setText(Float.toString(TmpAppData.getInstance().threshlod));

        mswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TmpAppData.getInstance().debug_mode = isChecked;
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.spinner_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        model_select.setAdapter(adapter);

        model_select.setSelection(TmpAppData.getInstance().model);

        model_select.setOnItemSelectedListener(this);

        threshlod.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String txt = threshlod.getText().toString();

                try {
                    TmpAppData.getInstance().threshlod = Float.parseFloat(txt);
                }
                catch (Exception e)
                {
                    Toast.makeText(SettingsActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        TmpAppData.getInstance().model = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}