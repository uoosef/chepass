package com.example.chepass;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;

public class sshConnection extends Thread{
    private Context context;
    private String basePath;
    private Process proc;

    public sshConnection(Context c){
        this.context = c;
        this.basePath = this.context.getFilesDir().getPath() + "/";
    }

    public void kill(){
        try {
            Runtime.getRuntime().exec(this.basePath + "busybox killall -9 sh");
            Runtime.getRuntime().exec(this.basePath + "busybox killall -9 ssh");
            Runtime.getRuntime().exec(this.basePath + "busybox killall -9 busybox");
        } catch (IOException e) {
            e.printStackTrace();
        }
        proc.destroy();
    }

    @Override
    public void run(){
        try {
            copyAssets();
            createRunShFile(
                    this.basePath, "123",
                    "monthlyssh.com-uooci", "uk.ssh.monthlyssh.com",
                    "443"
            );
            String runableShellScript = this.basePath + "run.sh";
            String myExec = this.basePath + "busybox sh " + runableShellScript;
            proc = Runtime.getRuntime().exec(myExec);
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            // Read the output from the command
            Log.e("ERROR", "Here is the standard output of the command:\n");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                Log.e("ERROR", s);
            }

            // Read any errors from the attempted command
            Log.e("ERROR", "Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                Log.e("ERROR", s);
            }
        }
        catch (IOException e) {
            Log.e("YEK ERROR TOKHMI!: ", "coudnt start ssh!");
        }
    }

    private void copyAssets() {
        AssetManager assetManager = this.context.getAssets();
        String[] files = {
                "ssh",
                "sshpass",
                "busybox",
                "tun2socks",
        };
        String cpuAbi = Build.CPU_ABI.toLowerCase();
        if(cpuAbi.contains("arm"))
            cpuAbi = "arm/";
        else if(cpuAbi.contains("x86") || cpuAbi.contains("x64"))
            cpuAbi = "x86/";
        else{
            Log.e("UNKNOWN ARCHITECTURE: ", "we dont support this cpu architecture");
        }
        try {
            for(String filename : files) {
                InputStream in = null;
                OutputStream out = null;
                in = assetManager.open("chepass/" + cpuAbi + filename);

                File outFile = new File(this.basePath, filename);

                out = new FileOutputStream(outFile);
                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
                Runtime.getRuntime().exec("chmod +x " + this.basePath + "/" + filename);
            }
        } catch(IOException e) {
            Log.e("tag", "Failed to copy all asset files");
        }
    }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }
    private void createRunShFile(String path, String password,
                                 String userName, String host
                                ,String port){
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(path + "run.sh"), "utf-8"));
            writer.write("HOME="+path+"\n");
            writer.write("cd ~\n");
            writer.write("while :\n");
            writer.write("do\n");
            writer.write("\t./sshpass -p " + password + " ./ssh -oStrictHostKeyChecking=no "
                            + userName + "@" + host + " -NTD 8080 -g -p " + port);
            writer.write("\ndone\n");
        } catch (IOException ex) {
            // Report
        } finally {
            try {writer.close();} catch (Exception ex) {/*ignore*/}
        }
    }
}
