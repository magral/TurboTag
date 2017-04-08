package com.example.leah.edg_touch;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.media.Image;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class PlayerScreen extends AppCompatActivity implements NfcAdapter.OnNdefPushCompleteCallback, NfcAdapter.CreateNdefMessageCallback{
    //NFC-------------------------------
    NfcAdapter nfcAdapter;
    private ArrayList<String> messagesToSendArray = new ArrayList<String>();
    private ArrayList<String> messagesReceivedArray = new ArrayList<String>();
    private String[][] techListsArray;
    PendingIntent pendingIntent;
    IntentFilter[] intentFiltersArray;
    //----------------------------------
    //Connections
    TextView response;
    ConnectionDefinition connectionDefinition;
    int ID = Scoreboard.getLocalID();
    //----------------------------------
    //Image Loading
    private CustomLayout bg;
    private CustomButton beam;
    private ImageView top, bt, ansbg;
    private TextView timer;
    private EditText pInput;
    //View spaces
    private View rootView;
    private LinearLayout answerLayout;
    private CountDownTimer gameTimer;
    //----------------------------------
    // Socketing
    private Socket socket;
    /*
    {
        try{
            socket = IO.socket("http://192.168.1.87:9001");
        } catch(URISyntaxException e){
            e.printStackTrace();
        }
    }
    //----------------------------------
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectionDefinition = new ConnectionDefinition();
        {
            try {
                //socket = IO.socket("http://192.168.1.87:9001");
                //System.out.println(());
                socket = IO.socket("http://192.168.25.117:443");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        if(Scoreboard.isPlayersEmpty()){
            GetUsers gu = new GetUsers();
            gu.execute();
        }
        setContentView(R.layout.activity_player_screen);
        //Load Images
        timer = (TextView) findViewById(R.id.playerTimer);
        bg = (CustomLayout) findViewById(R.id.playerLayout);
        top = (ImageView) findViewById(R.id.top_border);
        bt = (ImageView) findViewById(R.id.bottom_border);
        ansbg = (ImageView) findViewById(R.id.playerBG);
        beam = (CustomButton) findViewById(R.id.playerSubmit);
        pInput = (EditText) findViewById(R.id.playerInput);

        rootView = findViewById(android.R.id.content).getRootView();

        Picasso.with(this).load(R.drawable.title_bg).into(bg);
        Picasso.with(this).load(R.drawable.top_border).fit().into(top);
        Picasso.with(this).load(R.drawable.bottom_border).fit().into(bt);
        Picasso.with(this).load(R.drawable.question_answer_bg).fit().into(ansbg);
        Picasso.with(this).load(R.drawable.beam_send_button).into(beam);

        //NFC---------------------------------------------
        Intent intent = new Intent(getApplicationContext(), GameMasterScreen.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        pendingIntent = PendingIntent.getActivity(
                this, 0, intent.addFlags( Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_CANCEL_CURRENT);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

        try {
            ndef.addDataType("text/plain");
        }
        catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        intentFiltersArray = new IntentFilter[] {ndef, };
        techListsArray = new String[][] { new String[] { NfcF.class.getName() } };
        handleNfcIntent(getIntent());
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        } else {
            Toast.makeText(this, "NFC not available on this device",
                    Toast.LENGTH_SHORT).show();
        }
        nfcAdapter.setNdefPushMessageCallback(this, this);

        nfcAdapter.setOnNdefPushCompleteCallback(this, this);
        //----------------------------------------------------
        //Sockets & DB Connections
        /*socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args){
                System.out.println("Player connected");
            }
        });*/
        socket.on("game start", gameStart);
        gameTimer = new CountDownTimer(15000, 1000) {

            public void onTick(long millisUntilFinished) {
                timer.setText(" " + (millisUntilFinished / 1000));
            }

            public void onFinish() {
                timer.setText("Round Over!");
            }
        };
        socket.on("transfer game master", transferGameMaster);
        //socket.on("get question", onGetQuestion );
        socket.connect();
        beam.setOnClickListener(customOnClickListener());
    }

    public class GetUsers extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... params){
            try {
                Connection con = connectionDefinition.CONN();
                if(con == null){
                    throw new Error("SQL Connection error");
                }
                else {
                    String query = "SELECT ID FROM Users";
                    Statement statement = con.createStatement();
                    ResultSet rs = statement.executeQuery(query);

                    while(rs.next()){
                        int uid = rs.getInt("ID");
                        PlayerDefinition p = new PlayerDefinition(uid);
                        Scoreboard.addPlayer(p);
                    }
                }
            } catch (SQLException e){
                e.printStackTrace();
            }
            return null;
        }

    }
    View.OnClickListener customOnClickListener()  {
        return new View.OnClickListener() {
            public void onClick(View v) {
                addMessage(v, pInput.getText().toString());
            }
        };
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        socket.disconnect();

    }

    public void addMessage(View view, String newMessage) {
        messagesToSendArray.add(newMessage);
        System.out.println("STRING SENDING::: " + newMessage);
        Toast.makeText(this, "Added Message", Toast.LENGTH_LONG).show();
    }


    //Save our Array Lists of Messages for if the user navigates away
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putStringArrayList("messagesToSend", messagesToSendArray);
        savedInstanceState.putStringArrayList("lastMessagesReceived",messagesReceivedArray);
    }

    //Load our Array Lists of Messages for when the user navigates back
    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        messagesToSendArray = savedInstanceState.getStringArrayList("messagesToSend");
        messagesReceivedArray = savedInstanceState.getStringArrayList("lastMessagesReceived");
    }

    @Override
    public void onNdefPushComplete(NfcEvent event){
        messagesToSendArray.clear();
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event){
        //Will be called when another NFC capable device is detected
        if(messagesToSendArray.size() == 0){
            System.out.println("no messages in message to send array");
            return null;
        }
        socket.on("alert player correct", listenForCorrect);
        //socket.emit("player sent", true);
        NdefRecord[] recordsToAttach = createRecords();
        return new NdefMessage(recordsToAttach);
    }

    public NdefRecord[] createRecords(){
        System.out.println("Create Records");
        NdefRecord[] records = new NdefRecord[messagesToSendArray.size() + 1];
        for(int i = 0; i < messagesToSendArray.size(); i++){
            String sendMessage = messagesToSendArray.get(i);
            byte[] payload = sendMessage.getBytes(Charset.forName("UTF-8"));

            NdefRecord record = NdefRecord.createMime("text/plain",payload);
            records[i] = record;
        }
        //Ensure our application is used for data transfer
        records[messagesToSendArray.size()] = NdefRecord.createApplicationRecord(getPackageName());
        return records;
    }

    private void handleNfcIntent(Intent NfcIntent) {
        System.out.println("Handle");
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(NfcIntent.getAction())) {
            System.out.println("discover");
            Parcelable[] receivedArray =
                    NfcIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            System.out.println("ndef discovered");
            if(receivedArray != null) {
                System.out.println("array != null");
                messagesReceivedArray.clear();
                NdefMessage receivedMessage = (NdefMessage) receivedArray[0];
                NdefRecord[] attachedRecords = receivedMessage.getRecords();

                for (NdefRecord record:attachedRecords) {
                    String string = new String(record.getPayload());
                    //Make sure we don't pass along our AAR (Android Application Record)
                    if (string.equals(getPackageName())) { continue; }
                    System.out.println(string);
                    messagesReceivedArray.add(string);
                }
            }
            else {
                System.out.println("ndef null");
                Toast.makeText(this, "Received Blank Parcel", Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public void onNewIntent(Intent intent) {
        handleNfcIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
        handleNfcIntent(getIntent());
    }

    @Override
    public void onPause(){
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    private Emitter.Listener transferGameMaster = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Scoreboard.updateGMIndex();
            Scoreboard.advanceNextQuestion();
            if(Scoreboard.getGMIndex() >= Scoreboard.getPlayers().size()){
                Scoreboard.setGMIndex(0);
            }
            if(Scoreboard.getPlayers().get(Scoreboard.getGMIndex()).getUserID() == ID) {
                Intent newGM = new Intent(PlayerScreen.this, GameMasterScreen.class);
                PlayerScreen.this.startActivity(newGM);
            }
        }
    };

    private Emitter.Listener listenForCorrect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Scoreboard.AddPoint(ID);
            if(Scoreboard.checkPoints(ID) == 5){
                pInput.setHint("YOU WON!");
                //TODO: ACTUAL WIN SCREEN
            }
        }
    };

    private Emitter.Listener gameStart = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            System.out.println("Game Start");
            gameTimer.start();
        }
    };

}
