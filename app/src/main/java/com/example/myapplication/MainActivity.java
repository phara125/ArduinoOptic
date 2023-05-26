package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    Button startButton, sendButton, clearButton, stopButton;
    TextView Sensor, Log, Timer;
    EditText editText;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    Pattern pattern = Pattern.compile("\\d+");

    int resistorValue;
    boolean mode = true;

    long timer, t, sleep;
    long tstart = System.currentTimeMillis();
    int m, s, ms;
    String strms, strs, strm;

    String svms = "00"; // сотые доли секунд
    String svs = "00"; // секунды
    String svm = "00"; // минуты

    // Для bullet
    Drawable bulletDrawableSensor, bulletDrawableTimer;
    int bulletColorRed, bulletColorGreen;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                LoopFunc(data);
                textShow(Sensor, data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    private void LoopFunc(String data) {
        // время
        t = System.currentTimeMillis(); // считываем время работы ардуинки
        timer = (t - tstart); // берем разницу полного времени работы и времени нажатия кнопки и получаем время, которое прошло после нажатия

        s = (int) Math.floor((timer - m * 60000) / 1000); // высчитываем секунды
        m = (int) Math.floor(timer / 60000); // высчитываем минуты
        ms = (int) (timer % 1000) / 10; // высчитываем сотые доли секунды

        // преобразуем в удобный вывод
        // Если число меньше 10, то в нем 1 символ, поэтому, преобразуем в текст и добавляем спереди символ "0"
        if (ms < 10) strms = "0" + ms;
        else strms = String.valueOf(ms);
        if (s < 10) strs = "0" + s;
        else strs = String.valueOf(s);
        if (m < 10) strm = "0" + m;
        else strm = String.valueOf(m);

        // тут мы будем показывать то, что находится на паузе
        if (mode) // если программа в состоянии стоп/пауза mode=true
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setBulletRed(bulletDrawableTimer);
                }
            });

            String disptime = svm + "." + svs + "." + svms; // формируем строку вывода
            textShow(Timer, disptime); // вывод на дисплей

            tstart = t; // тут сохраняем время нажатия кнопки, дабы с помощью разницы от общего времени работы вычислить сколько работает секундомер
            m = s = ms = 0; // сбрасываем значения на 0, они нам больше не нужны ибо мы их сохранили в mode=false
        }

        if (!mode) // если программа находится в состоянии работы mode=false
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setBulletGreen(bulletDrawableTimer);
                }
            });

            // выводим динамически циферки
            String disptime = svm + "." + svs + "." + svms; // формируем строку вывода
            textShow(Timer, disptime); // вывод на дисплей

            svms = strms; // сохраняем в переменную милисекунды ибо неожиданно может быть нажата кнопка остановки
            svs = strs; // так же сохраняем секунды
            svm = strm; // и минуты
        }

        // обработка кнопки старт/стоп
        // Получение в переменную resistorValue показаний датчика
        resistorValue = 0;
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            String extractedNumber = matcher.group();
            resistorValue = Integer.parseInt(extractedNumber);
        }

        // вывод resistorValue на Sensor
        if (resistorValue < 500) // если на барьере старт/стоп появился логический 0
        {
            // Делаем зеленым
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setBulletGreen(bulletDrawableSensor);
                }
            });

            if (t - sleep > 1000) { // нельзя брать сигнал с барьера чаще чем 1 раз в 1 секунду
                sleep = t; // так же это сделано чтобы избежать "дребезга кнопок" используя кнопки без подтяжки, всё это делается программно
                mode = !mode; // инверсия старт/стоп
            }
        } else {
            // Делаем красным
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setBulletRed(bulletDrawableSensor);
                }
            });
        }
    }

    private void setBulletGreen(Drawable objDrawable) {
        // bulletDrawableSensor
        // bulletDrawableTimer
        objDrawable = objDrawable.mutate(); // Ensure that the drawable is mutable
        objDrawable.setTint(bulletColorGreen);
    }

    private void setBulletRed(Drawable objDrawable) {
        objDrawable = objDrawable.mutate(); // Ensure that the drawable is mutable
        objDrawable.setTint(bulletColorRed);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(Log, "Serial Connection Opened!\n");
                        } else {
                            tvAppend(Log, "SERIAL PORT NOT OPEN\n");
                        }
                    } else {
                        tvAppend(Log, "SERIAL PORT IS NULL\n");
                    }
                } else {
                    tvAppend(Log, "SERIAL PERM NOT GRANTED\n");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieve the bullet drawable
        bulletDrawableTimer = ContextCompat.getDrawable(this, R.drawable.bullet);
        bulletDrawableSensor = ContextCompat.getDrawable(this, R.drawable.bullet);
        // Set the desired color for the bullet drawable
        bulletColorRed = ContextCompat.getColor(this, android.R.color.holo_red_light);
        bulletColorGreen = ContextCompat.getColor(this, android.R.color.holo_green_light);

        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        startButton = findViewById(R.id.buttonStart);
        sendButton = findViewById(R.id.buttonSend);
        clearButton = findViewById(R.id.buttonClear);
        stopButton = findViewById(R.id.buttonStop);
        editText = findViewById(R.id.editText);
        Log = findViewById(R.id.Log);
        Sensor = findViewById(R.id.Sensor);
        Timer = findViewById(R.id.Timer);

        int bulletWidthTimer = bulletDrawableTimer.getIntrinsicWidth();
        int bulletWidthSensor = bulletDrawableSensor.getIntrinsicWidth();


        int leftPadding = getResources().getDimensionPixelSize(R.dimen.left_padding);
        int gapWidth = getResources().getDimensionPixelSize(R.dimen.right_padding);
        int totalWidthTimer = bulletWidthTimer + gapWidth;
        int totalWidthSensor = bulletWidthSensor + gapWidth;

        Timer.setCompoundDrawablesWithIntrinsicBounds(bulletDrawableTimer, null, bulletDrawableTimer, null);
        Timer.setPadding(leftPadding, 0, totalWidthTimer, 0);
        Sensor.setCompoundDrawablesWithIntrinsicBounds(bulletDrawableSensor, null, bulletDrawableSensor, null);
        Sensor.setPadding(leftPadding, 0, totalWidthSensor, 0);

        Log.setMovementMethod(new ScrollingMovementMethod());
        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        Log.setEnabled(bool);
    }

    public void onClickStart(View view) {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 6790)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }

    public void onClickSend(View view) {
        if (connection == null) return;
        String string = editText.getText().toString();
        byte[] data = string.getBytes();
        serialPort.write(data);
        tvAppend(Log, "Data Sent : " + string + "\n");
    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();
        tvAppend(Log, "Serial Connection Closed! \n");
    }

    public void onClickClear(View view) {
        Log.setText(" ");
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

    private void textShow(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.setText(ftext);
            }
        });
    }
}