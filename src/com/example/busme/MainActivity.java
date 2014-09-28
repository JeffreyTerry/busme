package com.example.busme;

import java.util.List;
import java.util.Stack;
import java.util.Vector;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class MainActivity extends FragmentActivity {
	private ListView mainListView;
	private EditText etDestination, etStart;
	private View shadowExpanded, shadowRetracted, mainEtDivider;
	private MainController mainController;

	private ViewPager mViewPager;
	private MainFragmentAdapter mFragmentAdapter;
	public static FragmentManager fragmentManager;
	
	private Stack<Integer> pageHist;
	private boolean saveToHistory;
	private int currentPage;
	
	private SwipeRefreshLayout swipeLayout;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mainController = new MainController(this);

		setContentView(R.layout.viewpager_main);
		initializePages();
	}

	public MainController getMainController() {
		return mainController;
	}

	private void initializePages() {
		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(Fragment.instantiate(this, MainViewListFragment.class.getName()));
		fragments.add(Fragment.instantiate(this, MainViewMapFragment.class.getName()));
		
	    mViewPager = (ViewPager) findViewById(R.id.pager);
        mFragmentAdapter = new MainFragmentAdapter(this.getSupportFragmentManager(), fragments);
        mViewPager.setAdapter(mFragmentAdapter);
        
        // TODO
        //swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
		//swipeLayout.setOnRefreshListener(this);
		
        //Map fragment
        fragmentManager = this.getSupportFragmentManager();
        
        //Page changer
        pageHist = new Stack<Integer>();
        mViewPager.setOnPageChangeListener(new OnPageChangeListener(){
        	@Override
            public void onPageSelected(int a) {
                if(saveToHistory){
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
	
	public void onBackPressed(){
		if (pageHist.size()%2 == 0){
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			startActivity(intent);
		}
		else {
			saveToHistory = false;
			mViewPager.setCurrentItem(pageHist.pop().intValue());
			saveToHistory = true;
		}
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
	
	/*@Override
	public void onRefresh() {
        mainController.fetchNewCards(etStart.getText().toString(), 
        		etDestination.getText().toString());
        swipeLayout.setRefreshing(false);
    }*/
}
