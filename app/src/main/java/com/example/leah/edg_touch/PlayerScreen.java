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
    CustomLayout bg;
    CustomButton beam;
    ImageView top, bt, ansbg;
    EditText pInput;
    //View spaces
    View rootView;
    LinearLayout answerLayout;
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

        {
            try {
                //socket = IO.socket("http://192.168.1.87:9001");
                //System.out.println(());
                socket = IO.socket("http://192.168.43.82:443");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        if(Scoreboard.isPlayersEmpty()){
            GetUsers gu = new GetUsers();
            gu.execute();
        }
        setContentView(R.layout.activity_red_answer_screen);
        answerLayout = (LinearLayout) findViewById(R.id.buttonLayoutRed);
        //Load Images
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
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args){
                System.out.println("Player connected");
                //socket.emit("start game", socket);
            }
        });

        socket.on("transferGameMAster", transferGameMaster);
        connectionDefinition = new ConnectionDefinition();
        response = (TextView) findViewById(R.id.answerRed);
        //socket.on("get question", onGetQuestion );
        socket.connect();
        beam.setOnClickListener(customOnClickListener(beam, pInput.getText().toString()));
    }

    /*private Emitter.Listener onGetQuestion = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try{
                Connection con = connectionDefinition.CONN();
                if(con == null){
                    throw new Error("SQL Connection Error");
                }
                else{
                    String query = "SELECT AnswerData FROM Users Where ID = "  + ID ;
                    Statement stm = con.createStatement();
                    ResultSet rs = stm.executeQuery(query);
                    while(rs.next()){
                        final String ans = rs.getString("AnswerData");
                        final ArrayList<String> parsedAnswers = new ArrayList<String>(Arrays.asList(ans.split(",")));
                        System.out.println(ans);
                        PlayerScreen.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("SIZE OF PARSED ARRAY::::::::::::" + parsedAnswers.size());
                                answerLayout.removeAllViews();
                                for(int i = 0; i < parsedAnswers.size(); i++ ) {
                                    CustomButton a = new CustomButton(getApplicationContext());
                                    System.out.println("PARSED ANSWERS LIST::::: " + parsedAnswers);
                                    a.setOnClickListener(customOnClickListener(a, parsedAnswers.get(i) ));
                                    a.setText(parsedAnswers.get(i));
                                    ActionBar.LayoutParams lp = new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, android.app.ActionBar.LayoutParams.WRAP_CONTENT);
                                    Picasso.with(getApplicationContext()).load(R.drawable.beam_send_button).into(a);
                                    answerLayout.addView(a, lp);
                                }
                                // response.setText(ans);
                            }
                        });
                    }
                }
            } catch( SQLException e){
                e.printStackTrace();
            }
        }
    };
*/
    public class GetUsers extends AsyncTask<Integer, Integer, Integer> {
        @Override
        protected Integer doInBackground(Integer... params){
            Integer Answerid = params[0];
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
            return Answerid;
        }

        @Override
        protected void onPostExecute(Integer p){

        }
    }
    View.OnClickListener customOnClickListener(final Button button, final String message)  {
        return new View.OnClickListener() {
            public void onClick(View v) {
                addMessage(v, message);
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
        //socket.emit("player sent", true);
        NdefRecord[] recordsToAttach = createRecords();
        return new NdefMessage(recordsToAttach);
    }

    public NdefRecord[] createRecords(){
        System.out.println("Create Records");
        NdefRecord[] records = new NdefRecord[messagesToSendArray.size() + 1];
        for(int i = 0; i < messagesToSendArray.size(); i++){
            System.out.println(messagesToSendArray.get(i));
            String sendMessage = messagesToSendArray.get(i) + ", " + ID;
            byte[] payload = sendMessage.
                    getBytes(Charset.forName("UTF-8"));

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
            if(Scoreboard.getPlayers().get(Scoreboard.getGMIndex()).getUserID() == ID) {
                Scoreboard.updateGMIndex();
                Scoreboard.advanceNextQuestion();
                Intent newGM = new Intent(PlayerScreen.this, GameMasterScreen.class);
                PlayerScreen.this.startActivity(newGM);
            }
        }
    };

}
