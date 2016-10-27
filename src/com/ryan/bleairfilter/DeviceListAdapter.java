package com.ryan.bleairfilter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DeviceListAdapter extends BaseAdapter {

	private List<BLEDevice> mBleArray;
	private ViewHolder viewHolder;
	private Context mContext;
	
	public DeviceListAdapter(Context context) {
		mBleArray = new ArrayList<BLEDevice>();
		mContext = context;
	}

	public void addDevice(String name , String mac) {
		//if (!mBleArray.contains(device)) {
		// 这里就不需要过滤了，在server层已经有过滤了
			mBleArray.add(new BLEDevice(name, mac));
		//}
	}
	
	public void clearDevice() {
		// TODO Auto-generated method stub
		mBleArray.clear();
	}
	
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mBleArray.size();
	}

	@Override
	public BLEDevice getItem(int position) {
		// TODO Auto-generated method stub
		return mBleArray.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.item_list, null);
			viewHolder = new ViewHolder();
			viewHolder.tv_devName = (TextView) convertView
					.findViewById(R.id.tv_devName);
			viewHolder.tv_devAddress = (TextView) convertView
					.findViewById(R.id.tv_devAddress);
			convertView.setTag(viewHolder);
		} else {
			convertView.getTag();
		}

		// add-Parameters
		BLEDevice device = mBleArray.get(position);
		String devName = device.getName();
		if (devName != null && devName.length() > 0) {
			viewHolder.tv_devName.setText(devName);
		} else {
			viewHolder.tv_devName.setText("unknow-device");
		}
		viewHolder.tv_devAddress.setText(device.getAddress());

		return convertView;
	}

	class ViewHolder {
		TextView tv_devName, tv_devAddress;
	}
	
	class BLEDevice{
		String name;
		String address;
		public BLEDevice(String name, String address) {
			this.name = name;
			this.address = address;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getAddress() {
			return address;
		}
		public void setAddress(String address) {
			this.address = address;
		}
		
	}


	
	
}
