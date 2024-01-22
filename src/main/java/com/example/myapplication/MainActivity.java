package com.example.myapplication;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

public class MainActivity extends AppCompatActivity {
    private NfcAdapter nfcAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not supported on this device.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }





    }

    @Override
    protected void onResume() {
        super.onResume();

        if (nfcAdapter != null) {
            // Enable NFC foreground dispatch to handle scanned tags


            // Check if NFC is not enabled, then prompt the user to enable it (for Android 13 and above)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !nfcAdapter.isEnabled()) {
                Intent enableNfcIntent = new Intent(Settings.ACTION_NFC_SETTINGS);
                startActivity(enableNfcIntent);
            } else {
                Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
                nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (nfcAdapter != null) {
            // Disable NFC foreground dispatch when the app is in the background
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Handle the NFC intent when a tag is scanned
        NdefMessage[] messages = getNfcMessagesFromIntent(intent);
        if (messages != null && messages.length > 0) {
            String numberText = new String(messages[0].getRecords()[0].getPayload());
            displayNFCmessage(numberText);
        }
    }

    private void displayNFCmessage(String message) {

        TextView nfcDataTextView = findViewById(R.id.nfcDataTextView);

        if (message.length() > 3) {
            String payloadString = message.substring(3); // Skip the first two characters
            // for later einai mono otan den exo base64
            String d1messagebase = convertBase64ToBinaryString(payloadString);  //h to original i to payloadmessage

            String DecompressedMessage = decompressZlib(d1messagebase);



            // Display the extracted payload
            //Toast.makeText(this, "Received NFC message: " + payloadString, Toast.LENGTH_SHORT).show();

            nfcDataTextView.setText(DecompressedMessage);

        } else {
            // If the message is too short, display the original message
            //Toast.makeText(this, "Received NFC message: " + message, Toast.LENGTH_SHORT).show();

        }

    }

    private NdefMessage[] getNfcMessagesFromIntent(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            // Extract the NFC data from the intent
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null) {
                NdefMessage[] messages = new NdefMessage[rawMessages.length];
                for (int i = 0; i < rawMessages.length; i++) {
                    messages[i] = (NdefMessage) rawMessages[i];
                }
                return messages;
            }
        }
        return null;
    }

    private String convertBase64ToBinaryString(String base64String) {
        try {
            // Decode base64 string to bytes
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);

            // Convert bytes to binary string
            StringBuilder binaryString = new StringBuilder();
            for (byte b : decodedBytes) {
                for (int i = 7; i >= 0; --i) {
                    binaryString.append((b >> i) & 1);
                }
            }
            return binaryString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error decoding base64 string";
        }
    }

    private String decompressZlib(String binaryData) {
        try {
            // Convert binary string to byte array
            byte[] compressedBytes = new byte[binaryData.length() / 8];
            for (int i = 0; i < binaryData.length(); i += 8) {
                compressedBytes[i / 8] = (byte) Integer.parseInt(binaryData.substring(i, i + 8), 2);
            }

            // Create an Inflater object for decompression
            Inflater inflater = new Inflater();
            inflater.setInput(compressedBytes);

            // Create a ByteArrayOutputStream to hold the decompressed data
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedBytes.length);

            // Decompress the data
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }

            // Convert decompressed byte array to string
            inflater.end();
            return new String(outputStream.toByteArray());
        } catch (DataFormatException e) {
            e.printStackTrace();
            return "Error decompressing data";
        }
    }
}
