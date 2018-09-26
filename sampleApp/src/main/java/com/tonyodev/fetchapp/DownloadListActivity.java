package com.tonyodev.fetchapp;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;

import com.tonyodev.fetch2.AbstractFetchListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2core.Downloader;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2core.Extras;
import com.tonyodev.fetch2core.MutableExtras;
import com.tonyodev.fetch2okhttp.OkHttpDownloader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class DownloadListActivity extends AppCompatActivity implements ActionListener {

    private static final int STORAGE_PERMISSION_CODE = 200;
    private static final long UNKNOWN_REMAINING_TIME = -1;
    private static final long UNKNOWN_DOWNLOADED_BYTES_PER_SECOND = 0;
    private static final int GROUP_ID = "listGroup".hashCode();
    static final String FETCH_NAMESPACE = "DownloadListActivity";

    private View mainView;
    private FileAdapter fileAdapter;
    private Fetch fetch;
    SharedPreferences preferences;
    private Request request;
    private int generalId;
    TinyDB tinyDB;
    ArrayList<String> idArraylist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);
       // preferences = PreferenceManager.getDefaultSharedPreferences(this);

        setUpViews();

        final FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .setDownloadConcurrentLimit(4)
                .setHttpDownloader(new OkHttpDownloader(Downloader.FileDownloaderType.PARALLEL))
                .setNamespace(FETCH_NAMESPACE)
                .build();
        fetch = Fetch.Impl.getInstance(fetchConfiguration);
        tinyDB=new TinyDB(this);
        idArraylist=new ArrayList<>();
        checkStoragePermissions();
    }

    private void setUpViews() {
        final SwitchCompat networkSwitch = findViewById(R.id.networkSwitch);
        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        mainView = findViewById(R.id.activity_main);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        networkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                fetch.setGlobalNetworkType(NetworkType.WIFI_ONLY);
            } else {
                fetch.setGlobalNetworkType(NetworkType.ALL);
            }
        });
        fileAdapter = new FileAdapter(this);
        recyclerView.setAdapter(fileAdapter);
    }

    protected void customResume(int id)
    {


            //Refresh the screen with the downloaded data. So we perform a download query
            fetch.getDownload(id, download -> {
                Log.d("id","id is here in customResume Method"+id);
                if (download != null) {
                    //setProgressView(download.getStatus(), download.getProgress());
                    fileAdapter.addDownload(download);
                }
            }).addListener(fetchListener);

    }

    private void parseidResumeDownloads()
    {
        ArrayList<String> templist=tinyDB.getListString("requestidlist");

        if(templist!=null)
        {
            if(templist.size()!=0)
            {
                Log.d("checkid","templist is not null in parseidResumeDownloads Methods and size of list"+ templist.size());
                for(String id : templist)
                {
                    customResume(Integer.parseInt(id));
                }

            }
            else
            {
                Log.d("checkid","inner else --- id is  null in parseidResumeDownloads Methods and size of list"+templist.size());
                enqueueDownloads();
            }
        }
        else
        {
            Log.d("checkid","outer else --- is  null in parseidResumeDownloads Methods and size of list"+templist.size());
            enqueueDownloads();
        }
    }

    @Override
    protected void onResume() {
     super.onResume();

       /*     fetch.getDownloadsInGroup(GROUP_ID, downloads -> {
                  Log.d("id","id is here in onResume Method"+GROUP_ID);
            final ArrayList<Download> list = new ArrayList<>(downloads);
            Collections.sort(list, (first, second) -> Long.compare(first.getCreated(), second.getCreated()));
            for (Download download : list) {
                fileAdapter.addDownload(download);
                break;
            }
        }).addListener(fetchListener);*/


       super.onResume();

        if (request != null) {
            //Refresh the screen with the downloaded data. So we perform a download query
            fetch.getDownload(request.getId(), download -> {
                Log.d("id","id is here in onResume Method"+request.getId());
                if (download != null) {
                    //setProgressView(download.getStatus(), download.getProgress());
                    fileAdapter.addDownload(download);
                }
            }).addListener(fetchListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        fetch.removeListener(fetchListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fetch.close();
    }

    private final FetchListener fetchListener = new AbstractFetchListener() {
        @Override
        public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
            fileAdapter.addDownload(download);
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onCompleted(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
            super.onError(download, error, throwable);
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
            fileAdapter.update(download, etaInMilliseconds, downloadedBytesPerSecond);
        }

        @Override
        public void onPaused(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onResumed(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onCancelled(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onRemoved(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onDeleted(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }
    };

    private void checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        } else {
            /*if(preferences.contains("requestid"))
            {
                if(!preferences.getString("requestid","").equals("")) {
                    String tempid = preferences.getString("requestid", "");
                    generalId = Integer.parseInt(tempid);
                    customResume(generalId);
                }
                else
                {
                    enqueueDownloads();
                }


            }
            else {
                enqueueDownloads();
            }*/
            parseidResumeDownloads();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
           /* if(preferences.contains("requestid"))
            {
                if(!preferences.getString("requestid","").equals("")) {
                    String tempid = preferences.getString("requestid", "");
                    generalId = Integer.parseInt(tempid);
                    customResume(generalId);
                }
                else
                {
                    enqueueDownloads();
                }


            }
            else
            {
                enqueueDownloads();
            }*/
            parseidResumeDownloads();
        } else {
            Snackbar.make(mainView, R.string.permission_not_enabled, Snackbar.LENGTH_INDEFINITE).show();
        }
    }

    private void enqueueDownloads() {
     /*   final List<Request> requests = Data.getFetchRequestWithGroupId(GROUP_ID);
        fetch.enqueue(requests, updatedRequests -> {

        }, error -> Timber.d("DownloadListActivity Error: %1$s", error.toString()));*/
           final String url = Data.sampleUrls[1];
       // final String url = editTextURL.getText().toString().trim();
        // final String url = "https://www.youtube.com/watch?v=WrgGisb4BKo";
        final String filePath = Data.getSaveDir() + "/movies/" + Data.getNameFromUrl(url);
        request = new Request(url, filePath);
        request.setExtras(getExtrasForRequest(request));
        //request.setIdentifier(343);
        fetch.enqueue(request, updatedRequest -> {
            request = updatedRequest;
        }, error -> Timber.d("SingleDownloadActivity Error: %1$s", error.toString()));

    /*    SharedPreferences.Editor editor = preferences.edit();
           int tempid=request.getId();

        editor.putString("requestid",String.valueOf(tempid));
        editor.apply();*/
        idArraylist.add(String.valueOf(request.getId()));
        tinyDB.putListString("requestidlist",idArraylist);

    }
    private Extras getExtrasForRequest(Request request) {
        final MutableExtras extras = new MutableExtras();
        extras.putBoolean("testBoolean", true);
        extras.putString("testString", "test");
        extras.putFloat("testFloat", Float.MIN_VALUE);
        extras.putDouble("testDouble",Double.MIN_VALUE);
        extras.putInt("testInt", Integer.MAX_VALUE);
        extras.putLong("testLong", Long.MAX_VALUE);
        return extras;
    }
    @Override
    public void onPauseDownload(int id) {
        fetch.pause(id);
    }

    @Override
    public void onResumeDownload(int id) {
        fetch.resume(id);
    }
    public void removedIdFromSharedP(int id)
    {
        ArrayList<String> templist=tinyDB.getListString("requestidlist");
        if(templist.size()!=0)
        {
            Log.d("checkid","templist is not null in parseidResumeDownloads Methods and size of list"+ templist.size());
            for(String tempid : templist)
            {
                if(id==(Integer.parseInt(tempid)))
                {
                    if(templist.remove(String.valueOf(id)))
                    tinyDB.putListString("requestidlist",templist);

                }
            }

        }

    }
    @Override
    public void onRemoveDownload(int id) {
        fetch.remove(id);
        removedIdFromSharedP(id);
    }

    @Override
    public void onRetryDownload(int id) {
        fetch.retry(id);
    }

}