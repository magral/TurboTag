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
import android.os.Debug;
import android.os.Parcelable;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
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
    final private int MAX_QUESTIONS_IN_PACK = 10;
    NfcAdapter nfcAdapter;

    private ArrayList<String> answersReceived = new ArrayList<String>();
    private TextView answersReceivedSpace;
    private String[][] techListsArray;

    PendingIntent pendingIntent;
    IntentFilter[] intentFiltersArray;

    TextView questionSpace;
    CustomButton getQuestion;
    CustomLayout bg;
    ImageView tp, bt, qbg;
    ConnectionDefinition connectionDefinition;

    ArrayList<String> answers;
    Players players;
    private String submittedSide;
    private Socket socket;
    private InetAddress ip;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        {
            try {
                ip = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        {
            try {
                //socket = IO.socket("http://192.168.1.87:9001");
                //System.out.println(());
                socket = IO.socket("http://192.168.43.82:443");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        setContentView(R.layout.activity_game_master_screen);
        //Load Images
        bg = (CustomLayout) findViewById(R.id.q_bg);
        tp = (ImageView) findViewById(R.id.tpb);
        bt = (ImageView) findViewById(R.id.btb);
        qbg = (ImageView) findViewById(R.id.qspaceBg);
        Picasso.with(this).load(R.drawable.question_answer_bg).fit().into(qbg);
        Picasso.with(this).load(R.drawable.top_border).fit().into(tp);
        Picasso.with(this).load(R.drawable.bottom_border).fit().into(bt);
        Picasso.with(this).load(R.drawable.title_bg).into(bg);

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
        //
        //Socketing
        answers = new ArrayList<String>();
        players = new Players();
        System.out.println(socket);
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args){
                socket.emit("start game", socket);
            }
        });

        socket.on("get question", onGetQuestion);
        socket.on("red side sent", onRedSideReceived);
        socket.on("blue side sent", onBlueSideReceived);
        socket.connect();
        connectionDefinition = new ConnectionDefinition();
        questionSpace = (TextView) findViewById(R.id.questionSpace);
        getQuestion = (CustomButton) findViewById(R.id.getQuestion);
        Picasso.with(this).load(R.drawable.beam_send_button).into(getQuestion);
        getQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer packnum = Scoreboard.getQuestionNumber();
                if(Scoreboard.getQuestionNumber() <= MAX_QUESTIONS_IN_PACK) {
                    GetQuestions get = new GetQuestions();
                    get.execute(packnum);

                }
                else{
                    socket.off("get question");
                    socket.off("red side sent");
                    socket.off("blue side sent");
                    socket.disconnect();
                    int winner = Scoreboard.determineWinner();
                    if(winner == 0){
                        //TODO: Something with winnning side
                        questionSpace.setText("RED SIDE WINNER");
                    }
                    else if(winner == 1){
                        //TODO: Something with winning side
                        questionSpace.setText("BLUE SIDE WINNER");
                    }
                    else{
                        //TODO: Something on draw
                        questionSpace.setText("DRAW....?");
                    }
                }
            }
        });
    }
    /*
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    System.out.println("ip1--:" + inetAddress);
                    System.out.println("ip2--:" + inetAddress.getHostAddress());

                    // for getting IPV4 format
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {

                        String ip = inetAddress.getHostAddress().toString();
                        System.out.println("ip---::" + ip);
                        // return inetAddress.getHostAddress().toString();
                        return ip;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
*/
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
                    String string = new String(record.getPayload());
                    //checking to make sure the package passed through refers to our application
                    if (string.equals(getPackageName())) { continue; }
                    answersReceivedSpace.setText(string);
                    answersReceived.add(string);
                    if(Scoreboard.compareAnswers(string) == 0){
                        if(submittedSide == "RED"){
                            Scoreboard.addRedTeamPoint(1);
                            questionSpace.setText("Correct Answer!");
                        }
                        else{
                            Scoreboard.addBlueTeamPoint(1);
                            questionSpace.setText("Correct Answer!");
                        }
                    }

                    System.out.println("Red: " + Scoreboard.getRedTeamPoints() + " Blue: " + Scoreboard.getBlueTeamPoints());
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

    public void AssignAnswers(Players players, ArrayList<String> answers){
        ArrayList<PlayerDefinition> redTeam = players.getPlayers();
        ArrayList<PlayerDefinition> blueTeam = players.getPlayers();
        Random randomGenerator = new Random();
        int win;
        ArrayList<String> answerList = new ArrayList<String>();
        answerList.add(Scoreboard.getCurrentAnswer());
        answerList.add(answers.get(0));
        answerList.add(answers.get(1));
        answerList.add(answers.get(2));
        System.out.println("PRINTING ANSWER LIST:::::: " + answerList);
        //answerList.add(answers.get(3));
        //Assign Red team answers
        if(redTeam.size() == 0) {
            Toast.makeText(this, "Too little players to start a game", Toast.LENGTH_LONG).show();
        }
        else{
            int index = 0;
            for(int i = 0; i < answerList.size(); i++){
                if(index >= redTeam.size()){
                    index = 0;
                }
                System.out.println("CURRENT PLAYER ANSWER:::::::: " + redTeam.get(index).getAnswer());
                redTeam.get(index).setAnswer(answerList.get(i));
            }
            /*
            index = 0;
            for(Iterator<String> i = answerList.iterator(); i.hasNext(); ){
                if(index < blueTeam.size()){
                    index = 0;
                }
                blueTeam.get(index).setAnswer(i.next());
            }*/
        }
        /*
        switch(redTeam.size()){
            case 0:
                System.out.println("Too little players to start a game");
            case 1:
                redTeam.get(0).setAnswer(Scoreboard.getCurrentAnswer());
                break;
            case 2:
                win = randomGenerator.nextInt(redTeam.size());
                if(win == 0){
                    redTeam.get(0).setAnswer(Scoreboard.getCurrentAnswer());
                    redTeam.get(1).setAnswer(answers.get(0));
                }
                else{
                    redTeam.get(1).setAnswer(Scoreboard.getCurrentAnswer());
                    redTeam.get(0).setAnswer(answers.get(0));
                }
                break;
            case 3:
                win = randomGenerator.nextInt(redTeam.size());
                switch (win){
                    case 0:
                        redTeam.get(0).setAnswer(Scoreboard.getCurrentAnswer());
                        redTeam.get(1).setAnswer(answers.get(0));
                        redTeam.get(2).setAnswer(answers.get(1));
                        break;
                    case 1:
                        redTeam.get(0).setAnswer(answers.get(0));
                        redTeam.get(1).setAnswer(Scoreboard.getCurrentAnswer());
                        redTeam.get(2).setAnswer(answers.get(1));
                        break;
                    case 2:
                        redTeam.get(0).setAnswer(answers.get(0));
                        redTeam.get(1).setAnswer(answers.get(1));
                        redTeam.get(2).setAnswer(Scoreboard.getCurrentAnswer());

                }
                break;
            case 4:
                win = randomGenerator.nextInt(redTeam.size());
                redTeam.get(win).setAnswer(Scoreboard.getCurrentAnswer());
                redTeam.remove(win);
                for(int i = 0; i < redTeam.size(); i++){
                    redTeam.get(i).setAnswer(answers.get(i));
                }
                break;
            default:
                System.out.println("Something caused a default in red team switch");
        }
        switch(blueTeam.size()) {
            case 0:
                throw new Error("Too little players to start a game: 0");
            case 1:
                blueTeam.get(0).setAnswer(Scoreboard.getCurrentAnswer());
                break;
            case 2:
                win = randomGenerator.nextInt(redTeam.size());
                if (win == 0) {
                    blueTeam.get(0).setAnswer(Scoreboard.getCurrentAnswer());
                    blueTeam.get(1).setAnswer(answers.get(0));
                } else {
                    blueTeam.get(1).setAnswer(Scoreboard.getCurrentAnswer());
                    blueTeam.get(0).setAnswer(answers.get(0));
                }
                break;
            case 3:
                win = randomGenerator.nextInt(redTeam.size());
                switch (win) {
                    case 0:
                        blueTeam.get(0).setAnswer(Scoreboard.getCurrentAnswer());
                        blueTeam.get(1).setAnswer(answers.get(0));
                        blueTeam.get(2).setAnswer(answers.get(1));
                        break;
                    case 1:
                        blueTeam.get(0).setAnswer(answers.get(0));
                        blueTeam.get(1).setAnswer(Scoreboard.getCurrentAnswer());
                        blueTeam.get(2).setAnswer(answers.get(1));
                        break;
                    case 2:
                        blueTeam.get(0).setAnswer(answers.get(0));
                        blueTeam.get(1).setAnswer(answers.get(1));
                        blueTeam.get(2).setAnswer(Scoreboard.getCurrentAnswer());

                }
                break;
            case 4:
                win = randomGenerator.nextInt(redTeam.size());
                blueTeam.get(win).setAnswer(Scoreboard.getCurrentAnswer());
                blueTeam.remove(win);
                for (int i = 0; i < blueTeam.size(); i++) {
                    blueTeam.get(i).setAnswer(answers.get(i));
                }
                break;
            default:
                System.out.println("Something caused a default in red team switch");
        }*/
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
                    String query = "SELECT Question FROM Questions Where AnswerID = " + params[0];
                    Statement stm = con.createStatement();
                    ResultSet rs = stm.executeQuery(query);
                    while(rs.next()){
                        q = rs.getString("Question");
                    }
                    Scoreboard.advanceNextQuestion();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return q;
        }

        @Override
        protected void onPostExecute(String m){
            questionSpace.setText(m);
            GetUsers getUsers = new GetUsers();
            getUsers.execute(n);
        }
    }

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
                    String query = "SELECT ID, TeamID FROM Users";
                    Statement statement = con.createStatement();
                    ResultSet rs = statement.executeQuery(query);

                    while(rs.next()){
                        int uid = rs.getInt("ID");
                        int tid = rs.getInt("TeamID");
                        PlayerDefinition p = new PlayerDefinition(uid, tid);
                        if(tid == 1){
                            players.addRedPlayer(p);
                        }
                        else{
                            players.addBluePlayer(p);
                        }
                        players.addPlayer(p);
                    }
                }
            } catch (SQLException e){
                e.printStackTrace();
            }
            return Answerid;
        }

        @Override
        protected void onPostExecute(Integer p){
            GetAnswers getAnswers = new GetAnswers();
            getAnswers.execute(p);
        }
    }

    public class GetAnswers extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... params){
            try {
                Connection conn = connectionDefinition.CONN();
                if( conn == null){
                    throw new Error("SQL Connection error");
                }
                else {
                    String query = "SELECT Answer, Correct From Answers where AnswerID = " + params[0];
                    Statement statement = conn.createStatement();
                    ResultSet rs = statement.executeQuery(query);
                    while(rs.next()){
                        if(rs.getInt("Correct") == 1){
                            Scoreboard.setCurrentAnswer(rs.getString("Answer"));
                        }
                        else{
                            answers.add(rs.getString("Answer"));
                        }
                    }
                }
            } catch (SQLException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void none){
            AssignAnswers(players, answers);
            FillAnswerData fillData = new FillAnswerData();
            fillData.execute(players.getPlayers());
        }
    }

    public class FillAnswerData extends AsyncTask<ArrayList<PlayerDefinition>, String, Void>{
        @Override
        protected Void doInBackground(ArrayList<PlayerDefinition>... params){
            try{
                Connection conn = connectionDefinition.CONN();
                if( conn == null){
                    throw new Error("SQL Connection error");
                }
                else {
                    System.out.println("size params" + params[0].size());
                    for (int i = 0; i < params[0].size(); i++) {
                        System.out.println("Fill answer " + params[0].get(i).getAnswer());
                        String query = "UPDATE Users SET AnswerData = isnull(AnswerData, '') + ('" + params[0].get(i).getAnswer() + "') WHERE ID = " + params[0].get(i).getUserID();
                        //String query = "INSERT INTO Users (ID, TeamID, AnswerData) VALUES ('" + params[0].get(i).getUserID() + "', '" + params[0].get(i).getTeamID() + "', '" + params[0].get(i).getAnswer() + "');";
                        System.out.println(query);
                        Statement stm = conn.createStatement();
                        stm.executeUpdate(query);
                    }
                    answers.clear();
                    players.clear();
                    socket.emit("activate player", true);
                }
            } catch (SQLException e){
                e.printStackTrace();
            }
            return null;
        }
    }
    private Emitter.Listener onGetQuestion = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject obj = (JSONObject) args[0];
            boolean receive;
            try {
                receive = obj.getBoolean("success");
                System.out.println(receive);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private Emitter.Listener onRedSideReceived = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            submittedSide = "RED";
        }
    };

    private Emitter.Listener onBlueSideReceived = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            submittedSide = "BLUE";
        }
    };

}
