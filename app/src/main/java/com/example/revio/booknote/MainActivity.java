package com.example.revio.booknote;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int DIALOG_FORMAT = 1;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechList;
    private EditText mText;
    private ArrayAdapter<String> mAdapter;
    private Tag mCurrentTag;
    private ArrayList<String> mCurrentNotes = new ArrayList<String>();
    private ListView mList;
    private Button mAddButton;

    private static final String TAG = "MyActivity";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mList = (ListView) findViewById(R.id.list);
        mList.setAdapter(mAdapter);
        registerForContextMenu(mList);

        mAddButton = (Button) findViewById(R.id.add_button);
        mText = (EditText) findViewById(R.id.text);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        prepareFilters();
        onNewIntent(getIntent());

    }

    private void prepareFilters() {
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndefIntent = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefIntent.addDataType("text/plain");
        } catch (Exception e) {
            // should never happen
        }

        IntentFilter formatableIntent = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

        mFilters = new IntentFilter[] { formatableIntent, ndefIntent};
        mTechList = new String[][] { new String[] {NdefFormatable.class.getName() }, new String[]
                { Ndef.class.getName() } };
    }

    public void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechList);
    }

    public void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    public void onNewIntent(Intent intent) {
        setIntent(intent);
        String action = intent.getAction();

        Bundle bundle = intent.getExtras();
        if (action.equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
            handleActionTechDiscovered(bundle);
        } else if (action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
            handleActionNDEFDiscovered(bundle);
        }
    }

    private void handleActionTechDiscovered(Bundle bundle) {

    }

    private void handleActionNDEFDiscovered(Bundle bundle) {
        Tag tag = bundle.getParcelable(NfcAdapter.EXTRA_TAG);
        mCurrentTag = tag;
        // checkWritable(tag);
        readNotes();
    }

    private void checkWritable(Tag tag) {
        if (! Ndef.get(tag).isWritable()) {
            showMessage("Il tag non è scrivibile");
            mAddButton.setEnabled(false);
        } else {
            mAddButton.setEnabled(true);
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void readNotes() {
        mCurrentNotes.clear();
        mAdapter.clear();

        mCurrentNotes.addAll(Arrays.asList(readRecords()));

        for (String note : mCurrentNotes) {
            mAdapter.add(note);
        }
    }

    private String[] readRecords() {
        Ndef ndefTag = null;

        try {
            ndefTag = Ndef.get(mCurrentTag);
            Log.d(TAG, "connect() method return error sometimes");
            ndefTag.connect();
            NdefRecord[] records = ndefTag.getNdefMessage().getRecords();

            ArrayList<String> strings = new ArrayList<String>();
            for (NdefRecord record : records) {
                strings.add(decodeTextPayload(record.getPayload()));
            }
            String[] output = new String[strings.size()];
            strings.toArray(output);
            return output;
            } catch (Exception e){
            e.printStackTrace();
            return null;
        } finally {
            if (ndefTag != null && ndefTag.isConnected()) {
                try {
                    ndefTag.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String decodeTextPayload(byte[] payload) throws Exception {
        byte status = payload[0];
        int languageCodeLenght = status & 0x3f;
        return new String(payload, 1 + languageCodeLenght, payload.length - 1 - languageCodeLenght, "UTF-8");
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
}
