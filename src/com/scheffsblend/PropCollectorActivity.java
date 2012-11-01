package com.scheffsblend;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PropCollectorActivity extends Activity {

    private static final String UPLOAD_URL = "http://openbuildprop.scheffsblend.com/upload/";
    private static final String TAG = PropCollectorActivity.class.getName();

    private Button mUploadButton;
    private TextView mStatusText;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mUploadButton = (Button)findViewById(R.id.uploadButton);
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUploadButton.setEnabled(false);
                new UploadTask().execute();
            }
        });

        mStatusText = (TextView) findViewById(R.id.status);
    }


    /**
     * Simple AsyncTask to upload /system/build.prop to UPLOAD_URL
     */
    private class UploadTask extends AsyncTask<Void, Void, Boolean> {
        private Context mContext = null;
        @Override
        protected Boolean doInBackground(Void... params) {
            Log.i(TAG, "Uploading build.prop to " + UPLOAD_URL);

            HttpURLConnection connection = null;
            DataOutputStream outputStream = null;

            String pathToOurFile = "/system/build.prop";
            String urlServer = UPLOAD_URL;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";

            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;

            try {
                FileInputStream fileInputStream = new FileInputStream(new File(
                        pathToOurFile));

                Log.i(TAG, "Connecting to server to upload build.prop.");
                java.net.URL url = new URL(urlServer);
                connection = (HttpURLConnection) url.openConnection();

                // Allow Inputs & Outputs
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);

                // Enable POST method
                connection.setRequestMethod("POST");

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type",
                        "multipart/form-data;boundary=" + boundary);

                outputStream = new DataOutputStream(connection.getOutputStream());
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream
                        .writeBytes("Content-Disposition: form-data; name=\"file\";filename=\""
                                + pathToOurFile + "\"" + lineEnd);
                outputStream.writeBytes(lineEnd);

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // Read file
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    outputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens
                        + lineEnd);

                // Responses from the server (code and message)
                String serverResponseMessage = connection.getResponseMessage();
                int code = connection.getResponseCode();
                Log.i(TAG, "Done uploading build.prop with response: " + serverResponseMessage);

                fileInputStream.close();
                outputStream.flush();
                outputStream.close();

                // if server did not respond with 200 response code then something went wrong
                if (code != 200)
                    return false;
            } catch (Exception ex) {
                // Exception handling
                Log.e(TAG, ex.toString());
                return false;
            }

            // if we get here we will assume uploading of build.prop was successful
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (result.equals(Boolean.TRUE)) {
                mStatusText.setText(PropCollectorActivity.this.getString(R.string.status_upload_success));
                mStatusText.setTextColor(0xFF00FF00);
            } else {
                mStatusText.setText(PropCollectorActivity.this.getString(R.string.status_upload_failure));
                mStatusText.setTextColor(0xFFFF0000);
                mUploadButton.setEnabled(true);
            }
        }
    }
}
