package com.liquidware.networkedserial.app;

import com.liquidware.amberserial.app.R;

public interface Event {
    public void onTimerTick(long millisUpTime);
    public void onSocketDataReceived(String msg);
    public void onSocketDataTransmit(String msg);
}
