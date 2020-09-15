package com.example.test;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class MainActivity extends Activity {
    GraphView heartRateGraphView;
    LineGraphSeries<DataPoint> heartRateGraphSeries;
    ArrayList<DataPoint> heartRateData;
    TextView currentHeartRateTextView;
    TextView currentTemperatureTextView;
    ImageView temperatureBackgroundCoverSheet;
    ImageView heartImageView;
    ImageView maskStatusImageView;

    AnimationDrawable heartBeatAnimation;

    private final Handler threadHandler = new Handler();

    int readingsToShowPerFrame = 120;
    int lastHeartRateReading = 0;

    float lastTemperatureReading = 0; // In Celsius
    float temperatureLowerLimit = 30; // In Celsius
    float temperatureUpperLimit = 60; // In Celsius

    int graphUpdateTimeInterval = 1000; // In milliseconds
    int dataUpdateTimeInterval = 500; // In milliseconds
    float temperatureBackgroundCoverSheetCurrentScale = 1f;

    Runnable graphUpdaterThread;
    Runnable dataUpdaterThread;
    Runnable heartBeatThread;

    int[] heartBeatIntervals = new int[]{300, 125, 200, 125, 300};
    float[] heartBeatScales = new float[]{1f, 1.4f, 1.2f, 1.4f, 1f};
    int heartBeatIntervalPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        heartRateData = new ArrayList<>();
        heartRateData.add(new DataPoint(0, 0));

        heartImageView = findViewById(R.id.heart);

        maskStatusImageView = findViewById(R.id.mask_status_image);

        currentHeartRateTextView = findViewById(R.id.heart_rate_result_text_view);
        currentHeartRateTextView.setText(getString(R.string.heart_rate, lastHeartRateReading));

        currentTemperatureTextView = findViewById(R.id.temperature_result_text_view);
        currentTemperatureTextView.setText(getString(R.string.temperature, lastTemperatureReading));

        // Setting up the temperature display
        temperatureBackgroundCoverSheet = findViewById(R.id.temperature_graph_white_cover_sheet);
        temperatureBackgroundCoverSheet.setPivotX(temperatureBackgroundCoverSheet.getMeasuredWidth());


        // Setting up the graph view and its series for heart rate.
        heartRateGraphView = findViewById(R.id.heart_rate_graph);
        heartRateGraphSeries = new LineGraphSeries<>();
        heartRateGraphView.addSeries(heartRateGraphSeries);
        heartRateGraphSeries.resetData(getHeartRateData());


        // Customizing the heart rate graph

        heartRateGraphView.getViewport().setXAxisBoundsManual(true);
        heartRateGraphView.getViewport().setYAxisBoundsManual(true);

        heartRateGraphView.getViewport().setMinX(0);
        heartRateGraphView.getViewport().setMaxX(readingsToShowPerFrame);

        heartRateGraphView.getViewport().setMinY(40);
        heartRateGraphView.getViewport().setMaxY(100);

        heartRateGraphView.setTitle(getString(R.string.heart_rate_title));
        heartRateGraphView.setTitleColor(Color.BLACK);
        heartRateGraphView.setTitleTextSize(80);

        heartRateGraphView.getLegendRenderer().setVisible(false);

        heartRateGraphView.setBackgroundColor(Color.WHITE);
        heartRateGraphView.getViewport().setBackgroundColor(Color.parseColor("#FFFFFF"));

        heartRateGraphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        heartRateGraphView.getGridLabelRenderer().setVerticalLabelsAlign(Paint.Align.CENTER);

        heartRateGraphView.getGridLabelRenderer().setGridColor(Color.parseColor("#FFFFFF"));

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(7);
        heartRateGraphSeries.setCustomPaint(paint);

        heartRateGraphSeries.setBackgroundColor(Color.BLACK);
        heartRateGraphSeries.setDrawDataPoints(false);
        heartRateGraphSeries.setThickness(7);

        onResume();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialise all threads

        // Thread to update graphs
        graphUpdaterThread = new Runnable() {
            @Override
            public void run() {

                // Update the temperature speedometer
                lastTemperatureReading = DataMonitor.getTemperature();
                changeTemperature(lastTemperatureReading);
                currentTemperatureTextView.setText(getString(R.string.temperature, lastTemperatureReading));

                // Remove first reading and the add one at the end.
                if(heartRateData.size() >= readingsToShowPerFrame){
                    heartRateData.remove(0);
                    for(int i = 0; i < heartRateData.size(); i++){
                        heartRateData.set(i, new DataPoint(heartRateData.get(i).getX() - 1, heartRateData.get(i).getY()));
                    }
                }

                // Update the heart Rate graph
                lastHeartRateReading = DataMonitor.getHeartRate();
                heartRateData.add(new DataPoint(heartRateData.size() - 1, lastHeartRateReading));
                heartRateGraphSeries.resetData(getHeartRateData());
                currentHeartRateTextView.setText(getString(R.string.heart_rate, lastHeartRateReading));

                //Log.d("Graph", "Updated the graphs, bitch. No. Of Readings = " + heartRateData.size());

                if(heartBeatAnimation != null)
                    heartBeatThread.run();

                int lol = (int)(Math.random() * 2) + 1;
                if(lol == 1){
                    maskStatusImageView.setImageResource(R.drawable.face_silheoutte_masked);
                }else{
                    maskStatusImageView.setImageResource(R.drawable.face_silheoutte);
                }

                threadHandler.postDelayed(graphUpdaterThread, graphUpdateTimeInterval);
            }
        };

        // Thread to update data
        dataUpdaterThread = new Runnable() {
            @Override
            public void run() {
                DataMonitor.setHeartRate(generateHeartRateData());
                DataMonitor.setTemperature(generateTemperatureData());
                threadHandler.postDelayed(dataUpdaterThread, dataUpdateTimeInterval);
                /*Log.d("Data", "Heart Rate: " + DataMonitor.getHeartRate());
                Log.d("Data", "Temperature: " + DataMonitor.getTemperature());*/

            }
        };

        heartBeatThread = new Runnable() {
            @Override
            public void run() {
                if(heartBeatIntervalPosition >= heartBeatIntervals.length - 1){
                    heartBeatIntervalPosition = 0;
                }else{
                    heartBeatIntervalPosition += 1;
                }

                heartImageView.animate().scaleX(heartBeatScales[heartBeatIntervalPosition]).scaleY(heartBeatScales[heartBeatIntervalPosition]);

                //Log.d("Heart", "Scale = " + lastScale);

                threadHandler.postDelayed(heartBeatThread, heartBeatIntervals[heartBeatIntervalPosition]);
            }
        };

        threadHandler.postDelayed(graphUpdaterThread, graphUpdateTimeInterval);
        threadHandler.postDelayed(dataUpdaterThread, dataUpdateTimeInterval);
        threadHandler.postDelayed(heartBeatThread, heartBeatIntervals[heartBeatIntervalPosition]);
    }

    @Override
    protected void onPause() {
        super.onPause();
        threadHandler.removeCallbacks(graphUpdaterThread);
        threadHandler.removeCallbacks(dataUpdaterThread);
        threadHandler.removeCallbacks(heartBeatThread);
    }

    private DataPoint[] getHeartRateData(){
        DataPoint[] data = new DataPoint[heartRateData.size()];

        for(int i = 0; i < heartRateData.size(); i++){
            data[i] = heartRateData.get(i);
        }

        return data;
    }

    public void changeTemperature(float temperature){
        float scale = (100 / (temperatureUpperLimit - temperatureLowerLimit) * (temperature - temperatureLowerLimit)) /100;


        /*ScaleAnimation animation = new ScaleAnimation(
                temperatureBackgroundCoverSheetCurrentScale, scale,
                1f, 1f,
                Animation.RELATIVE_TO_SELF, 1f,
                Animation.RELATIVE_TO_SELF, 0f);

        animation.setDuration(2000);
        animation.setFillAfter(true);

        temperatureBackgroundCoverSheet.startAnimation(animation);*/

        temperatureBackgroundCoverSheet.animate().scaleX(scale).setDuration(graphUpdateTimeInterval / 2);

        temperatureBackgroundCoverSheetCurrentScale = scale;
        Log.d("White Sheet", "Scale = " + scale);
    }



    // This is to be received by receive heart rate data
    public int generateHeartRateData(){
        return (int) (Math.random() * ((70 - 50) + 1) + 50);
    }

    // This is to be received by receive temperature data
    public float generateTemperatureData(){
        return (float)(Math.random() * (temperatureUpperLimit - temperatureLowerLimit + 1) + temperatureLowerLimit);
    }
}