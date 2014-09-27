package com.example.busme;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
		View android = (FrameLayout) inflater.inflate(R.layout.mainlist_frag, container, false);
		mainController = ((MainActivity) this.getActivity()).getMainController();
		
		etStart = (EditText) android.findViewById(R.id.etStart);
		etDestination = (EditText) android.findViewById(R.id.etDestination);
		shadowExpanded = android.findViewById(R.id.shadowExpanded);
		shadowRetracted = android.findViewById(R.id.shadowRetracted);
		mainEtDivider = android.findViewById(R.id.mainEtDivider);
		mainListView = (ListView) android.findViewById(R.id.lvMain);
		
		mainController.setEtStart(etStart);
		mainController.setEtDestination(etDestination);
		mainController.setListView(mainListView);
		return android;
		
	}
	
}
