package com.example.busme;

import java.util.List;
import java.util.Stack;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class MainActivity extends FragmentActivity {
	public static final String NULL_DEVICE_ID = "9876";
	public static final String NULL_DATA_VERSION = "-1";
	private static String id;
	private static String dataVersion; // used to keep the app's data in sync with the server's data
	private ListView mainListView;
	private EditText etDestination, etStart;
	private View shadowExpanded, shadowRetracted, mainEtDivider;
	private MainController mainController;

	private ViewPager mViewPager;
	private MainFragmentAdapter mFragmentAdapter;
	public static FragmentManager fragmentManager;
	private SharedPreferences prefs;

	private Stack<Integer> pageHist;
	private boolean saveToHistory;
	private int currentPage;

	private SwipeRefreshLayout swipeLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainController = new MainController(this);

		setContentView(R.layout.viewpager_main);
		initializePages();

		// load shared preferences
		prefs = this.getSharedPreferences("com.example.busme",
				Context.MODE_PRIVATE);

		// check to make sure our device id is valid
		new Thread(new IdChecker()).start();
		new Thread(new DataChecker()).start();
		
		// a holder id to make sure routes don't crash the app inadvertently before the CheckIdTask has finished executing
		MainActivity.id = NULL_DEVICE_ID;
		MainActivity.id = NULL_DATA_VERSION;
	}

	public static String getId() {
		return MainActivity.id;
	}

	public MainController getMainController() {
		return mainController;
	}

	private void initializePages() {
		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(Fragment.instantiate(this,
				MainViewListFragment.class.getName()));
		fragments.add(Fragment.instantiate(this,
				MainViewMapFragment.class.getName()));

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mFragmentAdapter = new MainFragmentAdapter(
				this.getSupportFragmentManager(), fragments);
		mViewPager.setAdapter(mFragmentAdapter);

		// TODO
		// swipeLayout = (SwipeRefreshLayout)
		// findViewById(R.id.swipe_container);
		// swipeLayout.setOnRefreshListener(this);

		// Map fragment
		fragmentManager = this.getSupportFragmentManager();

		// Page changer
		pageHist = new Stack<Integer>();
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int a) {
				if (saveToHistory) {
					pageHist.push(Integer.valueOf(currentPage));
				}
			}

			@Override
			public void onPageScrolled(int a, float b, int c) {
			}

			@Override
			public void onPageScrollStateChanged(int a) {
			}
		});
		saveToHistory = true;
	}

	public void setShadows(View shadowExpanded, View shadowRetracted) {
		this.shadowExpanded = shadowExpanded;
		this.shadowRetracted = shadowRetracted;
	}

	public void setMainEts(EditText etStart, EditText etDestination,
			View mainEtDivider) {
		this.etStart = etStart;
		this.etDestination = etDestination;
		this.mainEtDivider = mainEtDivider;
	}

	public void onBackPressed() {
		if (pageHist.size() % 2 == 0) {
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			startActivity(intent);
		} else {
			saveToHistory = false;
			mViewPager.setCurrentItem(pageHist.pop().intValue());
			saveToHistory = true;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
//		mainController.resetLocationUpdateCount();
	}

	public void showEtStart(View v) {
		shadowRetracted.setVisibility(View.INVISIBLE);
		shadowExpanded.setVisibility(View.VISIBLE);
		etStart.setVisibility(View.VISIBLE);
		mainEtDivider.setVisibility(View.VISIBLE);
		etDestination.setCursorVisible(true);
	}

	/*
	 * @Override public void onRefresh() {
	 * mainController.fetchNewCards(etStart.getText().toString(),
	 * etDestination.getText().toString()); swipeLayout.setRefreshing(false); }
	 */

	private class IdChecker implements Runnable {
		private String getNewDeviceId() {
			try {
				return MainModel.getJSONObjectForURL("/newdevice").getString("id");
			} catch (JSONException e) {
				e.printStackTrace();
				return NULL_DEVICE_ID;
			}
		}
		
		private boolean idIsStillValid(String id) {
			JSONObject result = MainModel.getJSONObjectForURL("/checkdeviceid/" + id);
			try {
				return result.getBoolean("valid");
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		@Override
		public void run() {
			try {
				if (prefs.getString("id", "").contentEquals("")) {
					MainActivity.id = getNewDeviceId();
				} else {
					MainActivity.id = prefs.getString("id", "");
					if (!idIsStillValid(MainActivity.id)) {
						MainActivity.id = getNewDeviceId();
					}
				}
				Editor editor = prefs.edit();
				editor.putString("id", MainActivity.id);
				editor.commit();
				
				System.out.println("id: " + MainActivity.id);
				// now that we've made sure our device id is valid, we get some default cards
				mainController.fetchNewCards(MainModel.LOCATION_UNSPECIFIED,
						MainModel.LOCATION_UNSPECIFIED);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private class DataChecker implements Runnable {
		private String getNewDeviceId() {
			try {
				return MainModel.getJSONObjectForURL("/newdevice").getString("id");
			} catch (JSONException e) {
				e.printStackTrace();
				return NULL_DEVICE_ID;
			}
		}
		
		private boolean idIsStillValid(String id) {
			JSONObject result = MainModel.getJSONObjectForURL("/checkdataversion/" + dataVersion);
			try {
				return result.getBoolean("valid");
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		@Override
		public void run() {
			try {
				if (prefs.getString("id", "").contentEquals("")) {
					MainActivity.id = getNewDeviceId();
				} else {
					MainActivity.id = prefs.getString("id", "");
					if (!idIsStillValid(MainActivity.id)) {
						MainActivity.id = getNewDeviceId();
					}
				}
				Editor editor = prefs.edit();
				editor.putString("id", MainActivity.id);
				editor.commit();
				
				System.out.println("id: " + MainActivity.id);
				// now that we've made sure our device id is valid, we get some default cards
				mainController.fetchNewCards(MainModel.LOCATION_UNSPECIFIED,
						MainModel.LOCATION_UNSPECIFIED);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
