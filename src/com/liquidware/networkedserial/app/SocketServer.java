
package com.liquidware.networkedserial.app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.AsyncTask;
import android.util.Log;

public class SocketServer extends AsyncTask<String, Integer, Boolean> implements Event {
    final String TAG = "SocketServer";
    ServerSocket serverSocket = null;
    Socket socket = null;
    DataInputStream dataInputStream = null;
    DataOutputStream dataOutputStream = null;
    EventNotifier mNotifier;

    public SocketServer(EventNotifier notifier, int port) {
        //Listen for events
        mNotifier = notifier;
        mNotifier.addListener(this);
        try {
            serverSocket = new ServerSocket(port);
            Log.d(TAG, "Listening :" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Boolean doInBackground(String... msg) {
        while (true) {
            try {
                socket = serverSocket.accept();
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                Log.d(TAG, "ip: " + socket.getInetAddress());
                //Log.d(TAG, "message: " + dataInputStream.readUTF());
                dataOutputStream.writeUTF("Hello!");
                while (true) {
                    byte[] buff = { 0 };
                    try {
                        int count = dataInputStream.read(buff);
                        if (count > 0) {
                            String m = new String(buff);
                            mNotifier.onSocketDataReceived(m);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        break;
                    }
                }
                dataInputStream = null;
                dataOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void onSocketDataTransmit(String msg) {
        //Send the message to the socket
        try {
            if (dataOutputStream != null)
                dataOutputStream.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onTimerTick(long millisUpTime) {
    }

    public void onSocketDataReceived(String msg) {
    }
}
