package com.example.lcs5382.mobilesensordetect;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;

/** 2019.2.9 01:10 Made by CheonSol Lee
 *
 * 추가 기능 : Git 재연결
 *
 * */
public class MainActivity extends Activity {
    SensorManager sensorManager;
    SensorEventListener accListener;
    SensorEventListener oriListener;
    SensorEventListener gyroListener;
    SensorEventListener magnoListener;
    Sensor oriSensor;
    Sensor accSensor;
    Sensor gyroSensor;
    Sensor magnoSensor;
    TextView accValueX, accValueY, accValueZ;
    TextView oriValueX, oriValueY, oriValueZ;
    TextView gyroValueX, gyroValueY, gyroValueZ;
    TextView magnoValueX, magnoValueY, magnoValueZ;
    Button btnStart, btnStop, btnSave;
    TextView tvTimer;

    private static int count = 0; // 시간단위 인덱스

    private SensorData sensorData = new SensorData();

    private final static int BTN_START = 1;
    private final static int BTN_STOP  = 2;
    private final static int BTN_SAVE  = 3;

    private boolean token = true;  // false:Stop , true:Run

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);          // SensorManager 인스턴스를 가져옴

        accSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);  // 가속도 센서
        oriSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);    // 방향   센서
        gyroSensor  = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);      // 자이로 센서
        magnoSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD); // 마그노 센서

        accListener   = new accListener();      // 가속도 리스너 인스턴스
        oriListener   = new oriListener();      // 방향   리스너 인스턴스
        gyroListener  = new gyroListener();     // 자이로 리스너 인스턴스
        magnoListener = new magnoListener();    // 마그노 리스너 인스턴스

        accValueX = (TextView)findViewById(R.id.acc_x);
        accValueY = (TextView)findViewById(R.id.acc_y);
        accValueZ = (TextView)findViewById(R.id.acc_z);
        oriValueX = (TextView)findViewById(R.id.ori_x);
        oriValueY = (TextView)findViewById(R.id.ori_y);
        oriValueZ = (TextView)findViewById(R.id.ori_z);
        gyroValueX = (TextView)findViewById(R.id.gyro_x);
        gyroValueY = (TextView)findViewById(R.id.gyro_y);
        gyroValueZ = (TextView)findViewById(R.id.gyro_z);
        magnoValueX = (TextView) findViewById(R.id.magno_x);
        magnoValueY = (TextView) findViewById(R.id.magno_y);
        magnoValueZ = (TextView) findViewById(R.id.magno_z);

        tvTimer = (TextView) findViewById(R.id.tv_timer);

        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop  = (Button) findViewById(R.id.btn_stop);
//        btnSave  = (Button) findViewById(R.id.btn_save);

        btnStart.setOnClickListener(onclickListener);
        btnStop.setOnClickListener(onclickListener);
