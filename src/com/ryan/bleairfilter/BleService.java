package com.ryan.bleairfilter;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;


public class BleService extends Service implements BluetoothAdapter.LeScanCallback {
	public static final String TAG = "BleService";
	
	static final int MSG_REGISTER = 1;  // UI注册
	static final int MSG_UNREGISTER = 2;  // UI取消注册
	static final int MSG_START_SCAN = 3;   // 开始扫描BLE
	static final int MSG_STATE_CHANGED = 4;  // 状态改变
	static final int MSG_DEVICE_FOUND = 5;   // 设备找到
	static final int MSG_DEVICE_CONNECT = 6;  // 连接设备
	static final int MSG_DEVICE_DISCONNECT = 7; // 断开设备
	static final int MSG_DEVICE_RECV_DATA = 8;  // 设备数据
	static final int MSG_DEVICE_SEND_DATA = 8;  // 发送数据
	
	private static final long SCAN_PERIOD = 5000;

	public static final String KEY_MAC_ADDRESSES = "KEY_MAC_ADDRESSES";
	public static final String KEY_DEVICE_NAME = "KEY_DEVICE_NAME";
	public static final String KEY_RECV_DATA = "KEY_RECV_DATA";
	
	/** BLE中所需要的服务 */
	private static final UUID UUID_AIRFILTER_SERVICE = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
	/** BLE中read/notify的特征值 */
	private static final UUID UUID_AIRFILTER_READ_NOTIFY = UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb");
	/** BLE中write的特征值 */
	private static final UUID UUID__AIRFILTER_WRITE = UUID.fromString("0000ffe9-0000-1000-8000-00805f9b34fb");
	//private static final UUID UUID_CCC = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
	//private static final byte[] ENABLE_SENSOR = {0x01};

	// 写数据的队列
	private static final Queue<Object> sWriteQueue = new ConcurrentLinkedQueue<Object>();
	private static boolean sIsWriting = false;

	private final IncomingHandler mHandler;
	// 服务端的信息
	private final Messenger mMessenger;
	// 客户端的信息，这里用list表示允许有多个客户端
	private final List<Messenger> mClients = new LinkedList<Messenger>();
	// 保存搜索到的蓝牙设备
	private final Map<String, BluetoothDevice> mDevices = new HashMap<String, BluetoothDevice>();
	// 蓝牙GATT,可以理解成对应一个BLE设备
	private BluetoothGatt mGatt = null;

	public enum State {
		UNKNOWN,
		IDLE,
		SCANNING,
		BLUETOOTH_OFF, 
		CONNECTING,
		CONNECTED,
		DISCONNECTING
	}

	// 蓝牙适配器，可以理解成手机蓝牙设备
	private BluetoothAdapter mBluetooth = null;
	private State mState = State.UNKNOWN;

