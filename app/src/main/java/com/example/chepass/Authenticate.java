package com.example.chepass;

import android.content.Context;
import android.widget.Toast;

import com.google.gson.Gson;
import com.retrofit.lite.services.APITask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

class ApiResponse {
    public Boolean register;
    public String address;
    public String pass;
}

public class Authenticate implements APITask.Listener {
    private final Context context;
    private Callback callback;
    private final String code;
    private Boolean isCodeOnDisk;

    public Authenticate(Context ctx, String c, Callback clbk){
        context = ctx;
        code = c;
        callback = clbk;
    }

    public void Auth(){
       isCodeOnDisk = CheckIfIsInDisk();
       APITask.from(context).sendGET(101, "https://example.com/codes/" + code + ".json", null, this);
    }

    public boolean CheckIfIsInDisk(){
        File file = new File(context.getFilesDir(),code + ".json");
        return file.exists();
    }

    public void SaveOnDisk(){
        try {
            FileOutputStream fos = context.openFileOutput(code+".json",Context.MODE_PRIVATE);
            Writer out = new OutputStreamWriter(fos);
            out.write("ok");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSuccess(int pid, int status, Map<String, String> headers, String body) {
        Gson gson = new Gson();
        ApiResponse gsonObj = gson.fromJson(body, ApiResponse.class);
        if(gsonObj.address == null || gsonObj.register == null || gsonObj.pass == null){
            Toast.makeText(context, "Invalid Response",
                    Toast.LENGTH_LONG).show();
            return;
        }
        //if(!isCodeOnDisk && !gsonObj.register){
        if(!gsonObj.register){
            Toast.makeText(context, "This code was used! please obtain a new one",
                    Toast.LENGTH_LONG).show();
            return;
        }
        /*if(!isCodeOnDisk){
            SaveOnDisk();
        }*/
        callback.onSuccess(gsonObj.address, gsonObj.pass);
    }

    @Override
    public void onFailed(int pid, Exception ex) {
        Toast.makeText(context, "Server side error please contact with support.",
                Toast.LENGTH_LONG).show();
    }
}