//        btnSave.setOnClickListener(onclickListener);

        btnStart.setOnTouchListener(onTouchListener);
        btnStop.setOnTouchListener(onTouchListener);
    }

    /* 버튼터치 리스너 등록*/
    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                switch (returnButtonType(v)){
                    // RED : RUN, GRAY : STOP
                    case BTN_START:{
                        Toast.makeText(MainActivity.this,"Running",Toast.LENGTH_SHORT).show();
                        btnStart.setBackgroundColor(Color.RED);
                        btnStop.setBackgroundColor(Color.GRAY);
                        break;
                    }

                    case BTN_STOP:{
                        Toast.makeText(MainActivity.this,"Stopped",Toast.LENGTH_SHORT).show();
                        btnStart.setBackgroundColor(Color.GRAY);
                        btnStop.setBackgroundColor(Color.RED);
                        break;
                    }
                }
            }

            return false;
        }
    };

    /* 버튼클릭 리스너 등록*/
    private View.OnClickListener onclickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (returnButtonType(v)){
                case BTN_START:{
                    try {
                        startSensorDetection();
                    } catch (IOException e) {
                        Log.i("BTN_START error:", "IOException");
                        e.printStackTrace();
                    }
                    break;
                }

                case BTN_STOP:{
                    stopSensorDetection();
                    break;
                }

//                case BTN_SAVE:{
//                    break;
//                }
            }
        }
    };

    private void printTimer(){
        tvTimer.setText(String.valueOf(count));
    }

    /* 특정 시간에 1번씩 TimerTask가 실행*/
    private void timerSetting(){
        final Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                //BTN_STOP누를시 timer가 해지
                if(token == false){
                    timer.cancel();
                }
                else{
                    count++;
                    sensorData.setCount(count);
                    Log.i("timer_count", String.valueOf(count));
                    String data = convertSensorValueType(sensorData);

//                    printTimer();

                    try {
                        makeFile(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        timer.schedule(timerTask, 500, 1000);
    }

    /* 버튼타입에 따라 정수형으로 리턴*/
    private int returnButtonType(View v) {
        if     (v == btnStart) return BTN_START;
        else if(v == btnStop)  return BTN_STOP;
        else                   return BTN_SAVE;
    }

    /* 센서탐지 시작 후 CSV파일로 저장*/
    private void startSensorDetection() throws IOException {
        token = true;
        count = 0;
        sensorManager.registerListener(accListener  , accSensor,   SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(oriListener  , oriSensor,   SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(gyroListener , gyroSensor,  SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(magnoListener, magnoSensor, SensorManager.SENSOR_DELAY_NORMAL);
        Log.i("start_count", String.valueOf(count));

        //token이 true: run, false: stop
        if(token){
            // csv파일로 전송
            timerSetting();
        }
    }

    /* 센서 값을 CSV형식으로 변경*/
    private String convertSensorValueType(SensorData sensorData) {
        String data;

        data  = String.valueOf(sensorData.getCount()) + ",";
        data += "" + sensorData.getAccStringList()  + ",";
        data += "" + sensorData.getOriStringList()  + ",";
        data += "" + sensorData.getGyroStringList() + ",";
        data += "" + sensorData.getMagnoStringList();
        Log.i("in_convert_func", data);
        return data;
    }

    /* 센서탐지 정지*/
    private void stopSensorDetection(){
        token = false;
        sensorManager.unregisterListener(accListener);
        sensorManager.unregisterListener(oriListener);
        sensorManager.unregisterListener(gyroListener);
        sensorManager.unregisterListener(magnoListener);
        Log.i("BTN_STOP_IN", String.valueOf(count));
    }


    /* 가속도 센서 값이 바뀔때마다 호출됨 */
    private class accListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {
            double xValue = event.values[0];
            double yValue = event.values[1];
            double zValue = event.values[2];

            accValueX.setText(Double.toString(xValue));
            accValueY.setText(Double.toString(yValue));
            accValueZ.setText(Double.toString(zValue));

            sensorData.setAccValueList(xValue, yValue, zValue);

            // 타이머 추가
            printTimer();

            Log.i("SENSOR", "Acceleration changed.");
            Log.i("SENSOR", "Acceleration X: " + event.values[0]
                    + ", Acceleration Y: " + event.values[1]
                    + ", Acceleration Z: " + event.values[2]);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    /* 방향 센서 값이 바뀔때마다 호출됨 */
    private class oriListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {
            double xValue = event.values[0];
            double yValue = event.values[1];
            double zValue = event.values[2];

            oriValueX.setText(Double.toString(xValue));
            oriValueY.setText(Double.toString(yValue));
            oriValueZ.setText(Double.toString(zValue));

            sensorData.setOriValueList(xValue, yValue, zValue);

            Log.i("SENSOR", "Orientation changed.");
            Log.i("SENSOR", "Orientation X: " + event.values[0]
                    + ", Orientation Y: " + event.values[1]
                    + ", Orientation Z: " + event.values[2]);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    /* 자이로 센서 값이 바뀔때마다 호출됨 */
    private class gyroListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {
            double xValue = event.values[0];
            double yValue = event.values[1];
            double zValue = event.values[2];

            gyroValueX.setText(Double.toString(xValue));
            gyroValueY.setText(Double.toString(yValue));
            gyroValueZ.setText(Double.toString(zValue));

            sensorData.setGyroValueList(xValue, yValue, zValue);

            Log.i("SENSOR", "Gyroscope changed.");
            Log.i("SENSOR", "Gyroscope X: " + event.values[0]
                    + ", Gyroscope Y: " + event.values[1]
                    + ", Gyroscope Z: " + event.values[2]);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    /* 마그노 센서 값이 바뀔때마다 호출됨 */
    private class magnoListener implements SensorEventListener {
        public void onSensorChanged(SensorEvent event) {
            double xValue = event.values[0];
            double yValue = event.values[1];
            double zValue = event.values[2];

            magnoValueX.setText(Double.toString(xValue));
            magnoValueY.setText(Double.toString(yValue));
            magnoValueZ.setText(Double.toString(zValue));

            sensorData.setMagnoValueList(xValue, yValue, zValue);

            Log.i("SENSOR", "Magnetic sensor changed.");
            Log.i("SENSOR", "Magnetic sensor X: " + event.values[0]
                    + ", Magnetic sensor Y: " + event.values[1]
                    + ", Magnetic sensor Z: " + event.values[2]);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    /* 파일 입출력 : csv파일로 저장 */
    private void makeFile(String data) throws IOException{
        String fileName = getExternalFilesDir(null)+"/test.csv";
        Log.i("make_file", fileName);
        Log.i("make_file_data", data);

        PrintWriter printWriter = new PrintWriter(new FileWriter(fileName, true));
        printWriter.println(data);
        printWriter.close();
    }


}

