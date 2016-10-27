package com.ryan.bleairfilter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.ryan.bleairfilter.DeviceListAdapter.ViewHolder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements OnClickListener, OnCheckedChangeListener {
	private final int ENABLE_BT = 1;
	
	// 用于保存MAC地址
	private SharedPreferences mSp = null;
	
	// 菜单选项
	private MenuItem mRefreshItem = null;
	
	// UI界面的信使
	private final Messenger mMessenger;

	// 服务端的信使
	private Messenger mServiceMessenger = null;
	// 启动服务的Intent
	private Intent mServiceIntent;
	
	// 服务端的状态机
	private BleService.State mState = BleService.State.UNKNOWN;
	// 记录当前连接的设备地址
	private String currConnectAddress;
	
	// APP标题
	private String mTitle;
	
	// 显示搜索和连接的进度条
	private ProgressDialog progressDialog;
	// 用来显示多个界面
	private ViewPager mViewPager = null;
	private final ArrayList<View> mArrayList = new ArrayList<View>();
	private MyPagerAdapter mPageAdapter = null;
	// 底部按钮
	private ImageView mBtnControl;
	private ImageView mBtnSettings;
	private ImageView mBtnDevices;
	// 设备列表
	private ListView mDeviceList;
	private DeviceListAdapter mDevListAdapter;
	// 调试界面
	private TextView mSettingDevName;
	private TextView mSettingRecvText;
	private EditText mSettingSendText;
	private Button	 mSettingSendBtn;
	
	// 控制界面
	private ImageView mBtnZhineng;
	private ImageView mBtnLizi;
	private ImageView mBtnHuangjing;
	private ImageView mBtnShuimian;
	private ImageView mBtnKaiguan;
	private ToggleButton mBtnLvwang;
	private ImageView mBtnFengsan;
	private ImageView mBtnDingshi;
	
	private ImageView mAirQualityImg;
	
	private TextView mLvWANG1;
	private TextView mLvWANG2;
	private TextView mLvWANG3;
	private TextView mLvWANG4;
	
	/** 类加载时创建UI信使 */
	public MainActivity() {
		super();
		mMessenger = new Messenger(new IncomingHandler(this));
	}
	
	@SuppressLint("InflateParams")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		setContentView(R.layout.main);
		
		// 用于保存MAC地址
		mSp = this.getSharedPreferences(Constant.SAVE_MAC_ADDRESS, Context.MODE_PRIVATE);
		
		mTitle = MainActivity.this.getTitle().toString();
		
		// 界面相关操作
		mViewPager = (ViewPager)findViewById(R.id.main_viewpager);
		// 装载TAB VIEW
		LayoutInflater mInflater = LayoutInflater.from(this);
		mArrayList.add(mInflater.inflate(R.layout.control_layout, null));
		mArrayList.add(mInflater.inflate(R.layout.settings_layout, null));
		mArrayList.add(mInflater.inflate(R.layout.devices_layout, null));
		
		// 设置适配器
		mPageAdapter = new MyPagerAdapter();
		mViewPager.setAdapter(mPageAdapter);
		mViewPager.setCurrentItem(0); //默认启动联系人页面
		mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			public void onPageSelected(int arg0) {
				// TODO Auto-generated method stub
				switch(arg0){
					case 0:
						break;
					case 1:
						break;
					case 2:
						break;
					default:
						break;
				}
			}
			public void onPageScrolled(int arg0, float arg1, int arg2) {}
			public void onPageScrollStateChanged(int arg0) {}
		});
		
		mBtnControl = (ImageView)findViewById(R.id.btnControl);
		mBtnSettings = (ImageView)findViewById(R.id.btnSettings);
		mBtnDevices = (ImageView)findViewById(R.id.btnDevicesList);
		mBtnControl.setOnClickListener(this);
		mBtnSettings.setOnClickListener(this);
		mBtnDevices.setOnClickListener(this);

		// 设备列表
		mDeviceList = (ListView) (mArrayList.get(2).findViewById(R.id.devices_listview));
		mDevListAdapter = new DeviceListAdapter(this);
		mDeviceList.setAdapter(mDevListAdapter);
		mDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				// 已经连上了就不要发送再次连接
				if(mState == BleService.State.CONNECTED){
					Toast.makeText(MainActivity.this, "已经连上设备，不要重复连接", Toast.LENGTH_SHORT).show();
					return;
				}
				
				
				Message msg = Message.obtain(null, BleService.MSG_DEVICE_CONNECT);
				if (msg != null) {
					currConnectAddress = ((ViewHolder)view.getTag()).tv_devAddress.getText().toString();
					msg.obj = currConnectAddress;
					Constant.Log("onItemClick: tv_devAddress="+msg.obj);
					try {
						progressDialog = ProgressDialog.show(MainActivity.this, null,
								"连接设备:"+currConnectAddress);
						mServiceMessenger.send(msg);
					} catch (RemoteException e) {
						Constant.Log("Lost connection to service"+e);
						unbindService(mConnection);
					}
				}
			}
		});
		
		//--------------------------------------------
		// 调试界面的UI
		mSettingDevName = (TextView)(mArrayList.get(1).findViewById(R.id.tv_devName));
		mSettingRecvText = (TextView)(mArrayList.get(1).findViewById(R.id.tv_receiveData));
		mSettingSendText = (EditText)(mArrayList.get(1).findViewById(R.id.et_writeContent));
		mSettingSendBtn = (Button)(mArrayList.get(1).findViewById(R.id.btn_sendMsg));
		mSettingSendBtn.setOnClickListener(this);
		mSettingSendText.addTextChangedListener(new TextWatcher(){
			private CharSequence temp;  
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				temp = s; // 当前输入的字符
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				// 统计用户输入了多少个字符
	            mSettingSendBtn.setText("发送(" + temp.length()+")");  
			}
		});
		
		//--------------------------------------------
		// 控制界面
		mBtnZhineng = (ImageView)(mArrayList.get(0).findViewById(R.id.img_btn_zhineng));
		mBtnZhineng.setOnClickListener(this);
		mBtnLizi = (ImageView)(mArrayList.get(0).findViewById(R.id.img_btn_lizi));
		mBtnLizi.setOnClickListener(this);
		mBtnHuangjing = (ImageView)(mArrayList.get(0).findViewById(R.id.img_btn_huangjing));
		mBtnHuangjing.setOnClickListener(this);
		mBtnShuimian = (ImageView)(mArrayList.get(0).findViewById(R.id.img_btn_shuimian));
		mBtnShuimian.setOnClickListener(this);
		mBtnKaiguan = (ImageView)(mArrayList.get(0).findViewById(R.id.img_btn_kaiguan));
		mBtnKaiguan.setOnClickListener(this);
		mBtnLvwang = (ToggleButton)(mArrayList.get(0).findViewById(R.id.img_btn_lvwang));
		mBtnLvwang.setOnCheckedChangeListener(this);
		mBtnFengsan = (ImageView)(mArrayList.get(0).findViewById(R.id.img_btn_fengsan));
		mBtnFengsan.setOnClickListener(this);
		mBtnDingshi = (ImageView)(mArrayList.get(0).findViewById(R.id.img_btn_dingshi));
		mBtnDingshi.setOnClickListener(this);
		
		enableAllButton(false);
		
		
		mAirQualityImg = (ImageView)(mArrayList.get(0).findViewById(R.id.img_air_stats));
		
		mLvWANG1 = (TextView)(mArrayList.get(0).findViewById(R.id.text_lvwang_1));
		mLvWANG2 = (TextView)(mArrayList.get(0).findViewById(R.id.text_lvwang_2));
		mLvWANG3 = (TextView)(mArrayList.get(0).findViewById(R.id.text_lvwang_3));
		mLvWANG4 = (TextView)(mArrayList.get(0).findViewById(R.id.text_lvwang_4));
		ShowLvWang(false);
		
		//--------------------------------------------
		// 定义服务Intent
		mServiceIntent = new Intent(this, BleService.class);
	}
	
	/** 设置按钮的状态 */
	private void enableAllButton(boolean enabled){
		mBtnZhineng.setEnabled(enabled);
		mBtnLizi.setEnabled(enabled);
		mBtnHuangjing.setEnabled(enabled);
		mBtnShuimian.setEnabled(enabled);
		mBtnKaiguan.setEnabled(enabled);
		mBtnLvwang.setEnabled(enabled);
		mBtnFengsan.setEnabled(enabled);
		mBtnDingshi.setEnabled(enabled);
		
		if(enabled){
			MainActivity.this.setTitle(mTitle+"(已连接)");
		}
		else{
			MainActivity.this.setTitle(mTitle+"(已断开)");
		}
		
	}
	
	
	@Override
	protected void onStart() {
		super.onStart();
		
		// 绑定服务
		bindService(mServiceIntent, mConnection, BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onStop() {
		if (mServiceMessenger != null) {
			try {
				Message msg = Message.obtain(null, BleService.MSG_UNREGISTER);
				if (msg != null) {
					// 告诉server要回复的信使是UI信使
					msg.replyTo = mMessenger;
					// 发送消息
					mServiceMessenger.send(msg);
				}
			} catch (Exception e) {
				Constant.Log("Error unregistering with BleService"+e);
				mServiceMessenger = null;
			} finally {
				unbindService(mConnection);
			}
		}
		super.onStop();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		mRefreshItem = menu.findItem(R.id.action_refresh);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_refresh) {
			Constant.Log("onOptionsItemSelected  startScan()  mServiceMessenger="+mServiceMessenger);
			// 主动发起扫描
			if (mServiceMessenger != null) {
				startScan();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	
	
	//----------------------------------------------------------------
	/** 连接服务的回调 */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Constant.Log("onServiceConnected");
			mServiceMessenger = new Messenger(service);
			try {
				Message msg = Message.obtain(null, BleService.MSG_REGISTER);
				if (msg != null) {
					msg.replyTo = mMessenger;
					msg.obj = readMacAddress();
					mServiceMessenger.send(msg);
				} else {
					mServiceMessenger = null;
				}
			} catch (Exception e) {
				Constant.Log("Error connecting to BleService"+e);
				mServiceMessenger = null;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Constant.Log("onServiceDisconnected");
			mServiceMessenger = null;
		}
	};
	
	
	/** UI端处理消息的handler */
	private static class IncomingHandler extends Handler {
		private final WeakReference<MainActivity> mActivity;

		public IncomingHandler(MainActivity activity) {
			mActivity = new WeakReference<MainActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			MainActivity activity = mActivity.get();
			if (activity != null) {
				switch (msg.what) {
					case BleService.MSG_STATE_CHANGED: // 状态改变
						activity.stateChanged(BleService.State.values()[msg.arg1]);
						break;
					case BleService.MSG_DEVICE_FOUND: // 找到我们的BLE设备(指定UUID)，这个时候还没开始连接
						Bundle data = msg.getData();
						if (data != null && data.containsKey(BleService.KEY_MAC_ADDRESSES)) {
							String name = data.getString(BleService.KEY_DEVICE_NAME);
							String address = data.getString(BleService.KEY_MAC_ADDRESSES);
							Constant.Log("MSG_DEVICE_FOUND: name="+name+", mac="+address);
							activity.mDevListAdapter.addDevice(name, address);
							activity.mDevListAdapter.notifyDataSetChanged();
						}
						break;
					case BleService.MSG_DEVICE_RECV_DATA: // 接收到数据
						Bundle recvdata = msg.getData();
						String recvStr = recvdata.getString(BleService.KEY_RECV_DATA);
						// 处理接收到的数据
						byte[] recvBytes = Utils.stringToHexBytes(recvStr);
						activity.processRecvData(recvBytes);
						//=================================
						if(activity.mSettingRecvText != null){
							String oldStr = activity.mSettingRecvText.getText().toString();
							activity.mSettingRecvText.setText(oldStr+">>>"+recvStr+"\n");
						}
						break;
				}
			}
			super.handleMessage(msg);
		}
	}
	
	/** 记录板端发过来的数据 */
	private int fengsu = 0x0;
	private boolean shuimian = false;
	private boolean huangjing = false;
	private boolean lizhi = false;
	private boolean zhineng = false;
	private boolean power = false;
	
	private int airQuality = 0x0;
	private int dingshi = 0x0;
	
	private int lvwang1 = 0x0;
	private int lvwang2 = 0x0;
	private int lvwang3 = 0x0;
	private int lvwang4 = 0x0;
	
	/** 处理接收到的数据，反应到按钮状态 */
	private void processRecvData(byte[] recvBytes){
		Constant.Log("processRecvData: recvBytes[0]="+Integer.toHexString(recvBytes[0])+", recvBytes[1]"+Integer.toHexString(recvBytes[1])+
				", recvBytes[2]="+Integer.toHexString(recvBytes[2])+", recvBytes[3]="+Integer.toHexString(recvBytes[3])+", recvBytes[4]="+Integer.toHexString(recvBytes[4]));
		
		if((recvBytes[0]&0xff) == 0xaa){
			
			enableAllButton(true);
			// =================第一个status字节===============
			// 1. 风速
			if((recvBytes[1] & Constant.STATUS1_FENGSU_HIGH) != 0){
				fengsu = 3;
				mBtnFengsan.setBackgroundResource(R.drawable.btn_fengsu_gao);
			}
			else if((recvBytes[1] & Constant.STATUS1_FENGSU_MIDDLE) != 0){
				fengsu = 2;
				mBtnFengsan.setBackgroundResource(R.drawable.btn_fengsu_zhong);
			}
			else if((recvBytes[1] & Constant.STATUS1_FENGSU_LOW) != 0){
				fengsu = 1;
				mBtnFengsan.setBackgroundResource(R.drawable.btn_fengsu_di);
			}
			else{
				fengsu = 0;
				mBtnFengsan.setBackgroundResource(R.drawable.btn_fengsu_wu);
			}
			Constant.Log("processRecvData: fengsu="+fengsu);
			if((recvBytes[1] & Constant.STATUS1_SHUIMIAN) != 0){
				// 睡眠开
				shuimian = true;
				mBtnShuimian.setBackgroundResource(R.drawable.btn_shuimian_on);
			}
			else{
				// 睡眠关
				shuimian = false;
				mBtnShuimian.setBackgroundResource(R.drawable.btn_shuimian_off);
			}
			if((recvBytes[1] & Constant.STATUS1_HUANGJING) != 0){
				// 环境开
				huangjing = true;
				mBtnHuangjing.setBackgroundResource(R.drawable.btn_huanjing_on);
			}
			else{
				// 环境关
				huangjing = false;
				mBtnHuangjing.setBackgroundResource(R.drawable.btn_huanjing_off);
			}
			if((recvBytes[1] & Constant.STATUS1_LIZHI) != 0){
				// 离子开
				lizhi = true;
				mBtnLizi.setBackgroundResource(R.drawable.btn_lizi_on);
			}
			else{
				// 离子关
				lizhi = false;
				mBtnLizi.setBackgroundResource(R.drawable.btn_lizi_off);
			}
			if((recvBytes[1] & Constant.STATUS1_ZHINENG) != 0){
				// 智能开
				zhineng = true;
				mBtnZhineng.setBackgroundResource(R.drawable.btn_zhineng_on);
			}
			else{
				// 智能关
				zhineng = false;
				mBtnZhineng.setBackgroundResource(R.drawable.btn_zhineng_off);
			}
			if((recvBytes[1] & Constant.STATUS1_POWER) != 0){
				// 开关开
				power = true;
				mBtnKaiguan.setBackgroundResource(R.drawable.btn_kaiguan_on);
			}
			else{
				// 开关关
				power = false;
				mBtnKaiguan.setBackgroundResource(R.drawable.btn_kaiguan_off);
			}
			
			
			//=====================第二个status字节=======================================
			// 1.空气质量
			airQuality = ((recvBytes[2] & Constant.STATUS2_AIR_QUALITY_MASK) >> 5)&0x07;
			Constant.Log("processRecvData: airQuality="+airQuality);
			if(airQuality == 0){
				mAirQualityImg.setImageResource(R.drawable.zhiling_you);
			}
			else if(airQuality == 1){
				mAirQualityImg.setImageResource(R.drawable.zhiliang_liang);
			}
			else if(airQuality == 2){
				mAirQualityImg.setImageResource(R.drawable.zhiliang_qingdu);
			}
			else if(airQuality == 3){
				mAirQualityImg.setImageResource(R.drawable.zhiliang_cha);
			}
			else if(airQuality == 4){
				mAirQualityImg.setImageResource(R.drawable.zhiliang_zhongdu);
			}
			else if(airQuality == 5){
				mAirQualityImg.setImageResource(R.drawable.zhiliang_yanzhong);
			}
			
			dingshi = (recvBytes[2] & Constant.STATUS2_DINGSHI_MASK)&0x1f;
			Constant.Log("processRecvData: dingshi="+dingshi);
			if(dingshi == 0){
				// 定时开
				mBtnDingshi.setBackgroundResource(R.drawable.btn_dingshi_wu);
			}
			else if(dingshi == 2){
				// 定时关
				mBtnDingshi.setBackgroundResource(R.drawable.btn_dingshi_2h);
			}
			else if(dingshi == 4){
				// 定时关
				mBtnDingshi.setBackgroundResource(R.drawable.btn_dingshi_4h);
			}
			else if(dingshi == 8){
				// 定时关
				mBtnDingshi.setBackgroundResource(R.drawable.btn_dingshi_8h);
			}
			
			//=====================第三个status字节=======================================
			lvwang1 = (recvBytes[3] & Constant.STATUS3_LVWANG_1_MASK)&0x0f;
			lvwang2 = ((recvBytes[3] & Constant.STATUS3_LVWANG_2_MASK) >> 4&0x0f);
			Constant.Log("processRecvData: lvwang1="+lvwang1+", lvwang2="+lvwang2);
			//=====================第四个status字节=======================================
			lvwang3 = (recvBytes[4] & Constant.STATUS4_LVWANG_3_MASK)&0x0f;
			lvwang4 = ((recvBytes[4] & Constant.STATUS4_LVWANG_4_MASK) >> 4)&0x0f;
			Constant.Log("processRecvData: lvwang3="+lvwang3+", lvwang4="+lvwang4);
			
		}
	}
	

	//------------------------------------------------------------------
	/** 开始蓝牙BLE扫描 */
	private void startScan() {
		mRefreshItem.setEnabled(false);
		//mDeviceList.setDevices(this, null);
		//mDeviceList.setScanning(true);
		mDevListAdapter.clearDevice();
		mDevListAdapter.notifyDataSetChanged();
		// 显示一个连接BLE的进度条
		progressDialog = ProgressDialog.show(this, null,
				"Serach devivce...");
		
		// 发送开始扫描的消息
		Message msg = Message.obtain(null, BleService.MSG_START_SCAN);
		if (msg != null) {
			try {
				mServiceMessenger.send(msg);
			} catch (RemoteException e) {
				Constant.Log("Lost connection to service"+e);
				unbindService(mConnection);
			}
		}
	}
	
	
	/** BLE状态改变 */
	private void stateChanged(BleService.State newState) {
		boolean disconnected = (mState == BleService.State.CONNECTED);
		mState = newState;
		switch (mState) {
			case SCANNING: // 正在扫描
				mRefreshItem.setEnabled(true); 
				//mDeviceList.setScanning(true);
				break;
			case BLUETOOTH_OFF: // 发起扫描后，如果判断到当前是停用蓝牙，则要先打开蓝牙
				if(progressDialog != null){
					progressDialog.dismiss(); // 搜索不到，取消进度条
				}
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, ENABLE_BT);
				break;
			case IDLE:
				if(progressDialog != null){
					progressDialog.dismiss(); // 搜索完成，取消进度条
				}
				if (disconnected) {
					// 连接上了
					Toast.makeText(this, "已断开设备", Toast.LENGTH_SHORT).show();
					mSettingDevName.setText("无设备连接");
					
					enableAllButton(false);
				}
				mRefreshItem.setEnabled(true);
				//mDeviceList.setScanning(false);
				break;
			case CONNECTED: // 设备连接上了，还没开始搜索
				if(progressDialog != null){
					progressDialog.dismiss(); // 搜索完成，取消进度条
				}
				Toast.makeText(this, "已连接上设备", Toast.LENGTH_SHORT).show();
				enableAllButton(true);
				mSettingDevName.setText(currConnectAddress);
				
				// 已经连上一次了，就保存当前的MAC地址
				saveMacAddress(currConnectAddress);
				break;
			default:
				break;
		}
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				startScan(); // 用户开启蓝牙后开始扫描
			} else {
				//The user has elected not to turn on
				//Bluetooth. There's nothing we can do
				//without it, so let's finish().
				finish();
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	
	
	
	/** Viewpager的Adapter */
	private class MyPagerAdapter extends PagerAdapter{

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mArrayList.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			// TODO Auto-generated method stub
			return arg0 == (arg1);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			// TODO Auto-generated method stub
			((ViewPager)container).addView(mArrayList.get(position));
			return mArrayList.get(position);
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			// TODO Auto-generated method stub
			((ViewPager)container).removeView(mArrayList.get(position));
		}
	}


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
			case R.id.btnControl:
				mViewPager.setCurrentItem(0);
				break;
			case R.id.btnSettings:
				mViewPager.setCurrentItem(1);
				break;
			case R.id.btnDevicesList:
				mViewPager.setCurrentItem(2);
				break;
			//------------------------------------
			case R.id.btn_sendMsg:
				sendTheData();
				break;
				
			//------------发送控制命令-----------------------
			case R.id.img_btn_zhineng:
				//Toast.makeText(this, "智能"+isChecked, Toast.LENGTH_SHORT).show();
				sendTheData(Constant.OP_ZHINENG);
				break;
			case R.id.img_btn_lizi:
				//Toast.makeText(this, "离子"+isChecked, Toast.LENGTH_SHORT).show();
				sendTheData(Constant.OP_LIZHI);
				break;
			case R.id.img_btn_huangjing:
				//Toast.makeText(this, "环境"+isChecked, Toast.LENGTH_SHORT).show();
				sendTheData(Constant.OP_HUANGJING);
				break;
			case R.id.img_btn_shuimian:
				//Toast.makeText(this, "睡眠"+isChecked, Toast.LENGTH_SHORT).show();
				sendTheData(Constant.OP_SHUIMIAN);
				break;
			case R.id.img_btn_kaiguan:
				//Toast.makeText(this, "开关"+isChecked, Toast.LENGTH_SHORT).show();
				sendTheData(Constant.OP_POWER);
				break;
			//case R.id.img_btn_lvwang:
				//Toast.makeText(this, "滤网"+isChecked, Toast.LENGTH_SHORT).show();
				//break;
			case R.id.img_btn_fengsan:
				//Toast.makeText(this, "风扇"+isChecked, Toast.LENGTH_SHORT).show();
				sendTheData(Constant.OP_FENGSU);
				break;
			case R.id.img_btn_dingshi:
				//Toast.makeText(this, "定时"+isChecked, Toast.LENGTH_SHORT).show();
				sendTheData(Constant.OP_DINGSHI);
				break;
			//-----------------------------------
			default:
				break;
		}
	}

	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		if(buttonView.getId() != R.id.img_btn_lvwang){
			return;
		}
		
		ShowLvWang(isChecked);
	}

	private void ShowLvWang(boolean isShow){
		if(isShow){
			mLvWANG1.setText("滤网一：\n"+Integer.toString(lvwang1*10)+"%");
			mLvWANG2.setText("滤网二：\n"+Integer.toString(lvwang2*10)+"%");
			mLvWANG3.setText("滤网三：\n"+Integer.toString(lvwang3*10)+"%");
			mLvWANG4.setText("滤网四：\n"+Integer.toString(lvwang4*10)+"%");
			mLvWANG1.setVisibility(View.VISIBLE);
			mLvWANG2.setVisibility(View.VISIBLE);
			mLvWANG3.setVisibility(View.VISIBLE);
			mLvWANG4.setVisibility(View.VISIBLE);
		}
		else{
			mLvWANG1.setVisibility(View.INVISIBLE);
			mLvWANG2.setVisibility(View.INVISIBLE);
			mLvWANG3.setVisibility(View.INVISIBLE);
			mLvWANG4.setVisibility(View.INVISIBLE);
		}
		
	}
	
	
	
	/** 发送命令 */
	private void sendTheData(String operation) {
		// TODO Auto-generated method stub
		if(mState != BleService.State.CONNECTED){
			Toast.makeText(this, "未连上设备，不能发送数据", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Message msg = Message.obtain(null, BleService.MSG_DEVICE_SEND_DATA);
		if (msg != null) {
			msg.obj = Constant.OP_HEAD + operation;
			Constant.Log("sendTheData: sendStr="+msg.obj);
			try {
				mServiceMessenger.send(msg);
			} catch (RemoteException e) {
				Constant.Log("Lost connection to service"+e);
				unbindService(mConnection);
			}
		}
	}
	
	
	
	
	/** APP发送数据给BLE */
	private void sendTheData() {
		// TODO Auto-generated method stub
		String sendStr = mSettingSendText.getText().toString().trim(); //"A5000102030405060708090a0b0c0d0e0f102030";
		if(mState != BleService.State.CONNECTED){
			Toast.makeText(this, "未连上设备，不能发送数据", Toast.LENGTH_SHORT).show();
			return;
		}
		if(sendStr == null || sendStr.length() <= 0){
			Toast.makeText(this, "请输入要发送的内容！", Toast.LENGTH_SHORT).show();
			return;
		}
		if(sendStr.length() != 40){
			Toast.makeText(this, "输入的数据长度不满足20个字节", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mSettingSendText.setText("");
		
		Message msg = Message.obtain(null, BleService.MSG_DEVICE_SEND_DATA);
		if (msg != null) {
			msg.obj = sendStr;
			Constant.Log("sendTheData: sendStr="+sendStr);
			try {
				mServiceMessenger.send(msg);
			} catch (RemoteException e) {
				Constant.Log("Lost connection to service"+e);
				unbindService(mConnection);
			}
		}
	}



    /** 保存MAC地址 */
    public void saveMacAddress(String mac){
    	Constant.Log("saveMacAddress");
        SharedPreferences.Editor spe = mSp.edit();
        spe.putString(Constant.SAVE_MAC_ADDRESS, mac);
        spe.commit();
    }
    
    /** 读取MAC地址 */
    public String readMacAddress(){
    	Constant.Log("readMacAddress");
        return mSp.getString(Constant.SAVE_MAC_ADDRESS, "");
    }
	
	
	
	
}
