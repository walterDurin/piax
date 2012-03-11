package jp.android_group.payforward.monac;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.piax.ov.jmes.MessageData;
import org.piax.ov.jmes.von.VONEntry;
import org.piax.ov.jmes.MessageSecurityManager;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class Sensor extends Activity implements Runnable {
    static double currentTemp = Double.NaN; 
    static double currentLight = Double.NaN; 
    static double currentCo2 = Double.NaN; 
    static double currentPress = Double.NaN; 
    static double currentHumi = Double.NaN; 
    
    TextView tempText;
    TextView lightText;
    TextView co2Text;
    TextView pressText;
    TextView humiText;
    TextView dateText;

    private static final String TAG = "Sensor";
    private static final String ACTION_USB_PERMISSION = "jp.android_group.payforward.monac.Sensor.action.USB_PERMISSION";

    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;

	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;

	private static final int MESSAGE_PRESSURE = 1;
	private static final int MESSAGE_TEMPERATURE = 2;
	private static final int MESSAGE_LIGHT = 3;
	private static final int MESSAGE_CO2 = 4;
	private static final int MESSAGE_HUMIDITY = 5;

	public static final byte LED_SERVO_COMMAND = 2;
	public static final byte RELAY_COMMAND = 3;

    protected class TemperatureMsg {
        private int temperature;

        public TemperatureMsg(int temperature) {
            this.temperature = temperature;
        }

        public int getTemperature() {
            return temperature;
        }
    }

    protected class LightMsg {
        private int light;

        public LightMsg(int light) {
            this.light = light;
        }

        public int getLight() {
            return light;
        }
    }
    
    protected class PressureMsg {
        private int pressure;

        public PressureMsg(int pressure) {
            this.pressure = pressure;
        }

        public int getPressure() {
            return pressure;
        }
    }
    
    protected class Co2Msg {
        private int co2;

        public Co2Msg(int co2) {
            this.co2 = co2;
        }

        public int getCo2() {
            return co2;
        }
    }
    
    protected class HumidityMsg {
        private int humidity;

        public HumidityMsg(int humidity) {
            this.humidity = humidity;
        }

        public int getHumidity() {
            return humidity;
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
	    public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                        if (intent.getBooleanExtra(
                                                   UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            openAccessory(accessory);
                        } else {
                            Log.d(TAG, "permission denied for accessory "
                                  + accessory);
                        }
                        mPermissionRequestPending = false;
                    }
                } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    //UsbAccessory accessory = UsbManager.getAccessory(intent);
                    if (accessory != null && accessory.equals(mAccessory)) {
                        closeAccessory();
                    }
                    tempText.setText("Temp: --");
                    lightText.setText("Light: --");
                    co2Text.setText("CO2: --");
                    pressText.setText("Press: --");
                    humiText.setText("Humidity: --");
                }
            }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        if (getLastNonConfigurationInstance() != null) {
            mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
            openAccessory(mAccessory);
        }

        setContentView(R.layout.dummy_sensor);

        Button setButton = (Button) findViewById(R.id.value_button);
        tempText = (TextView) findViewById(R.id.temp_value);
        lightText = (TextView) findViewById(R.id.light_value);
        co2Text = (TextView) findViewById(R.id.co2_value);
        pressText = (TextView) findViewById(R.id.press_value);
        humiText = (TextView) findViewById(R.id.humid_value);
        dateText = (TextView) findViewById(R.id.date_value);
        setButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                sendCurrentValue();
            }
        });
        if (Double.isNaN(currentTemp)) {
            tempText.setText("Temp: --");
        }
        else {
            tempText.setText("Temp:" + currentTemp);
        }
        if (Double.isNaN(currentLight)) {
            lightText.setText("Light: --");
        }
        else {
            lightText.setText("Light:" + currentLight);
        }
        if (Double.isNaN(currentCo2)) {
            co2Text.setText("CO2: --");
        }
        else {
            co2Text.setText("CO2:" + currentCo2);
        }
        if (Double.isNaN(currentPress)) {
            pressText.setText("Press: --");
        }
        else {
            pressText.setText("Press:" + currentPress);
        }
        if (Double.isNaN(currentHumi)) {
            humiText.setText("Humidity: --");
        }
        else {
            humiText.setText("Humidity:" + currentHumi);
        }
        
        if (Monac.nfc != null) {
            Monac.nfc.setActivity(this);
            Monac.nfc.setupNotificationResource(R.raw.touched);
        }
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mAccessory != null) {
            return mAccessory;
        } else {
            return super.onRetainNonConfigurationInstance();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (Monac.nfc != null) {
            Monac.nfc.onNewIntent(intent);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (Monac.nfc != null) {
            Monac.nfc.onPause();
        }
        closeAccessory();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (Monac.nfc != null) {
            Monac.nfc.setActivity(this);
            Monac.nfc.onResume();
        }
        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory)) {
                openAccessory(accessory);
            } else {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory,
                                                      mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            Log.d(TAG, "mAccessory is null");
        }
    }

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
            Thread thread = new Thread(null, this, "DemoKit");
            thread.start();
            Log.d(TAG, "accessory opened");
            enableControls(true);
        } else {
            Log.d(TAG, "accessory open fail");
        }
    }

    private void closeAccessory() {
        enableControls(false);
        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    protected void enableControls(boolean enable) {
    }
    
    protected void handleLightMessage(LightMsg l) {
        currentLight = l.getLight();
        lightText.setText("Light:" + currentLight);
    }

    protected void handleTemperatureMessage(TemperatureMsg t) {
        currentTemp = t.getTemperature() / 100.0;
        tempText.setText("Temp:" + currentTemp);
    }
    
    protected void handleCo2Message(Co2Msg c) {
        currentCo2 = c.getCo2();
        co2Text.setText("CO2:" + currentCo2);
        dateText.setText("Last Detected:" + new Date().toString());
    }
    
    protected void handlePressureMessage(PressureMsg p) {
        currentPress = p.getPressure();
        pressText.setText("Pressure:" + currentPress);
    }
    
    protected void handleHumidityMessage(HumidityMsg h) {
        currentHumi = h.getHumidity();
        humiText.setText("Humidity:" + currentHumi);
    }

    Handler mHandler = new Handler() {
        @Override
	    public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                case MESSAGE_TEMPERATURE:
                    TemperatureMsg t = (TemperatureMsg) msg.obj;
                    handleTemperatureMessage(t);
                    break;
                    
                case MESSAGE_CO2:
                    Co2Msg c = (Co2Msg) msg.obj;
                    handleCo2Message(c);
                    break;
                case MESSAGE_PRESSURE:
                    PressureMsg p = (PressureMsg) msg.obj;
                    handlePressureMessage(p);
                    break;
                case MESSAGE_HUMIDITY:
                    HumidityMsg h = (HumidityMsg) msg.obj;
                    handleHumidityMessage(h);
                    break;
                case MESSAGE_LIGHT:
                    LightMsg l = (LightMsg) msg.obj;
                    handleLightMessage(l);
                    break;
                }
            }
    };

    private int composeInt(byte hi, byte lo) {
        int val = (int) hi & 0xff;
        val *= 256;
        val += (int) lo & 0xff;
        return val;
    }

	public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		while (ret >= 0) {
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}

			i = 0;
			while (i < ret) {
				int len = ret - i;

				switch (buffer[i]) {
				case 0x2:
					if (len >= 3) {
						android.os.Message m = android.os.Message.obtain(mHandler,
								                                         MESSAGE_TEMPERATURE);
						m.obj = new TemperatureMsg(composeInt(buffer[i + 1],
								buffer[i + 2]));
						mHandler.sendMessage(m);
					}
					i += 3;
					break;
				case 0x3:
                    if (len >= 3) {
                        android.os.Message m = android.os.Message.obtain(mHandler,
                                                                         MESSAGE_PRESSURE);
                        m.obj = new PressureMsg(composeInt(buffer[i + 1],
                                buffer[i + 2]));
                        mHandler.sendMessage(m);
                    }
                    i += 3;
                    break;

				case 0x4:
					if (len >= 3) {
						android.os.Message m = android.os.Message.obtain(mHandler, MESSAGE_LIGHT);
						m.obj = new LightMsg(composeInt(buffer[i + 1],
								buffer[i + 2]));
						mHandler.sendMessage(m);
					}
					i += 3;
					break;
				
				case 0x5:
				    if (len >= 3) {
                        android.os.Message m = android.os.Message.obtain(mHandler,
                                                                         MESSAGE_CO2);
                        m.obj = new Co2Msg(composeInt(buffer[i + 1],
                                buffer[i + 2]));
                        mHandler.sendMessage(m);
                    }
                    i += 3;
                    break;
                    
				case 0x6:
                    if (len >= 3) {
                        android.os.Message m = android.os.Message.obtain(mHandler,
                                                                         MESSAGE_HUMIDITY);
                        m.obj = new HumidityMsg(composeInt(buffer[i + 1],
                                buffer[i + 2]));
                        mHandler.sendMessage(m);
                    }
                    i += 3;
                    break;
				default:
					Log.d(TAG, "unknown msg: " + buffer[i]);
					i = len;
					break;
				}
			}

		}
	}
    
    void sendCurrentValue() {
        MessageData mes = new MessageData("Temp:" + currentTemp + ", CO2:" + currentCo2 + ", Pressure:" + currentPress + ", Humidity:" + currentHumi + ", Light:" + currentLight + " at " + new Date().toString());
        mes.status = "tweet=require";
        
        VONEntry entry = null;
        if (Monac.dtn == null) {
         //   alert(getString(R.string.internal_error) + "\nNo DTN exists");
        }
        else {
            if (Monac.dtn.getSecurityManager() != null) {
                entry = ((MessageSecurityManager)Monac.dtn.getSecurityManager()).getLatestVONEntry();
            }
            else {
                //alert(getString(R.string.internal_error) + "\nDTN is not started");
            }
        }
        Monac.dtn.newMessage(mes);
    }
}
