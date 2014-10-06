package com.example.busme;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;

public class MainSearchActivity extends Activity {
	private MainController mainController;
	private EditText etStartQuery, etDestinationQuery;
	private ListView mainListView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainController = new MainController(this, false);

		setContentView(R.layout.main_search_activity);

		etStartQuery = (EditText) findViewById(R.id.etStartQuery);
		etDestinationQuery = (EditText) findViewById(R.id.etDestinationQuery);
		mainListView = (ListView) findViewById(R.id.lvSearchMain);

		mainController.setSearchActivityEtStart(etStartQuery);
		mainController.setSearchActivityEtDestination(etDestinationQuery);
		mainController.setSearchActivityListView(mainListView);

		if(getIntent().hasExtra("launch_button_source")) {
			if(getIntent().getStringExtra("launch_button_source").contentEquals(MainController.ET_SEARCH_TO_EXTRA)) {
				Log.d("et destination", "yeah");
				etDestinationQuery.requestFocus();
			} else {
				Log.d("et start", "yeah");
				etStartQuery.requestFocus();
			}
		}
		
		initializeSwipeLayout();
	}

	public MainController getMainController() {
		return mainController;
	}

	private void initializeSwipeLayout() {  // TODO
		// swipeLayout = (SwipeRefreshLayout)
		// findViewById(R.id.swipe_container);
		// swipeLayout.setOnRefreshListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// mainController.resetLocationUpdateCount();
	}

	/*
	 * @Override public void onRefresh() {
	 * mainController.fetchNewCards(etStart.getText().toString(),
	 * etDestination.getText().toString()); swipeLayout.setRefreshing(false); }
	 */
}
