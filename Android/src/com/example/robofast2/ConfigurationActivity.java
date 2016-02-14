package com.example.robofast2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ConfigurationActivity extends Activity {
	
	protected static final int CONFIG_SAVED = 2002;
	TextView inputF1, inputF2;
	Button saveConfig;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.config_layout);
		Intent intent = this.getIntent();
		
		inputF1 = (TextView) findViewById(R.id.inputF1);
		inputF2 = (TextView) findViewById(R.id.inputF2);
		inputF1.setText(getStoredValue("f1"));
		inputF2.setText(getStoredValue("f2"));
		saveConfig = (Button) findViewById(R.id.saveConfig);
		
		saveConfig.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if (inputF1.getText().toString().length()==0 || inputF2.getText().toString().length()==0) {
					Toast.makeText(getApplicationContext(), "Configuration is empty now!", Toast.LENGTH_SHORT).show();
				}
				else{	
					savePreferences("f1", inputF1.getText().toString());
					savePreferences("f2", inputF2.getText().toString());
					Toast.makeText(getApplicationContext(), "Configuration Updated!", Toast.LENGTH_SHORT).show();
					finish();
				}
				
				/*Intent intent = new Intent(ConfigurationActivity.this, MainActivity.class);
				startActivityForResult(intent, CONFIG_SAVED);*/
			}
		});
		
	}
	
	
	private String getStoredValue(String key) {
		// TODO Auto-generated method stub
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String valueString1 = sharedPreferences.getString(key,key);
		return valueString1;
		
	}

	private void savePreferences(String key, String value) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
}
	
}