	// 对应读写的特征值
	BluetoothGattCharacteristic bleReadNotifyCharacteristic;
	BluetoothGattCharacteristic bleWriteCharacteristic;
	
	
	/** 所有BLE设备状态改变，读写数据，通知的总回调 */
	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		// 连接状态改变时的回调
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			Constant.Log("[callback]Connection State Changed: " + (newState == BluetoothProfile.STATE_CONNECTED ? "Connected" : "Disconnected"));
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				setState(State.CONNECTED); // 连接上设备后立即开始搜索服务
				gatt.discoverServices();
			} else {
				setState(State.IDLE);
				bleReadNotifyCharacteristic = null;
				bleWriteCharacteristic = null;
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			Constant.Log("[callback]onServicesDiscovered: " + status);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				subscribe(gatt);
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Constant.Log("[callback]onCharacteristicWrite: " + status);
			sIsWriting = false;
			nextWrite();
		}
		
		@Override
		// Result of a characteristic read operation
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Constant.Log("[callback]onCharacteristicRead:"+gatt.getDevice().getName()
						+" read "
						+characteristic.getUuid().toString()
						+" -> "
						+Utils.bytesToHexString(characteristic.getValue()));
			}
		}
		
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			Constant.Log("[callback]onCharacteristicChanged: " + characteristic.getUuid());
			if (characteristic.getUuid().equals(UUID_AIRFILTER_READ_NOTIFY)) {
				String recvStr = Utils.bytesToHexString(characteristic.getValue());
				Constant.Log("[callback]onCharacteristicChanged:"+recvStr);
				
				Bundle bundle = new Bundle();
				bundle.putString(KEY_RECV_DATA, recvStr);
				Message msg = Message.obtain(null, MSG_DEVICE_RECV_DATA);
				msg.setData(bundle);
				sendMessage(msg);
			}
		}
	};

	/** 构造器 */
	public BleService() {
		mHandler = new IncomingHandler(this);
		mMessenger = new Messenger(mHandler);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	/** 服务端处理消息的handler */
	private static class IncomingHandler extends Handler {
		private final WeakReference<BleService> mService;

		public IncomingHandler(BleService service) {
			mService = new WeakReference<BleService>(service);
		}

		@Override
		public void handleMessage(Message msg) {
			BleService service = mService.get();
			if (service != null) {
				switch (msg.what) {
					case MSG_REGISTER: // UI发送注册消息
						//Constant.Log("UI Registered+++");
						service.mClients.add(msg.replyTo); // 把UI的信使保存
						String macAddress = (String) msg.obj;
						Constant.Log("UI Registered+++ macAddress="+macAddress+", service.mState="+service.mState);
						if(macAddress != null && macAddress.length() > 6){
							if (service.mState != State.CONNECTED) {
								service.connect(macAddress); // 断开BLE设备
							}
						}
						break;
					case MSG_UNREGISTER:
						Constant.Log("UI Unegistered---");
						service.mClients.remove(msg.replyTo); // 把UI的信使删除
						if (service.mState == State.CONNECTED && service.mGatt != null) {
							service.mGatt.disconnect(); // 断开BLE设备
						}
						break;
					case MSG_START_SCAN:
						Constant.Log("Start Scan"); 
						service.startScan();  // 开始扫描BLE设备
						break;
					case MSG_DEVICE_CONNECT:
						service.connect((String) msg.obj); // 连接BLE设备
						break;
					case MSG_DEVICE_DISCONNECT:
						if (service.mState == State.CONNECTED && service.mGatt != null) {
							service.mGatt.disconnect(); // 断开BLE设备
						}
						break;
					case MSG_DEVICE_SEND_DATA:
						Constant.Log("MSG_DEVICE_SEND_DATA: Write string = "+ (String)msg.obj); 
						if(service.bleWriteCharacteristic != null){
							byte[] sendvalue = Utils.stringToHexBytes((String) msg.obj);
							//for(byte c : sendvalue){
							//	Constant.Log("MSG_DEVICE_SEND_DATA:  c= "+ c); 
							//}
							Constant.Log("MSG_DEVICE_SEND_DATA: Write OK!!!"); 
							service.bleWriteCharacteristic.setValue(sendvalue);
							service.write(service.bleWriteCharacteristic); // 连接BLE设备
						}
						break;	
						
					default:
						super.handleMessage(msg);
				}
			}
		}
	}

	/** 服务端启动扫描BLE */
	private void startScan() {
		mDevices.clear();
		// 设置状态机，并通知UI端当前处于扫描状态
		setState(State.SCANNING);
		if (mBluetooth == null) {
			BluetoothManager bluetoothMgr = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
			mBluetooth = bluetoothMgr.getAdapter();
		}
		if (mBluetooth == null || !mBluetooth.isEnabled()) {
			setState(State.BLUETOOTH_OFF); // 蓝牙处于关闭状态
		} else {
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (mState == State.SCANNING) {
						mBluetooth.stopLeScan(BleService.this);
						setState(State.IDLE); // 如果还处于扫描状态，就停止扫描，扫描后回到idle，等待连接connect!
					}
				}
			}, SCAN_PERIOD);
			mBluetooth.startLeScan(this); // 开始扫描
		}
	}

	
	@Override
	/** 扫描BLE设备的回调函数，如果扫描到了就会改变状态机 */
	public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
		// 1.搜索到设备不为空； 2.之前的列表里面没有包括； 3.设备的名字不为空；
		// 4. [注意]搜索到的设备的名字要和我们指定的设备名字一致！！！！！！！
		if (device != null && !mDevices.containsValue(device) && device.getName() != null /*&& device.getName().equals(DEVICE_NAME)*/) {
			// 停止扫描
			if (mState == State.SCANNING) {
				mBluetooth.stopLeScan(BleService.this);
				setState(State.IDLE); // 如果还处于扫描状态，就停止扫描，扫描后回到idle，等待连接connect!
			}
			
			
			mDevices.put(device.getAddress(), device); // 保存找到的设备
			// 通知UI端找到设备
			Message msg = Message.obtain(null, MSG_DEVICE_FOUND);
			if (msg != null) {
				Bundle bundle = new Bundle();
				bundle.putString(KEY_MAC_ADDRESSES, device.getAddress());
				bundle.putString(KEY_DEVICE_NAME, device.getName());
				msg.setData(bundle);
				sendMessage(msg);
			}
			Constant.Log("Added " + device.getName() + ": " + device.getAddress());
		}
	}

	/** 通过MAC地址去连接设备 */
	public void connect(String macAddress) {
		BluetoothDevice device = mDevices.get(macAddress);
		Constant.Log("now call the connect, device="+device);
		if (device != null) {
			// 开始连接设备，所有后续的状态，数据等都通过回调处理
			mGatt = device.connectGatt(this, true, mGattCallback); 
		}
	}


	/** 连接上设备后，调用discoverServices搜索服务，回调onServicesDiscovered，找到服务后对应的处理 */
	private void subscribe(BluetoothGatt gatt) {
		Constant.Log("subscribe=====>>>>mState="+mState);
		if(mState != State.CONNECTED){
			Constant.Log("subscribe====the state is disconneted!!!!!!!!");
			return;
		}
		
		BluetoothGattService bleAirFilterService = gatt.getService(UUID_AIRFILTER_SERVICE);
		if (bleAirFilterService != null && bleReadNotifyCharacteristic == null && bleWriteCharacteristic == null) {
			bleReadNotifyCharacteristic = bleAirFilterService.getCharacteristic(UUID_AIRFILTER_READ_NOTIFY);
			bleWriteCharacteristic = bleAirFilterService.getCharacteristic(UUID__AIRFILTER_WRITE);
			
			if (bleReadNotifyCharacteristic != null && bleWriteCharacteristic != null) {
				// 设置通知
				gatt.setCharacteristicNotification(bleReadNotifyCharacteristic, true);
				final String macStr = Utils.macToString(gatt.getDevice().getAddress().toString());
				Constant.Log("subscribe=====>>>>macStr="+macStr);
				// 延时读数据
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						// 写数据--MAC地址来连接设备
						byte[] head = {'M','A','C','+','0','0','0','0','0','0'};
						byte[] number = Utils.stringToHexBytes(macStr);
						System.arraycopy(number, 0, head, 4, number.length);
						//Constant.Log("subscribe=====>>>>bleWriteCharacteristic="+bleWriteCharacteristic+", head="+head);
						if(bleWriteCharacteristic !=null){
							bleWriteCharacteristic.setValue(head);
							write(bleWriteCharacteristic);
						}
					}
				},400);
				
				
				// 延时读数据
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						read(bleReadNotifyCharacteristic);
					}
				},800);
				

			}
		}
	}

	
	//-----------------------------------------------------------------------
	/** 读特征值和读描述符 */
	public synchronized void read(Object o) {
		if (mBluetooth == null || mGatt == null) {
			Constant.Log("BluetoothAdapter not initialized");
			return;
		}
		
		if (o instanceof BluetoothGattCharacteristic) {
			mGatt.readCharacteristic((BluetoothGattCharacteristic) o);
		} else if (o instanceof BluetoothGattDescriptor) {
			mGatt.readDescriptor((BluetoothGattDescriptor) o);
		} 
	}
	
	/** 可以写特征值和描述符，这里参数就是传递特征值和描述符；具体要发送什么value，则是在之前就组装好了 */
	private synchronized void write(Object o) {
		if (mBluetooth == null || mGatt == null) {
			Constant.Log("BluetoothAdapter not initialized");
			return;
		}
		
		if (sWriteQueue.isEmpty() && !sIsWriting) {
			doWrite(o);
		} else {
			sWriteQueue.add(o);
		}
	}

	private synchronized void nextWrite() {
		if (!sWriteQueue.isEmpty() && !sIsWriting) {
			doWrite(sWriteQueue.poll());
		}
	}

	private synchronized void doWrite(Object o) {
		if (o instanceof BluetoothGattCharacteristic) {
			sIsWriting = true;
			mGatt.writeCharacteristic((BluetoothGattCharacteristic) o);
		} else if (o instanceof BluetoothGattDescriptor) {
			sIsWriting = true;
			mGatt.writeDescriptor((BluetoothGattDescriptor) o);
		} else {
			nextWrite();
		}
	}

	
	//-----------------------------------------------------------------------
	/** 设置当前状态机，并且通知UI端 */
	private void setState(State newState) {
		if (mState != newState) {
			mState = newState;
			Message msg = getStateMessage();
			if (msg != null) {
				sendMessage(msg);
			}
		}
	}

	private Message getStateMessage() {
		Message msg = Message.obtain(null, MSG_STATE_CHANGED);
		if (msg != null) {
			msg.arg1 = mState.ordinal(); // 返回枚举类型的位置(数值)
		}
		return msg;
	}

	/** 获取所有客户端的信使，发现消息 */
	private void sendMessage(Message msg) {
		for (int i = mClients.size() - 1; i >= 0; i--) {
			Messenger messenger = mClients.get(i);
			if (!sendMessage(messenger, msg)) {
				mClients.remove(messenger);
			}
		}
	}

	private boolean sendMessage(Messenger messenger, Message msg) {
		boolean success = true;
		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			Constant.Log("Lost connection to client"+e);
			success = false;
		}
		return success;
	}

	
	// 获取特征值，组成一个16位的整型
	/*
	private static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic characteristic, int offset) {
		Integer lowerByte = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
		Integer upperByte = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1);
		return (upperByte << 8) + lowerByte;
	}
	*/
	
	
}