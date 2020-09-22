package com.example.share;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

public class WifiDirectBrodcastReciver extends BroadcastReceiver {
    private WifiP2pManager p2pManager;
    private  WifiP2pManager.Channel channel;
    private  MainActivity mainActivity;

    public WifiDirectBrodcastReciver(WifiP2pManager p2pManager, WifiP2pManager.Channel channel, MainActivity mainActivity) {
        this.p2pManager = p2pManager;
        this.channel = channel;
        this.mainActivity = mainActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action=intent.getAction();
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
            int state=intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if(state==WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                Toast.makeText(context,"wifi is on",Toast.LENGTH_LONG).show();
            }else {
                Toast.makeText(context,"wifi is off",Toast.LENGTH_LONG).show();
            }

        }else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            if(p2pManager!=null){
                p2pManager.requestPeers(channel,mainActivity.peerListListener);
            }

        }else  if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            if(p2pManager==null){
                return;
            }

                NetworkInfo networkInfo=intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if(networkInfo.isConnected()){
                    p2pManager.requestConnectionInfo(channel,mainActivity.connectionInfoListener);
                }
                else {
                    mainActivity.conectingStatus.setText("DeviceDisconnected");
                }


        }else  if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){

        }
    }
}
