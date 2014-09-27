package com.example.busme;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;

public class MainViewListFragment extends Fragment {

	private MainController mainController;
	private ListView mainListView;
	private EditText etDestination, etStart;
	private View shadowExpanded, shadowRetracted, mainEtDivider;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View android = (FrameLayout) inflater.inflate(R.layout.mainlist_frag,
				container, false);
		MainActivity mainActivity = (MainActivity) this.getActivity();
		mainController = mainActivity.getMainController();

		etStart = (EditText) android.findViewById(R.id.etStart);
		etDestination = (EditText) android.findViewById(R.id.etDestination);
		shadowExpanded = android.findViewById(R.id.shadowExpanded);
		shadowRetracted = android.findViewById(R.id.shadowRetracted);
		mainEtDivider = android.findViewById(R.id.mainEtDivider);
		mainListView = (ListView) android.findViewById(R.id.lvMain);

		// hide the keyboard
		etStart.clearFocus();
		etDestination.clearFocus();
		InputMethodManager imm = (InputMethodManager) mainActivity
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		mainActivity.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		mainController.setEtStart(etStart);
		mainController.setEtDestination(etDestination);
		mainController.setListView(mainListView);

		mainActivity.setShadows(shadowExpanded, shadowRetracted);
		mainActivity.setMainEts(etStart, etDestination, mainEtDivider);

		return android;

	}
	
	

}
