package com.example.chepass;
import tun2socks.Tun2socks;

public class BrookConnection extends Thread{
    public String address;
    public String pass;
    public void kill(){
        try{
            Tun2socks.stopClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        try{
            Tun2socks.startClient("127.0.0.1:8080", address, pass);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
