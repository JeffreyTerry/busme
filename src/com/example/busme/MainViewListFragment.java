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
import android.widget.TextView;

public class MainViewListFragment extends Fragment {

	private MainController mainController;
	private ListView mainListView;
	private EditText etSearchFrom, etSearchTo;
	private MainActivity mainActivity;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View layout = (FrameLayout) inflater.inflate(R.layout.mainlist_frag,
				container, false);
		mainActivity = (MainActivity) getActivity();
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
		

		if(mainActivity.shouldPlayTutorial()) {
			TextView[] tutorialDialogs = new TextView[4];
			tutorialDialogs[0] = (TextView) layout.findViewById(R.id.tvYouCanClickCards);
			tutorialDialogs[1] = (TextView) layout.findViewById(R.id.tvYouCanSwipeToRemoveCards);
			tutorialDialogs[2] = (TextView) layout.findViewById(R.id.tvYouCanSwipeForMap);
			tutorialDialogs[3] = (TextView) layout.findViewById(R.id.tvYouCanSearch);
			mainController.setTutorialInfoDialogs(tutorialDialogs);
			mainController.playTutorial();
		}
		
		return layout;
	}
}
