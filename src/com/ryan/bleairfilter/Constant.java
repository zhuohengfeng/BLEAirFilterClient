package com.ryan.bleairfilter;

import android.util.Log;

public class Constant {

	/** LOG标签 */
	public static final String TAG = "zhfzhf";
	
	public static final boolean DEBUG = true;
	
	public static void Log(String str){
		if(DEBUG){
			Log.i(TAG, str);
		}
	}
	
	
	public static final String SAVE_MAC_ADDRESS = "save_mac_address";
	
	// APP -----> BLE
	/*
	BIT7: 定时键
	0 : 无
	1 : 定时键被按下
	BIT6: 风速键
	0 : 无
	1 : 风速键被按下
	BIT5: 滤网
	0 : 无
	1 : 滤网键被按下
	BIT4: 睡眠
	0 : 无
	1 : 睡眠键被按下
	BIT3: 环境
	0 : 无
	1 : 环境键被按下
	BIT2: 离子
	0 : 无
	1 : 离子键被按下
	BIT1: 智能
	0 : 无
	1 : 智能键被按下
	BIT0: 开关
	0 : 无
	1 : 开关键被按下
	 */
	/** 发送的命令 */
	public static final String OP_HEAD = "AA";  
	/** 开关键被按下*/
	public static final String OP_POWER = "01"; 
	/** 智能键被按下*/
	public static final String OP_ZHINENG = "02";
	/** 离子键被按下*/
	public static final String OP_LIZHI = "04"; 
	/** 环境键被按下*/
	public static final String OP_HUANGJING = "08";
	/** 睡眠键被按下*/
	public static final String OP_SHUIMIAN = "10";
	/** 滤网键被按下*/
	public static final String OP_LVWANG = "20";
	/** 风速键被按下*/
	public static final String OP_FENGSU = "40";
	/** 定时键被按下*/
	public static final String OP_DINGSHI = "80";
	
	
	
	
	// BLE ----> APP
	/*
	BIT7: 风速高
	0 : 无
	1 : 风速高
	BIT6: 风速中
	0 : 无
	1 : 风速中
	BIT5: 风速低
	0 : 无
	1 : 风速低
	
	BIT4: 睡眠
	0 : 无
	1 : 睡眠功能打开
	BIT3: 环境
	0 : 无
	1 : 环境灯亮
	BIT2: 离子
	0 : 离子功能关闭
	1 : 离子功能打开
	BIT1: 智能
	0 : 智能模式关闭
	1 : 智能模式打开
	BIT0: 开关
	0 : 机器关闭
	1 : 机器工作

	*/
	public static final byte STATUS1_FENGSU_HIGH = (byte) (0x80); 
	public static final byte STATUS1_FENGSU_MIDDLE = (byte) (0x40); 
	public static final byte STATUS1_FENGSU_LOW = (byte) (0x20); 
	
	public static final byte STATUS1_SHUIMIAN = (byte) (1<<4); 
	public static final byte STATUS1_HUANGJING = (byte) (1<<3); 
	public static final byte STATUS1_LIZHI = (byte) (1<<2); 
	public static final byte STATUS1_ZHINENG = (byte) (1<<1); 
	public static final byte STATUS1_POWER = (byte) (1<<0); 
	
	
	/*
	BIT7-5: 空气状况
	    000：优-----------------绿色
	    001：良-----------------黄色
	    010：轻度污染--------黄褐
	    011：中度污染--------红色
	    100：重度污染--------紫色
	    101：严重污染--------深褐色

	BIT4-0: 定时(0-31h)
		00000: 无定时
		00001: 定时1小时
		00010: 定时2小时
	*/
	public static final byte STATUS2_AIR_QUALITY_MASK = (byte) (0xE0); 
	
	public static final byte STATUS2_DINGSHI_MASK = (byte) (0x1F); 
	
	/*
	BIT7-4: 滤网2时间消耗占比
	    0000: 滤网消耗时间占比为0%（新滤网，进度条为满格）
	    0001: 滤网消耗时间占比为10%
	    0010: 滤网消耗时间占比为20%
	    ……
	    1010: 滤网消耗时间占比为100%
    BIT3-0: 滤网1时间消耗占比
	    0000: 滤网消耗时间占比为0%（新滤网，进度条为满格）
	    0001: 滤网消耗时间占比为10%
	    0010: 滤网消耗时间占比为20%
	    ……
	    1010: 滤网消耗时间占比为100%
    */
	public static final byte STATUS3_LVWANG_2_MASK = (byte) (0xF0); 
	public static final byte STATUS3_LVWANG_1_MASK = (byte) (0x0F); 
	
	/*
	BIT7-4: 滤网4时间消耗占比
	    0000: 滤网消耗时间占比为0%（新滤网，进度条为满格）
	    0001: 滤网消耗时间占比为10%
	    0010: 滤网消耗时间占比为20%
	    ……
	    1010: 滤网消耗时间占比为100%
	BIT3-0: 滤网3时间消耗占比
	    0000: 滤网消耗时间占比为0%（新滤网，进度条为满格）
	    0001: 滤网消耗时间占比为10%
	    0010: 滤网消耗时间占比为20%
	    ……
		1010: 滤网消耗时间占比为100%
	 */
	
	public static final byte STATUS4_LVWANG_4_MASK = (byte) (0xF0); 
	public static final byte STATUS4_LVWANG_3_MASK = (byte) (0x0F); 
	
	
}
