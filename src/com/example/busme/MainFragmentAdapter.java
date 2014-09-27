package com.example.busme;

import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;

public class MainFragmentAdapter extends FragmentPagerAdapter {

	private List<Fragment> fragments;
	FragmentTransaction fragmentTransaction;

	public MainFragmentAdapter(FragmentManager fm, List<Fragment> fragments) {
		super(fm);
		this.fragments = fragments;
		// TODO Auto-generated constructor stub
	}

	@Override
	public Fragment getItem(int position) {
		// TODO Auto-generated method stub
//
//		if (position == 1) {
//			fragmentTransaction = this.fragments.get(1).getFragmentManager()
//					.beginTransaction();
//			fragmentTransaction.replace(R.id.pager, this.fragments.get(0));
//			fragmentTransaction.addToBackStack(null);
//			fragmentTransaction.commit();
//		}
		return fragments.get(position);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return fragments.size();
	}

}
