package com.htd.yasmin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;

import java.io.File;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    public static final int PERMISSION_BLUETOOTH = 1;
    private String TAG = "WV";
    WebView myWebView;
    private ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> uploadMessage;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private Uri mCapturedImageURI = null;

    private String WEB_URL = "http://yasmin.z-techno.com";

    SwipeRefreshLayout mySwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        mySwipeRefreshLayout = (SwipeRefreshLayout)this.findViewById(R.id.swipeRefresh);

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        };

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else {
            init();
        }
    }


    public void handlePrint(String printText){

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        try{
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, PERMISSION_BLUETOOTH);
            } else {
                BluetoothConnection connection = BluetoothPrintersConnections.selectFirstPaired();
                if (connection != null) {
                    Toast.makeText(getApplicationContext(), "Sedang Mencetak..", Toast.LENGTH_SHORT).show();
                    Log.d("PRINTING",printText);
                    EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);
                    printer.printFormattedTextAndOpenCashBox(printText,100);
                } else {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(
                                BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        enableBtIntent.putExtra("printText",printText);
                        ((Activity) getApplicationContext()).startActivityForResult(enableBtIntent, PERMISSION_BLUETOOTH);
                    }else{
                        Toast.makeText(getApplicationContext(), "Silahkan sambungkan printer terlebih dahulu!", Toast.LENGTH_LONG).show();
                    }

                }
            }
        }catch(Exception e){
            Toast.makeText(getApplicationContext(), "Gagal Mencetak", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (myWebView.canGoBack()) {
                        myWebView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    void init()
    {
        myWebView = (WebView) findViewById(R.id.webview);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.addJavascriptInterface(new JavaScriptInterface(this), "Android");
        myWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        myWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        myWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        myWebView.getSettings().setAllowFileAccess(true);
        myWebView.getSettings().setAllowContentAccess(true);
        myWebView.getSettings().setDomStorageEnabled(true);

        myWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);

                try {
                    view.stopLoading();
                } catch (Exception e) {
                }

                if (view.canGoBack()) {
                    view.goBack();
                }

                view.loadUrl("about:blank");
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                if(description.contains("ERR_INTERNET_DISCONNECTED")){
                    alertDialog.setTitle("Tidak ada koneksi internet");
                    alertDialog.setMessage("Cek koneksi internet anda dan coba lagi!");

                    alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,"Coba Lagi" , new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                            startActivity(getIntent());
                        }
                    });
                }else{
                    alertDialog.setTitle("Gagal memuat aplikasi");
                    alertDialog.setMessage("Silahkan hubungi pihak yang bersangkutan!");
                }
                alertDialog.setCancelable(false);


                alertDialog.show();
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mySwipeRefreshLayout.setRefreshing(false);
            }
        });

        myWebView.setWebChromeClient(new WebChromeClient() {
            // For 3.0+ Devices (Start)
            // onActivityResult attached before constructor
            protected void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(i, "File Browser"), FILECHOOSER_RESULTCODE);
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }


            // For Lollipop 5.0+ Devices
            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }

                uploadMessage = filePathCallback;

//                Intent intent = fileChooserParams.createIntent();
                try {
                    // Create AndroidExampleFolder at sdcard

                    File imageStorageDir = new File(
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES)
                            , "Yasmin");

                    if (!imageStorageDir.exists()) {
                        // Create AndroidExampleFolder at sdcard
                        imageStorageDir.mkdirs();
                    }

                    // Create camera captured image file path and name
                    File file = new File(
                            imageStorageDir + File.separator + "IMG_"
                                    + String.valueOf(System.currentTimeMillis())
                                    + ".jpg");

                    mCapturedImageURI = Uri.fromFile(file);
                    // Camera capture image intent
                    final Intent captureIntent = new Intent(
                            android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("image/*");

                    // Create file chooser intent
                    Intent chooserIntent = Intent.createChooser(i, "Image Chooser");

                    // Set camera intent to file chooser
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                            , new Parcelable[] { captureIntent });

                    // On select image call onActivityResult method of activity
                    startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
//                    startActivityForResult(intent, REQUEST_SELECT_FILE);
                } catch (ActivityNotFoundException e) {
                    uploadMessage = null;
                    Toast.makeText(MainActivity.this, "Cannot Open File Chooser", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }

            //For Android 4.1 only
            protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUploadMessage = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "File Browser"), FILECHOOSER_RESULTCODE);
            }

            protected void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d(TAG, consoleMessage.message() + " -- From line " +
                        consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                return true;
            }
        });
        myWebView.loadUrl(WEB_URL);

        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        myWebView.reload();
                    }
                }
        );
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == FILECHOOSER_RESULTCODE) {
                if (uploadMessage == null)
                    return;

                Uri[] results = null;

                // Check that the response is a good one
                if (resultCode == RESULT_OK) {
                    if (intent == null) {
                        // If there is not data, then we may have taken a photo
                        if (mCapturedImageURI != null) {
                            results = new Uri[]{mCapturedImageURI};
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = WebChromeClient.FileChooserParams.parseResult(resultCode, intent);
                        }
                    }
                }

                uploadMessage.onReceiveValue(results);
                uploadMessage = null;
            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage)
                return;
            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
            // Use RESULT_OK only if you're implementing WebView inside an Activity
            Uri result = null; //intent == null || resultCode != MainActivity.RESULT_OK ? null : intent.getData();
            try{
                if (resultCode != RESULT_OK) {

                    result = null;

                } else {

                    // retrieve from the private variable if the intent is null
                    result = intent == null ? mCapturedImageURI : intent.getData();
                }
            }
            catch(Exception e)
            {
                Toast.makeText(getApplicationContext(), "activity :"+e,
                        Toast.LENGTH_LONG).show();
            }
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        } else if (requestCode == PERMISSION_BLUETOOTH) {
            if(resultCode == Activity.RESULT_OK){
                handlePrint(intent.getStringExtra("printText"));
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Gagal menghidupkan bluetooth!", Toast.LENGTH_LONG).show();
            }
        }else
            Toast.makeText(MainActivity.this, "Failed to Upload Image", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (!Arrays.asList(grantResults).contains(PackageManager.PERMISSION_DENIED)) {
                //all permissions have been granted
                init();
            }
        }
    }
}