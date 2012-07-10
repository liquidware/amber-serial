package com.liquidware.networkedserial.app;

public interface Event {
    public void onTimerTick(long millisUpTime);
    public void onSocketDataReceived(String msg);
    public void onSocketDataTransmit(String msg);
}
