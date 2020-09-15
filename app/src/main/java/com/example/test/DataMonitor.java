package com.example.test;

public class DataMonitor {
    public static int[] heartRateBuffer = new int[]{0, 0, 0, 0};
    public static float[] temperatureBuffer = new float[]{0f, 0f, 0f, 0f};

    public static float getTemperature() {
        return temperatureBuffer[0];
    }

    public static void setTemperature(float temperature) {
        for(int i = 1; i < DataMonitor.temperatureBuffer.length; i++) DataMonitor.temperatureBuffer[i - 1] = DataMonitor.temperatureBuffer[i];
        DataMonitor.temperatureBuffer[3] = temperature;
    }

    public static int getHeartRate() {
        return heartRateBuffer[0];
    }

    public static void setHeartRate(int heartRate) {
        for(int i = 1; i < DataMonitor.heartRateBuffer.length; i++) DataMonitor.heartRateBuffer[i - 1] = DataMonitor.heartRateBuffer[i];
        DataMonitor.heartRateBuffer[3] = heartRate;
    }
}
