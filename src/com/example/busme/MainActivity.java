package com.example.busme;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class MainActivity extends Activity {
	private MainController mainController;
	private ListView mainListView;
	private EditText etDestination, etStart;
	private View shadowExpanded, shadowRetracted, mainEtDivider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mainController = new MainController(this);

		setContentView(R.layout.activity_main);
		etStart = (EditText) findViewById(R.id.etStart);
		etDestination = (EditText) findViewById(R.id.etDestination);
		shadowExpanded = findViewById(R.id.shadowExpanded);
		shadowRetracted = findViewById(R.id.shadowRetracted);
		mainEtDivider = findViewById(R.id.mainEtDivider);
		mainListView = (ListView) findViewById(R.id.lvMain);

		mainController.setEtStart(etStart);
		mainController.setEtDestination(etDestination);
		mainController.setListView(mainListView);
	}


	@Override
	protected void onResume() {
		super.onResume();
		mainController.resetLocationUpdateCount();
	}



	public void showEtStart(View v) {
		shadowRetracted.setVisibility(View.INVISIBLE);
		shadowExpanded.setVisibility(View.VISIBLE);
		etStart.setVisibility(View.VISIBLE);
		mainEtDivider.setVisibility(View.VISIBLE);
		etDestination.setCursorVisible(true);
	}
}