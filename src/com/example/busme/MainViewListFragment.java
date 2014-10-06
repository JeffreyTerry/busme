package com.example.busme;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;

public class MainViewListFragment extends Fragment {

	private MainController mainController;
	private ListView mainListView;
	private EditText etSearchFrom, etSearchTo;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View layout = (FrameLayout) inflater.inflate(R.layout.mainlist_frag,
				container, false);
		MainActivity mainActivity = (MainActivity) this.getActivity();
		mainController = mainActivity.getMainController();

		etSearchFrom = (EditText) layout.findViewById(R.id.etSearchFrom);
		etSearchTo = (EditText) layout.findViewById(R.id.etSearchTo);
		mainListView = (ListView) layout.findViewById(R.id.lvMain);

		// hide the keyboard
		etSearchFrom.clearFocus();
		etSearchTo.clearFocus();
		mainActivity.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		mainController.setMainActivityEtSearchFrom(etSearchFrom);
		mainController.setMainActivityEtSearchTo(etSearchTo);
		mainController.setMainActivityListView(mainListView);

		return layout;
	}

}
