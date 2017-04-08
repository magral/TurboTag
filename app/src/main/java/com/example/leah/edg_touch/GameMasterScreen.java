package com.example.leah.edg_touch;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Debug;
import android.os.Parcelable;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Random;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class GameMasterScreen extends AppCompatActivity {
    NfcAdapter nfcAdapter;

    private ArrayList<String> answersReceived = new ArrayList<>();
    private TextView answersReceivedSpace;
    private String[][] techListsArray;

    PendingIntent pendingIntent;
    IntentFilter[] intentFiltersArray;

    TextView questionSpace, timer;
    CustomButton nextRound;
    CustomLayout bg;
    ImageView tp, bt, qbg;
    ConnectionDefinition connectionDefinition;

    ArrayList<String> answers;
    private Socket socket;
    int id;

    CountDownTimer gameTimer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        System.out.println("On Create Called");
        id = Scoreboard.getLocalID();
        connectionDefinition = new ConnectionDefinition();
        //Populate user list if no user list exists
        if(Scoreboard.isPlayersEmpty()){
            GetUsers gu = new GetUsers();
            gu.execute();
        }
        GetQuestions gq = new GetQuestions();
        gq.execute(Scoreboard.getQuestionNumber());


        if(Build.VERSION.SDK_INT >= 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        {
            try {
                //socket = IO.socket("http://192.168.1.87:9001");
                //System.out.println(());
                socket = IO.socket("http://192.168.25.117:443");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        setContentView(R.layout.activity_game_master_screen);
        //Load Images
        timer = (TextView) findViewById(R.id.masterTimer);
        bg = (CustomLayout) findViewById(R.id.q_bg);
        tp = (ImageView) findViewById(R.id.tpb);
        bt = (ImageView) findViewById(R.id.btb);
        qbg = (ImageView) findViewById(R.id.qspaceBg);
        questionSpace = (TextView) findViewById(R.id.questionSpace);
        //TODO: COMBINE THIS LISTENER WITH GET QUESTION
        nextRound = (CustomButton) findViewById(R.id.NextRound);
        Picasso.with(this).load(R.drawable.beam_send_button).into(nextRound);
        Picasso.with(this).load(R.drawable.question_answer_bg).fit().into(qbg);
        Picasso.with(this).load(R.drawable.top_border).fit().into(tp);
        Picasso.with(this).load(R.drawable.bottom_border).fit().into(bt);
        Picasso.with(this).load(R.drawable.title_bg).into(bg);
        Picasso.with(this).load(R.drawable.beam_send_button);

        //NFC
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
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

        answersReceivedSpace = (TextView) findViewById(R.id.answersReceivedSpace);
        updateTextViews();

        //Socketing
        answers = new ArrayList<>();
        /*socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args){
                socket.emit("start game", socket);
            }
        });*/

        gameTimer = new CountDownTimer(15000, 1000) {

            public void onTick(long millisUntilFinished) {
                timer.setText(" " + (millisUntilFinished / 1000));
            }

            public void onFinish() {
                timer.setText("Round Over!");
                nextRound.setOnClickListener(nextRoundListener(nextRound));
            }
        };
        socket.connect();
        socket.emit("start game");
    }
    //update text views
    private  void updateTextViews() {
        answersReceivedSpace.setText("Answer Received:\n");
        //Populate our list of messages we have received
        if (answersReceived.size() > 0) {
            for (int i = 0; i < answersReceived.size(); i++) {
                answersReceivedSpace.append(answersReceived.get(i));
                answersReceivedSpace.append("\n");
            }
        }
    }

    //Save instances of our received messages in case the player leaves the app
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putStringArrayList("lastMessagesReceived",answersReceived);
    }

    //Load saved instances
    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        answersReceived = savedInstanceState.getStringArrayList("lastMessagesReceived");
    }

    //Handle the intent that comes in through nfc
    private void handleNfcIntent(Intent NfcIntent) {
        //Check if there's a flag for NFC action and get the attached parcel
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(NfcIntent.getAction())) {
            Parcelable[] receivedArray =
                    NfcIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            //Check to make sure parcel isn't null
            if(receivedArray != null) {
                answersReceived.clear();
                NdefMessage receivedMessage = (NdefMessage) receivedArray[0];
                NdefRecord[] attachedRecords = receivedMessage.getRecords();

                //Add each sent message to an array
                for (NdefRecord record:attachedRecords) {
                    String playerAnswer = new String(record.getPayload());
                    //checking to make sure the package passed through refers to our application
                    if (playerAnswer.equals(getPackageName())) { continue; }
                    answersReceivedSpace.setText(playerAnswer);
                    answersReceived.add(playerAnswer);
                    if(Scoreboard.compareAnswers(playerAnswer) == 0){
                        if(Scoreboard.checkPoints(id) == 5){
                            questionSpace.setText("YOU WON!");
                        }
                        else {
                            Scoreboard.AddPoint(id);
                            socket.emit("alert player correct");
                            Scoreboard.advanceNextQuestion();
                        }
                    }
                }
                Toast.makeText(this, "Received " + answersReceived.size() +
                        " Messages", Toast.LENGTH_LONG).show();
                updateTextViews();
            }
            else {
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
        updateTextViews();
        handleNfcIntent(getIntent());
    }

    @Override
    public void onPause(){
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    public class GetQuestions extends AsyncTask<Integer, String, String> {
        String q;
        Integer n;
        @Override
        protected String doInBackground(Integer... params){
            n = params[0];
            try{
                Connection con = connectionDefinition.CONN();
                if(con == null){
                    q = "Error connecting to SQL server";
                }
                else {
                    String query = "SELECT Question FROM Questions Where QuestionNumber = " + params[0];
                    Statement stm = con.createStatement();
                    ResultSet rs = stm.executeQuery(query);
                    gameTimer.start();
                    while(rs.next()){
                        q = rs.getString("Question");
                        System.out.println("RECEIVED QUESTION:" + q);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return q;
        }

        @Override
        protected void onPostExecute(String m){
            questionSpace.setText(m);
            Scoreboard.setCurrentAnswer(m);
        }
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


    private void StartNextRound(){
        if(Scoreboard.checkPoints(id) == 5) {
            socket.off("get question");
            socket.disconnect();
        }
        else {
            Scoreboard.advanceNextQuestion();
            Scoreboard.updateGMIndex();
            Intent newPlayer = new Intent(GameMasterScreen.this, PlayerScreen.class);
            socket.emit("transfer game master");
            GameMasterScreen.this.startActivity(newPlayer);
        }
    }

    //Onclick to start next round
    //In future this should be disabled unless round is over
    View.OnClickListener nextRoundListener(final Button button)  {
        return new View.OnClickListener() {
            public void onClick(View v) {
                StartNextRound();
            }
        };
    }
}
