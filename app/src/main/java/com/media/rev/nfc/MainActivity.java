package com.media.rev.nfc;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;


public class MainActivity extends Activity {

    public static final String TAG = "NfcDEmo";


    private TextView mmTextView,nfcData;
    private NfcAdapter mNfcAdapter;
    private Tag nfctag;
    private NdefMessage nmsg[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mmTextView = (TextView) findViewById(R.id.textView1);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {

            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        TextView nfctitle = (TextView)findViewById(R.id.Title);
        Button button = (Button)findViewById(R.id.nfc_rawdData);
        if (!mNfcAdapter.isEnabled()) {
            mmTextView.setText("NFC is disabled.");
            button.setVisibility(View.INVISIBLE);
        } else {
            mmTextView.setText(R.string.availability);
            nfctitle.setVisibility(View.VISIBLE);
            button.setVisibility(View.VISIBLE);
        }
        nfcData =(TextView)findViewById(R.id.nfc_data);
        nfcData.setText("");

        Intent intent = getIntent();
        nfctag =(Tag)intent.getParcelableExtra(mNfcAdapter.EXTRA_TAG);
        //Displays the tag id and tech
        if (nfctag != null){
            TextView nfcId = (TextView)findViewById(R.id.nfc_id);
            nfcId.setText("");
            nfcId.setText(toHex(nfctag.getId()));
            TextView nfcTech = (TextView)findViewById(R.id.nfc_tech);
            nfcTech.setText("");
            String Tech[] =nfctag.getTechList();
            for (String techie :Tech){
                nfcTech.append(techie + "\n");
            }
            handleIntent(getIntent());
        }

    }

    private String toHex(byte[] data){
        String s="";
        for (byte dat :data){
            s += String.format("%02x",dat);
        }
        return s;
    }

    private void handleIntent(Intent intent) {

        Ndef ndef = Ndef.get(nfctag);
        if (ndef != null) {
            processing(ndef);
        }
    }



    private void processing(Ndef ndef) {

        try {

            ndef.connect();
            NdefMessage msg = ndef.getNdefMessage();
            nfcData.setText("");

            for (int i = 0; i < msg.getRecords().length; i++) {
                NdefRecord record = msg.getRecords()[i];
                String mime = record.toMimeType();
                nfcData.append("Mime TYPE " + mime + "\n\n");
                if (mime!= null) {
                    try {
                        dataOutput(record.getPayload());
                    } catch (Exception e) {
                        nfcData.append("\nCould not read content.");
                    }

                    nfcData.append("\n\n");
                }
                nfcData.append(record.toUri().toString());
                nfcData.append("\n\n");
            }

        } catch (Exception e) {
        } finally {
            try {
                ndef.close();
            } catch (Exception e) {
            }
        }

    }

    public void displayRawdData(View view) {
        nfcData.setText("");
        String output = "";
        MifareUltralight mfare = MifareUltralight.get(nfctag);
        if (mfare != null) {
            try {


                int type = mfare.getType();
                int len;

                if (type == mfare.TYPE_ULTRALIGHT_C) len = 44;
                else len = 16;
                String ascii = "";
                String hex = "";
                for (int i = 0; i < len; i += 4) {
                    byte[] b = mfare.readPages(i);
                    ascii += new String(b, Charset.forName("US-ASCII")) + "\n";

                    hex += toHex(b) + "\n";
                }
                output += "ASCII:\n" + ascii + "\nHex :\n" + hex;
                nfcData.setText(output);
            } catch (Exception e) {

                e.printStackTrace();
                Toast t = Toast.makeText(this, "no raw data available now ", Toast.LENGTH_LONG);
                t.show();
            } finally {
                try {
                    mfare.close();
                } catch (Exception e) {
                    e.printStackTrace();

                    Toast t = Toast.makeText(this, " Mifare UltraLight support unavailable ", Toast.LENGTH_LONG);
                    t.show();
                }
            }
        }
    }
    private void dataOutput(byte datta[]) {
        char state = (char) datta[0];
        boolean isUtf8 = (state & 0x80) != 0;
        int langLength = state & 0x7F;
        if (langLength > 0) {
            byte langBytes[] = new byte[langLength];
            System.arraycopy(datta, 1, langBytes, 0, langLength);
            nfcData.append("Language: " + new String(langBytes) + "\n");
        }
        int dataLength = datta.length - langLength - 1;
        byte dataBytes[] = new byte[dataLength];
        System.arraycopy(datta, 1 + langLength, dataBytes, 0, dataLength);
        nfcData.append("\n" + new String(dataBytes));
    }



}
