package com.example.user.task_3;

import android.app.DownloadManager;
import android.app.usage.NetworkStats;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;

    static final String IMAGE_ADDRESS = "https://i.ytimg.com/vi/gGBKCWMSw4o/maxresdefault.jpg";
    

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

                        //bitmap
                        saveImageToInternalStorage(bitmap);


                        overrideButton(downloadId);


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


    private void overrideButton(final long downloadId) {
        button.setEnabled(true);
        button.setText(R.string.button_open);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openImgIntent = new Intent();
                openImgIntent.setType(Intent.ACTION_VIEW);
                openImgIntent.setDataAndType(downloadManager.getUriForDownloadedFile(downloadId), "image/*");
                startActivity(openImgIntent);
            }
        });
    }


    private boolean loadImageFromStorage() {

        try {
            loadedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/saved_images/filename");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(loadedFile));

            Log.d("Loading: ", loadedFile.getAbsolutePath());

            pictureView.setImageBitmap(b);

            progressBar.setVisibility(View.INVISIBLE);

            statusText.setText(R.string.status_done);
            button.setEnabled(true);
            button.setText(R.string.button_open);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent openImgIntent = new Intent();
                    openImgIntent.setType(Intent.ACTION_VIEW);
                    Log.d("opening ", loadedFile.getAbsolutePath().toString());
                    openImgIntent.setDataAndType(Uri.fromFile(loadedFile), "image/*");
                    startActivity(openImgIntent);

                }
            });
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    public void saveImageToInternalStorage(Bitmap image) {

//        try {
//            // Use the compress method on the Bitmap object to write image to
//            // the OutputStream
//            FileOutputStream fos = openFileOutput("filename", Context.MODE_PRIVATE);
//
//            // Writing the bitmap to the output stream
//
//            image.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//            fos.close();
//
//            Log.d("saveToInternalStorage()", "saved");
//            return true;
//        } catch (Exception e) {
//            Log.e("saveToInternalStorage()", e.getMessage());
//            return false;
//        }


        // WTF


        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images/");
        myDir.mkdirs();

        String fname = "Image";
        File file = new File(myDir, fname);


        try {
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            if (!file.exists())
                file.createNewFile();

            FileOutputStream out = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // END WTF
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

}


