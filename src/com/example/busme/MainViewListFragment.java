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
	private EditText etDestination, etStart;
	private View shadowExpanded, shadowRetracted, mainEtDivider;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View layout = (FrameLayout) inflater.inflate(R.layout.mainlist_frag,
				container, false);
		MainActivity mainActivity = (MainActivity) this.getActivity();
		mainController = mainActivity.getMainController();

		etStart = (EditText) layout.findViewById(R.id.etStart);
		etDestination = (EditText) layout.findViewById(R.id.etDestination);
		shadowExpanded = layout.findViewById(R.id.shadowExpanded);
		shadowRetracted = layout.findViewById(R.id.shadowRetracted);
		mainEtDivider = layout.findViewById(R.id.mainEtDivider);
		mainListView = (ListView) layout.findViewById(R.id.lvMain);

		// hide the keyboard
		etStart.clearFocus();
		etDestination.clearFocus();
		mainActivity.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		mainController.setEtStart(etStart);
		mainController.setEtDestination(etDestination);
		mainController.setListView(mainListView);

		mainActivity.setShadows(shadowExpanded, shadowRetracted);
		mainActivity.setMainEts(etStart, etDestination, mainEtDivider);

		return layout;
	}

}
