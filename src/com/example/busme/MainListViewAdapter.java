
package com.example.busme;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainListViewAdapter extends ArrayAdapter<MainListViewItem> {
	private Context context;
	private ArrayList<MainListViewItem> items;
	private ViewHolderItem viewHolder;
	private boolean loading;

	public MainListViewAdapter(Context context,
			ArrayList<MainListViewItem> items) {
		super(context, R.layout.main_list_item, items);

		loading = false;
		this.context = context;
		this.items = items;
		addPaddingItems();
	}

	private void addPaddingItems() {
		items.add(0, new MainListViewItem(-1, -1, "null", "null"));
		items.add(0, new MainListViewItem(-1, -1, "null", "null"));
	}

	@Override
	public void clear() {
		super.clear();
		addPaddingItems();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position == 0) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			View v = inflater
					.inflate(R.layout.main_list_padding, parent, false);
			v.setTag("hello");
			return v;
		} else if (position == items.size() - 1) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			View v = inflater
					.inflate(R.layout.main_list_padding, parent, false);
			v.setTag("goodbye");
			return v;
		}
		position++;
		

		if (loading) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			View v = inflater.inflate(R.layout.list_loading, parent, false);
			v.setTag("loading");
			return v;
		}
		System.out.println(convertView);

		if (convertView == null || convertView.getTag().equals("hello")
				|| convertView.getTag().equals("goodbye")
				|| convertView.getTag().equals("loading")) {

			// inflate the layout
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			convertView = inflater.inflate(R.layout.main_list_item, parent,
					false);

			// well set up the ViewHolder
			viewHolder = new ViewHolderItem();
			
			viewHolder.tvTime = (TextView) convertView
					.findViewById(R.id.tvTime);
			viewHolder.tvRouteNumber = (TextView) convertView
					.findViewById(R.id.tvRouteNumber);
			viewHolder.tvRouteStart = (TextView) convertView
					.findViewById(R.id.tvRouteStart);
			viewHolder.tvRouteDestination = (TextView) convertView
					.findViewById(R.id.tvRouteDestination);
			viewHolder.linearlayoutCond = (LinearLayout) convertView.findViewById(R.id.linearlayoutCond); 			

			// store the holder with the view.
			convertView.setTag(viewHolder);

		} else {
			// we've just avoided calling findViewById() on resource everytime
			// just use the viewHolder
			viewHolder = (ViewHolderItem) convertView.getTag();
		}

		// object item based on the position
		MainListViewItem item = items.get(position);

		// assign values if the object is not null
		if (item != null) {
			// get the TextView from the ViewHolder and then set the text (item
			// name) and tag (item ID) values

			viewHolder.tvTime.setText(item.getTime()
					+ " mins");
			viewHolder.tvRouteNumber.setText("Bus #" + item.getRouteNumber());
			viewHolder.tvRouteStart.setText(item.getRouteStart());
			viewHolder.tvRouteDestination.setText(item.getRouteDestination());
			if (item.getTime() < 11) {
				viewHolder.tvTime.setTextColor(Color.rgb(55, 197, 112));
			} else if (item.getTime() < 31) {
				viewHolder.tvTime.setTextColor(Color.rgb(244, 191,65));
				viewHolder.linearlayoutCond.setBackgroundResource(R.drawable.main_list_item1);
			} else {
				viewHolder.tvTime.setTextColor(Color.rgb(219,68,61));
				viewHolder.linearlayoutCond.setBackgroundResource(R.drawable.main_list_item2);
			}
		}

		return convertView;
	}

	private static class ViewHolderItem {
		public TextView tvTime;
		public TextView tvRouteNumber;
		public TextView tvRouteStart;
		public TextView tvRouteDestination;
		public LinearLayout linearlayoutCond;
	}

	public void setLoading(boolean loading) {
		if(loading){
			items.clear();
			addPaddingItems();
			items.add(new MainListViewItem(-1, -1, "loading", "loading"));
		}
		this.loading = loading;
	}
}
