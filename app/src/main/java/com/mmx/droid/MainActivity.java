package com.mmx.droid;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    Button saveContactsButton;
    Cursor cursor;
    //ArrayList<String> vCard;
    String vfile;
    boolean vfileCreated = false;
    private Context context;
    private TextView resultTextView;
    private String TAG = "MMX";
    private String folderPath;
    private ProgressDialog mDialog;
    private AsyncTask<Void, Void, Void> contactsBackupTask;
    private boolean mRunning = false;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();
        context = this;
        mDialog = new ProgressDialog(context);
        mDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress));
        mDialog.setCancelable(false);
        mDialog.setCancelable(true);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {

            }
        });
        String contactDirPath = Environment.getExternalStorageDirectory().toString() + File.separator + "Contacts";
        folderPath = contactDirPath;
        File contactsDirfile = new File(contactDirPath);
        if (!contactsDirfile.exists()) {
            contactsDirfile.mkdir();
        }
        vfile = contactDirPath + File.separator + "Contacts" + "_" + System.currentTimeMillis() + ".vcf";
        saveContactsButton = (Button) findViewById(R.id.saveContactsButton);
        saveContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mRunning) {
                    return;
                }
                if (contactsBackupTask!=null && contactsBackupTask.getStatus() == AsyncTask.Status.RUNNING) {
                    Toast.makeText(context,"Backup already running", Toast.LENGTH_SHORT).show();
                } else {
                    contactsBackupTask = new ContactsBackupTask();
                    contactsBackupTask.execute(null,null,null);
                }
            }
        });
        resultTextView = (TextView) findViewById(R.id.resultTextView);
        resultTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (vfileCreated) {
                    openFolder();
                } else {
                    Toast.makeText(context, "Phone contacts not yet exported. Please click BACKUP CONTACTS to export.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public void openFolder() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Uri uri = Uri.parse(folderPath);
        intent.setDataAndType(uri, "resource/folder");
        startActivity(Intent.createChooser(intent, "Open folder"));
    }

    private boolean saveContactToExternalDirectory() {

        boolean result;
        try {
            //vCard = new ArrayList<String>();
            cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            Log.i("TAG two", "cursor" + cursor);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                Log.i("Number of contacts", "cursorCount" + cursor.getCount());

                for (int i = 0; i < cursor.getCount(); i++) {
                    get(cursor);
                    //Log.i("TAG send contacts", "Contact " + (i + 1) + "VcF String is" + vCard.get(i));
                    boolean status = cursor.moveToNext();
                    if (!status) {
                        break;
                    }
                }
                cursor.close();

                /*StringBuffer s = new StringBuffer();
                s.append( vCard.toString());
                string = s.toString();
                file = new File(string);

                //  Log.i("s", ""+s);
                //  Log.i("string", ""+string);
                Log.i("file", ""+file);*/

            } else {
                Log.i("TAG", "No Contacts in Your Phone");
            }
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }


    public void get(Cursor cursor) throws IOException {

        String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
        Log.i("lookupKey", "" + lookupKey);
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);


        AssetFileDescriptor fd = null;
        try {
            fd = this.getContentResolver().openAssetFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            return;
        }

        FileInputStream fis = fd.createInputStream();
        byte[] buf = new byte[(int) fd.getDeclaredLength()];
        fis.read(buf);
        String vcardstring = new String(buf);

        String storage_path = vfile;
        FileOutputStream mFileOutputStream = new FileOutputStream(storage_path, true);
        mFileOutputStream.write(vcardstring.toString().getBytes());
        mFileOutputStream.close();

        //vCard.add(storage_path);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private class ContactsBackupTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            mRunning = true;
            super.onPreExecute();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showProgressDialog();
                }
            });
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideProgressDialog();
                }
            });
            mRunning = false;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            boolean result = saveContactToExternalDirectory();
            if (result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Phone contacts exported successfully", Toast.LENGTH_SHORT).show();
                    }
                });

                vfileCreated = true;
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Phone contacts not exported", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showProgressDialog() {
        try {
            if (!mDialog.isShowing()) {
                mDialog.setIndeterminate(true);
                mDialog.setMessage("Saving Contacts");
                mDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hideProgressDialog() {
        try {
            if (mDialog.isShowing())
                mDialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
