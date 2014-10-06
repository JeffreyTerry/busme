package com.example.busme;

import java.util.List;
import java.util.Stack;
import java.util.Vector;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.SwipeRefreshLayout;
import android.widget.ListView;

public class MainActivity extends FragmentActivity {
	private SwipeRefreshLayout swipeLayout;
	private ListView mainActivityListView;
	private ViewPager mViewPager;
	private MainFragmentAdapter mFragmentAdapter;
	private MainController mainController;

	private Stack<Integer> pageHist;
	private boolean saveToHistory;
	private int currentPage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainController = new MainController(this, true);

		setContentView(R.layout.viewpager_main);
		initializePages();
		initializeSwipeLayout();
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
		mFragmentAdapter = new MainFragmentAdapter(getSupportFragmentManager(),
				fragments);
		mViewPager.setAdapter(mFragmentAdapter);

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

	private void initializeSwipeLayout() { // TODO
		// swipeLayout = (SwipeRefreshLayout)
		// findViewById(R.id.swipe_container);
		// swipeLayout.setOnRefreshListener(this);
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
		// mainController.resetLocationUpdateCount();
	}

	/*
	 * @Override public void onRefresh() {
	 * mainController.fetchNewCards(etStart.getText().toString(),
	 * etDestination.getText().toString()); swipeLayout.setRefreshing(false); }
	 */
}
