package com.ibm.chris.httpcachetest;

import android.net.http.HttpResponseCache;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            File httpCacheDir = new File(this.getCacheDir(), "http9");
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.i("CACHE", "HTTP response cache installation failed:" + e);
        }


        for(int i=0; i<2; i++) {
            Log.i("i", i + "");
            Thread thread = new Thread(new Runnable(){
                @Override
                public void run(){
                    try{
                        Log.i("x", downloadUrl("http://mqtt.chris.gunawardena.id.au/wp-content/uploads/cache_test.php"));
                        Log.i("x", downloadUrl("http://mobileapi.jumbo.com/v2/autocomplete"));
                    } catch (Exception e) {
                        Log.e("x", "IOException", e);
                    }
                }
            });
            thread.start();
        }
    }
    private String readStream(InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int i = is.read();
            while(i != -1) {
                bo.write(i);
                i = is.read();
            }
            return bo.toString();
        } catch (IOException e) {
            return "";
        }
    }


    private String downloadUrl(String uri) throws IOException {
        URL url = new URL(uri);
        InputStream stream = null;
        HttpURLConnection connection = null;
        String result = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(true);
            connection.setDefaultUseCaches(true);
            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.setReadTimeout(30000);
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.setConnectTimeout(30000);
            // For this use case, set HTTP method to GET.
            connection.setRequestMethod("GET");
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);
            // Open communications link (network traffic occurs here).
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();
            if (stream != null) {
                // Converts Stream to String with max length of 500.
                result = readStream(stream);
            }
        } finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                stream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    String getUrl(String uri) {
        BufferedReader rd  = null;
        StringBuilder sb = null;
        String line = null;
        URL url = null;
        HttpURLConnection connection = null;

        try {
            url = new URL(uri);
            connection = (HttpURLConnection)url.openConnection();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                return null;

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while((bytesRead = in.read(buffer)) > 0){
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toString();
        }
        catch(Exception e) {
            Log.e("eeee", "exception");
        }
        finally {
            if(connection!=null)
                connection.disconnect();
        }
        return "";
    }

    @Override
    protected void onStop() {
       super.onStop();
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }
    }

}
