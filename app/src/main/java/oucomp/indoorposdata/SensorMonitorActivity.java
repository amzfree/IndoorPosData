package oucomp.indoorposdata;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SensorMonitorActivity extends Activity implements SensorEventListener,
        OnClickListener {
    private SensorManager sensorManager;
    private Button btnStart, btnStop, btnSave;
    private Spinner typeSpinner;
    private boolean collectData = false;
    private AccelDataSet sensorData;
    private LinearLayout layout;
    private View mChart;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_monitor);
        layout = (LinearLayout) findViewById(R.id.chart_container);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnSave = (Button) findViewById(R.id.btnUpload);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        if (sensorData == null || sensorData.size() == 0) {
            btnSave.setEnabled(false);
        }

        typeSpinner = (Spinner) findViewById(R.id.type_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (collectData == true) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (collectData) {
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];
            long timestamp = System.currentTimeMillis();
            AccelData data = new AccelData(timestamp, x, y, z);
            sensorData.add(data);
        }
    }

    private ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
    private Timer theTimer = null;
    private int timerCount = 0;
    private Toast saveDataToast = null;

    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    timerCount++;
                    if (timerCount >= 10) {
                        toneG.startTone(ToneGenerator.TONE_PROP_ACK, 1000);

                        stopMonitor();
                    } else if (timerCount == 5) {
                        toneG.startTone(ToneGenerator.TONE_PROP_ACK, 300);
                        collectData = true;
                    } else {
                        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                    }
                }});
        }
    };

    private void startMonitor() {
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        btnSave.setEnabled(false);
        sensorData = new AccelDataSet();
        sensorData.setType("" + typeSpinner.getSelectedItem());
        sensorData.setName(typeSpinner.getSelectedItem() + "-" + filenameFormatter.format(new Date()));
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        timerCount = 0;
        theTimer = new Timer();
        theTimer.schedule(timerTask, 1000, 1000);
    }

    private void stopMonitor() {
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnSave.setEnabled(true);
        theTimer.cancel();
        theTimer.purge();
        collectData = false;
        sensorManager.unregisterListener(this);
        layout.removeAllViews();
        openChart();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStart:
                startMonitor();
                break;
            case R.id.btnStop:
                stopMonitor();
                break;
            case R.id.btnUpload:
                saveData();
                break;
            default:
                break;
        }

    }

    private DateFormat filenameFormatter = new SimpleDateFormat("yyyyMMdd-HHmm");

    private void saveData() {
        File sdCard = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (sdCard.getAbsolutePath() + "/indoorPosData");
        dir.mkdirs();

        String filename = sensorData.getName();
        File file = new File(dir, filename);
        try {
            OutputStream ostream = new FileOutputStream(file);
            sensorData.writeJsonStream(ostream);
            if (saveDataToast == null) {
                saveDataToast = Toast.makeText(getApplicationContext(), "Data Saved to sdcard0/indoorPosData/", Toast.LENGTH_LONG);
            }
            saveDataToast.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openChart() {
        if (sensorData != null || sensorData.size() > 0) {
            long t = sensorData.getDatalist().get(0).getTimestamp();
            XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

            XYSeries xSeries = new XYSeries("X");
            XYSeries ySeries = new XYSeries("Y");
            XYSeries zSeries = new XYSeries("Z");
            XYSeries rSeries = new XYSeries("R");

            for (AccelData data : sensorData.getDatalist()) {
                xSeries.add(data.getTimestamp() - t, data.getX());
                ySeries.add(data.getTimestamp() - t, data.getY());
                zSeries.add(data.getTimestamp() - t, data.getZ());
                rSeries.add(data.getTimestamp() - t, Math.sqrt(data.getX()*data.getX()+data.getY()*data.getY()+data.getZ()*data.getZ()));
            }

            dataset.addSeries(xSeries);
            dataset.addSeries(ySeries);
            dataset.addSeries(zSeries);
            dataset.addSeries(rSeries);


            XYSeriesRenderer xRenderer = new XYSeriesRenderer();
            xRenderer.setColor(Color.RED);
            xRenderer.setPointStyle(PointStyle.CIRCLE);
            xRenderer.setFillPoints(true);
            xRenderer.setLineWidth(1);
            xRenderer.setDisplayChartValues(false);

            XYSeriesRenderer yRenderer = new XYSeriesRenderer();
            yRenderer.setColor(Color.GREEN);
            yRenderer.setPointStyle(PointStyle.CIRCLE);
            yRenderer.setFillPoints(true);
            yRenderer.setLineWidth(1);
            yRenderer.setDisplayChartValues(false);

            XYSeriesRenderer zRenderer = new XYSeriesRenderer();
            zRenderer.setColor(Color.BLUE);
            zRenderer.setPointStyle(PointStyle.CIRCLE);
            zRenderer.setFillPoints(true);
            zRenderer.setLineWidth(1);
            zRenderer.setDisplayChartValues(false);

            XYSeriesRenderer rRenderer = new XYSeriesRenderer();
            zRenderer.setColor(Color.BLACK);
            zRenderer.setPointStyle(PointStyle.CIRCLE);
            zRenderer.setFillPoints(true);
            zRenderer.setLineWidth(1);
            zRenderer.setDisplayChartValues(false);

            XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
            multiRenderer.setXLabels(0);
            multiRenderer.setLabelsColor(Color.RED);
            multiRenderer.setChartTitle("t vs (x,y,z)");
            multiRenderer.setXTitle("Sensor Data");
            multiRenderer.setYTitle("Values of Acceleration");
            multiRenderer.setZoomButtonsVisible(true);
            for (int i = 0; i < sensorData.size(); i++) {

                multiRenderer.addXTextLabel(i + 1, ""
                        + (sensorData.getDatalist().get(i).getTimestamp() - t));
            }
            for (int i = 0; i < 12; i++) {
                multiRenderer.addYTextLabel(i + 1, ""+i);
            }

            multiRenderer.addSeriesRenderer(xRenderer);
            multiRenderer.addSeriesRenderer(yRenderer);
            multiRenderer.addSeriesRenderer(zRenderer);
            multiRenderer.addSeriesRenderer(rRenderer);

            // Getting a reference to LinearLayout of the MainActivity Layout


            // Creating a Line Chart
            mChart = ChartFactory.getLineChartView(getBaseContext(), dataset,
                    multiRenderer);

            // Adding the Line Chart to the LinearLayout
            layout.addView(mChart);

        }
    }

}
