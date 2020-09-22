package com.example.share;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.InetAddresses;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Button onoff, descover, send;
    ListView listView;
    TextView readmsg, conectingStatus;
    EditText writemsg;
    WifiManager wifiManager;
    WifiP2pManager wifiP2pManager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver broadcastReceiver;
    IntentFilter intentFilter;
    List<WifiP2pDevice> peerList = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] devicesArray;
    static final int MESSAGE_READ = 1;
    ServerClass serverClass;
    ClientClass clientClass;
    SendRecive sendRecive;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initialWork();
        exqListener();
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    readmsg.setText(tempMsg);
                    break;


            }
            return true;
        }
    });

    private void exqListener() {
        onoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(false);
                    onoff.setText("ON");
                } else {
                    wifiManager.setWifiEnabled(true);
                    onoff.setText("OFF");
                }
            }
        });
        descover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        conectingStatus.setText("Discover Started");
                    }

                    @Override
                    public void onFailure(int reason) {
                        conectingStatus.setText("Discover Starting fail");
                    }
                });
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice device = devicesArray[position];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;

                wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "Connected to" + device.deviceName, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(getApplicationContext(), "Not connected", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg=writemsg.getText().toString();
                sendRecive.write(msg.getBytes());
            }
        });
    }

    private void initialWork() {
        onoff=findViewById(R.id.onoff);
        descover=findViewById(R.id.Descover);
        send=findViewById(R.id.send);
        listView=findViewById(R.id.listview);
        readmsg=findViewById(R.id.readmsg);
        conectingStatus=findViewById(R.id.connecting);
        writemsg=findViewById(R.id.writemsg);
        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
wifiP2pManager= (WifiP2pManager) getApplicationContext().getSystemService(WIFI_P2P_SERVICE);
channel=wifiP2pManager.initialize(this,getMainLooper(),null);
broadcastReceiver=new WifiDirectBrodcastReciver(wifiP2pManager,channel,this);
intentFilter=new IntentFilter();
intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }
    WifiP2pManager.PeerListListener peerListListener=new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            if(!peers.getDeviceList().equals(peerList)){
                peerList.clear();
                peerList.addAll(peers.getDeviceList());
                deviceNameArray=new String[peers.getDeviceList().size()];
                devicesArray=new WifiP2pDevice[peers.getDeviceList().size()];
                int index=0;
                for(WifiP2pDevice device:peers.getDeviceList()){
                    deviceNameArray[index]=device.deviceName;
                    devicesArray[index]=device;
                    index++;
                }
                ArrayAdapter<String>arrayAdapter=new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listView.setAdapter(arrayAdapter);
            }
            if(peerList.size()==0){
                Toast.makeText(getApplicationContext(),"No Device Found",Toast.LENGTH_LONG).show();
            }
        }
    };
WifiP2pManager.ConnectionInfoListener connectionInfoListener=new WifiP2pManager.ConnectionInfoListener() {
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        final InetAddress groupOwenerAddress=info.groupOwnerAddress;

        if(info.groupFormed && info.isGroupOwner){
            conectingStatus.setText("Hpast");
            serverClass=new ServerClass();
            serverClass.start();
        }
        else if(info.groupFormed){
            conectingStatus.setText("client");
            clientClass=new ClientClass(groupOwenerAddress);
            clientClass.start();
        }

    }
};
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver,intentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }
    public  class  ServerClass extends  Thread{
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            super.run();
            try {
                serverSocket=new ServerSocket(8888);
                socket=serverSocket.accept();
                sendRecive=new SendRecive(socket);
                sendRecive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private  class  SendRecive extends Thread{
        private  Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;
        public  SendRecive(Socket skt){
            socket=skt;
            try {
                inputStream=socket.getInputStream();
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();
            byte[]buffer=new byte[1024];
            int bytes;
            while (socket!=null){
                try {
                    bytes=inputStream.read(buffer);
                    if(bytes>0){
                        handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public  void  write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public  class  ClientClass extends Thread{
        Socket socket;
        String hostAddress;
        public  ClientClass(InetAddress inetAddresses){
            hostAddress=inetAddresses.getHostAddress();
            socket=new Socket();
        }

        @Override
        public void run() {
            super.run();
            try {
                socket.connect(new InetSocketAddress(hostAddress,8888),500);
                sendRecive=new SendRecive(socket);
                sendRecive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}