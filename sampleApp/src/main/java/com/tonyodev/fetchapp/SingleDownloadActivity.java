package com.tonyodev.fetchapp;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tonyodev.fetch2.AbstractFetchListener;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2core.DownloadBlock;
import com.tonyodev.fetch2core.Downloader;
import com.tonyodev.fetch2core.Extras;
import com.tonyodev.fetch2core.MutableExtras;
import com.tonyodev.fetch2okhttp.OkHttpDownloader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;

import timber.log.Timber;


public class SingleDownloadActivity extends AppCompatActivity implements FetchListener,ActionListener {


    private View mainView;
    private TextView progressTextView;
    private TextView titleTextView;
    private TextView etaTextView;
    private TextView downloadSpeedTextView;
    private EditText editTextURL;
    private Button btnDownload;
    private static final int STORAGE_PERMISSION_CODE = 200;
    private static final long UNKNOWN_REMAINING_TIME = -1;
    private static final long UNKNOWN_DOWNLOADED_BYTES_PER_SECOND = 0;
    private static final int GROUP_ID = "listGroup".hashCode();
    static final String FETCH_NAMESPACE = "DownloadListActivity";
    private Request request;
    private Fetch fetch;
    private FileAdapter fileAdapter;
    SharedPreferences preferences;
    private int generalId;
    TinyDB tinyDB;
    ArrayList<String> idArraylist;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_download);
        mainView = findViewById(R.id.activity_single_download);
        progressTextView = findViewById(R.id.progressTextView);
        titleTextView = findViewById(R.id.titleTextView);
        etaTextView = findViewById(R.id.etaTextView);
        editTextURL=(EditText)findViewById(R.id.edittext_url);
        btnDownload=(Button)findViewById(R.id.btn_download);
        downloadSpeedTextView = findViewById(R.id.downloadSpeedTextView);
        fetch = Fetch.Impl.getDefaultInstance();
        tinyDB=new TinyDB(this);
        idArraylist=new ArrayList<>();

       // preferences = PreferenceManager.getDefaultSharedPreferences(this);

        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .setDownloadConcurrentLimit(4)
                .setHttpDownloader(new OkHttpDownloader(Downloader.FileDownloaderType.PARALLEL))
                .setNamespace(FETCH_NAMESPACE)
                .build();
        fetch = Fetch.Impl.getInstance(fetchConfiguration);
        fileAdapter = new FileAdapter(this);
        recyclerView.setAdapter(fileAdapter);
      // checkStoragePermissions();
        checkStoragePermission();

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!editTextURL.getText().toString().trim().equals(""))
                {

                   // checkStoragePermission();
                }
                else
                {
                    Toast.makeText(view.getContext(),"Please Enter any Url",Toast.LENGTH_LONG).show();
                }
            }
        });
    }



   /* private void checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        } else {
            if(preferences.contains("requestidlist"))
            {
                if(!preferences.getString("requestidlist","").equals("")) {
                    String tempid = preferences.getString("requestidlist", "");
                    generalId = Integer.parseInt(tempid);
                    customResume();
                }
                else
                {
                    enqueueDownloads();
                }


            }
            else {
                enqueueDownloads();
            }
        }
    }*/


    private void enqueueDownloads() {
     /*   final List<Request> requests = Data.getFetchRequestWithGroupId(GROUP_ID);
        fetch.enqueue(requests, updatedRequests -> {

        }, error -> Timber.d("DownloadListActivity Error: %1$s", error.toString()));*/
       final String url = Data.sampleUrls[1];
     //  final String url = editTextURL.getText().toString().trim();
        // final String url = "https://www.youtube.com/watch?v=WrgGisb4BKo";
        final String filePath = Data.getSaveDir() + "/movies/" + Data.getNameFromUrl(url);
        request = new Request(url, filePath);
        request.setExtras(getExtrasForRequest(request));
        //request.setIdentifier(343);
        fetch.enqueue(request, updatedRequest -> {
            request = updatedRequest;
        }, error -> Timber.d("SingleDownloadActivity Error: %1$s", error.toString()));

      /*  SharedPreferences.Editor editor = preferences.edit();
        int tempid=request.getId();
            editor.putString("requestid", String.valueOf(tempid));
            editor.apply();*/

              idArraylist.add(String.valueOf(request.getId()));
              tinyDB.putListString("requestidlist",idArraylist);


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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        fetch.close();
    }


    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        } else {
          /*  if(preferences.contains("requestid"))
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

            parseidResumeDownloads();
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

        } else {
            Snackbar.make(mainView, R.string.permission_not_enabled, Snackbar.LENGTH_INDEFINITE).show();
        }
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


    private final FetchListener fetchListener = new AbstractFetchListener() {
        @Override
        public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {

            fileAdapter.addDownload(download);
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
            String tempUrl=download.getUrl();
            Uri uri = Uri.parse(tempUrl);
            Status status = download.getStatus();

            mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(SingleDownloadActivity.this);
            mBuilder.setContentTitle(uri.getLastPathSegment())
                    .setContentText(getStatusString(status))
                    .setSmallIcon(R.drawable.ic_arrow_downward_black_24dp);
            mBuilder.setProgress(100, 0, false);
            mNotifyManager.notify(download.getId(), mBuilder.build());

        }

        @Override
        public void onCompleted(@NotNull Download download) {
            Status status = download.getStatus();
            mBuilder.setContentText(getStatusString(status));
            mNotifyManager.notify(download.getId(), mBuilder.build());
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
            super.onError(download, error, throwable);
            Status status = download.getStatus();
            mBuilder.setContentText(getStatusString(status));
            mNotifyManager.notify(download.getId(), mBuilder.build());
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {


            Status status = download.getStatus();

            int progress = download.getProgress();
            mBuilder.setContentText(getString(R.string.percent_progress, progress));
            mBuilder.setProgress(100, progress, false);
            mNotifyManager.notify(download.getId(), mBuilder.build());
            fileAdapter.update(download, etaInMilliseconds, downloadedBytesPerSecond);
        }

        @Override
        public void onPaused(@NotNull Download download) {
            Status status = download.getStatus();
            mBuilder.setContentText(getStatusString(status));
            mNotifyManager.notify(download.getId(), mBuilder.build());
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onResumed(@NotNull Download download) {
            Status status = download.getStatus();
            mBuilder.setContentText(getStatusString(status));
            mNotifyManager.notify(download.getId(), mBuilder.build());
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onCancelled(@NotNull Download download) {
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onRemoved(@NotNull Download download) {
            Status status = download.getStatus();
            mBuilder.setContentText(getStatusString(status));
            mNotifyManager.notify(download.getId(), mBuilder.build());
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }

        @Override
        public void onDeleted(@NotNull Download download) {
            Status status = download.getStatus();
            mBuilder.setContentText(getStatusString(status));
            mNotifyManager.notify(download.getId(), mBuilder.build());
            fileAdapter.update(download, UNKNOWN_REMAINING_TIME, UNKNOWN_DOWNLOADED_BYTES_PER_SECOND);
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        if (request != null) {
            //Refresh the screen with the downloaded data. So we perform a download query
            fetch.getDownload(request.getId(), download -> {
                if (download != null) {
                    setProgressView(download.getStatus(), download.getProgress());
                }
            }).addListener(fetchListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        fetch.removeListener(this);
    }




  /*  @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enqueueDownload();
        } else {
            Snackbar.make(mainView, R.string.permission_not_enabled, Snackbar.LENGTH_LONG).show();
        }
    }*/

  /*  private void enqueueDownload() {
       //final String url = Data.sampleUrls[0];
         final String url = editTextURL.getText().toString().trim();
       // final String url = "https://www.youtube.com/watch?v=WrgGisb4BKo";
        final String filePath = Data.getSaveDir() + "/movies/" + Data.getNameFromUrl(url);
        request = new Request(url, filePath);
        request.setExtras(getExtrasForRequest(request));
        fetch.enqueue(request, updatedRequest -> {
            request = updatedRequest;
        }, error -> Timber.d("SingleDownloadActivity Error: %1$s", error.toString()));
    }*/

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

    private void setTitleView(@NonNull final String fileName) {

        final Uri uri = Uri.parse(fileName);
        titleTextView.setText(uri.getLastPathSegment());
    }

    private void setProgressView(@NonNull final Status status, final int progress) {
        switch (status) {
            case QUEUED: {
                progressTextView.setText(R.string.queued);
                break;
            }
            case ADDED: {
                progressTextView.setText(R.string.added);
                break;
            }
            case DOWNLOADING:
            case COMPLETED: {
                if (progress == -1) {
                    progressTextView.setText(R.string.downloading);
                } else {
                    final String progressString = getResources().getString(R.string.percent_progress, progress);
                    progressTextView.setText(progressString);
                }
                break;
            }
            default: {
                progressTextView.setText(R.string.status_unknown);
                break;
            }
        }
    }

    private void showDownloadErrorSnackBar(@NotNull Error error) {
        final Snackbar snackbar = Snackbar.make(mainView, "Download Failed: ErrorCode: " + error, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.retry, v -> {
            fetch.retry(request.getId());
            snackbar.dismiss();
        });
        snackbar.show();
    }

   /* private void updateViews(@NotNull Download download, long etaInMillis, long downloadedBytesPerSecond, @Nullable Error error) {
        if (request.getId() == download.getId()) {
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(Utils.getETAString(this, etaInMillis));
            downloadSpeedTextView.setText(Utils.getDownloadSpeedString(this, downloadedBytesPerSecond));
            if (error != null) {
                showDownloadErrorSnackBar(download.getError());
            }
        }
    }*/

    @Override
    public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
        setTitleView(download.getFile());
        setProgressView(download.getStatus(), download.getProgress());
        //updateViews(download, 0, 0, null);
    }

    @Override
    public void onCompleted(@NotNull Download download) {
       // updateViews(download, 0, 0, null);
    }

    @Override
    public void onError(@NotNull Download download, @NotNull Error error, @Nullable Throwable throwable) {
       // updateViews(download, 0, 0, download.getError());
    }

    @Override
    public void onDownloadBlockUpdated(@NotNull Download download, @NotNull DownloadBlock downloadBlock, int totalBlocks) {

    }

    @Override
    public void onStarted(@NotNull Download download, @NotNull List<? extends DownloadBlock> downloadBlocks, int totalBlocks) {



       // updateViews(download, 0, 0, null);
    }

    @Override
    public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {



        //updateViews(download, etaInMilliseconds, downloadedBytesPerSecond, null);
    }

    @Override
    public void onPaused(@NotNull Download download) {
        //updateViews(download, 0, 0, null);
    }

    @Override
    public void onResumed(@NotNull Download download) {
        //updateViews(download, 0, 0, null);
    }

    @Override
    public void onCancelled(@NotNull Download download) {
       // updateViews(download, 0, 0, null);
    }

    @Override
    public void onRemoved(@NotNull Download download) {
        //updateViews(download, 0, 0, null);
    }

    @Override
    public void onDeleted(@NotNull Download download) {
        //updateViews(download, 0, 0, null);
    }

    @Override
    public void onAdded(@NotNull Download download) {
        setTitleView(download.getFile());
        setProgressView(download.getStatus(), download.getProgress());
        //updateViews(download, 0, 0, null);
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

    @Override
    public void onWaitingNetwork(@NotNull Download download) {

    }

    private String getStatusString(Status status) {
        switch (status) {
            case COMPLETED:
                return "Done";
            case DOWNLOADING:
                return "Downloading";
            case FAILED:
                return "Error";
            case PAUSED:
                return "Paused";
            case QUEUED:
                return "Waiting in Queue";
            case REMOVED:
                return "Removed";
            case NONE:
                return "Not Queued";
            default:
                return "Unknown";
        }
    }
}
