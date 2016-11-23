package com.example.user.task_3;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;

    static final String IMAGE_ADDRESS = "https://i.ytimg.com/vi/gGBKCWMSw4o/maxresdefault.jpg";
    static final int REQUEST_CODE = 1;

    String[] PERMISSIONS = {"android.permission.READ_EXTERNAL_STORAGE","android.permission.WRITE_EXTERNAL_STORAGE"};



    DownloadManager downloadManager;
    ImageView pictureView;
    long id;
    TextView statusText;
    Button button;
    ProgressBar progressBar;
    CoordinatorLayout coordinatorLayout;
    ConnectivityManager connectivityManager;
    long downloadId;
    File loadedFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(PERMISSIONS, REQUEST_CODE);
        }

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        statusText = (TextView) findViewById(R.id.status_text);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        button = (Button) findViewById(R.id.button);


        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.l_coordinator);
        pictureView = (ImageView) findViewById(R.id.imageView);


        if (!loadImageFromStorage()) {

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            progressBar.setVisibility(View.INVISIBLE);

            downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {


                    if (!hasInternetConnection()) {
                        Snackbar.make(coordinatorLayout, R.string.connection_lost, Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    button.setEnabled(false);
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(IMAGE_ADDRESS))
                            .setTitle(getResources().getString(R.string.download_title))
                            .setDescription(getResources().getString(R.string.download_description))
                            .setDestinationInExternalFilesDir(getApplicationContext(), Environment.DIRECTORY_DOWNLOADS, "task-3-image");

                    progressBar.setVisibility(View.VISIBLE);
                    id = downloadManager.enqueue(request);
                    statusText.setText(R.string.status_in_progress);

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putLong("id_downloaded", id);
                    editor.commit();


                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            DownloadManager.Query query = new DownloadManager.Query();
                            query.setFilterById(id);
                            Cursor cursor = downloadManager.query(query);
                            cursor.moveToFirst();

                            int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            cursor.close();

                            final int progressInt = (bytesDownloaded * 100 / bytesTotal);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setProgress(progressInt);
                                }
                            });
                        }
                    }, 0, 10);

                }
            });

        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DownloadManager.Query query = new DownloadManager.Query();

            progressBar.setVisibility(View.INVISIBLE);
            Cursor cursor = downloadManager.query(query);

            if (cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    downloadId = sharedPreferences.getLong("id_downloaded", 0);

                    final ParcelFileDescriptor fileDescriptor;


                    try {
                        fileDescriptor = downloadManager.openDownloadedFile(downloadId);
                        FileInputStream fis = new ParcelFileDescriptor.AutoCloseInputStream(fileDescriptor);
                        Bitmap bitmap = BitmapFactory.decodeStream(fis);
                        pictureView.setImageBitmap(bitmap);
                        statusText.setText(R.string.status_done);

                        saveImageToInternalStorage(bitmap);

                        overrideButton();

                    } catch (FileNotFoundException e) {

                    }
                }
            }
        }
    };


    public boolean hasInternetConnection() {

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        try {
            return networkInfo.isConnectedOrConnecting();
        } catch (NullPointerException e) {
            return false;
        }
    }


    private void overrideButton() {
        button.setEnabled(true);
        button.setText(R.string.button_open);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openImgIntent = new Intent();
                openImgIntent.setType(Intent.ACTION_VIEW);
                openImgIntent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "file.jpg")), "image/*");
                startActivity(openImgIntent);
            }
        });
    }


    private boolean loadImageFromStorage() {

        try {
            loadedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "file.jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(loadedFile));

            pictureView.setImageBitmap(b);

            progressBar.setVisibility(View.INVISIBLE);

            statusText.setText(R.string.status_done);
            overrideButton();
            return true;
        } catch (FileNotFoundException e) {
            Log.e("loading", e.getMessage());
            return false;
        }
    }



    public void saveImageToInternalStorage(Bitmap image) {

        File localFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "file.jpg");
        if (!localFile.exists()) {
            FileOutputStream out;
            FileInputStream fis = null;
            try {
                out = new FileOutputStream(localFile);
                if (fis != null) {
                    IOUtils.copy(fis, out);
                    fis.close();
                }
                if (out != null) {

                    image.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
                }
            } catch (IOException e) {
                Log.e("error", e.getMessage());
            }
        }
    }





    @Override
    protected void onResume() {

        super.onResume();

        IntentFilter intentFilter
                = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(receiver, intentFilter);

    }

    @Override
    protected void onPause() {

        super.onPause();
        unregisterReceiver(receiver);
    }


    @Override

    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){

        switch(permsRequestCode){

            case 1:

                boolean readAccepted = grantResults[0]== PackageManager.PERMISSION_GRANTED;
                boolean writeAccepted = grantResults[1]== PackageManager.PERMISSION_GRANTED;

                break;
        }
    }
}


