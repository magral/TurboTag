package com.example.leah.edg_touch;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ChooseYourSide extends AppCompatActivity {
    CustomLayout bg;
    ImageView tp, bt, banner;
    CustomButton create, join;
    ConnectionDefinition connectionClass;
    Intent startNewPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_your_side);
        /*if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            Intent NfcIntent = getIntent();
            Parcelable[] receivedArray = NfcIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (receivedArray != null) {
                NdefMessage receivedMessage = (NdefMessage) receivedArray[0];
                NdefRecord[] attachedRecords = receivedMessage.getRecords();

                for (NdefRecord record : attachedRecords) {
                    String string = new String(record.getPayload());
                    //Make sure we don't pass along our AAR (Android Application Record)
                    if (string.equals(getPackageName())) {
                        continue;
                    }
                }
            } else {
                Toast.makeText(this, "Received Blank Parcel", Toast.LENGTH_LONG).show();
            }
            finish();
            return;
        }*/

        bg = (CustomLayout) findViewById(R.id.custom_layout_CYS);
        tp = (ImageView) findViewById(R.id.tp);
        bt = (ImageView) findViewById(R.id.bt);
        banner = (ImageView) findViewById(R.id.choosesidebanner);
        create = (CustomButton) findViewById(R.id.CreateRoom);
        join = (CustomButton) findViewById(R.id.JoinRoom);


        Picasso.with(this).load(R.drawable.title_bg).into(bg);
        Picasso.with(this).load(R.drawable.top_border).fit().into(tp);
        Picasso.with(this).load(R.drawable.bottom_border).fit().into(bt);
        Picasso.with(this).load(R.drawable.chooseaside).fit().into(banner);
        Picasso.with(this).load(R.drawable.questionpack_button).into(create);
        Picasso.with(this).load(R.drawable.questionpack_button).into(join);

        connectionClass = new ConnectionDefinition();

        final Integer roomID = 1234;

        //Player creates a room
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EraseOldMembers eraseOldMembers = new EraseOldMembers();
                eraseOldMembers.execute();
                RegisterNewGameMaster register = new RegisterNewGameMaster();
                register.execute(roomID);
            }
        });

        //Player joins a room
        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterNewPlayer register = new RegisterNewPlayer();
                register.execute(roomID);
            }
        });

    }

    //Registers a new player to the database
    public class RegisterNewPlayer extends AsyncTask<Integer, Integer, Integer>{
        String msg;
        @Override
        protected Integer doInBackground(Integer... params){
            int id = 0;
            try{
                Connection con = connectionClass.CONN();
                if(con == null){
                    msg = "Error connecting to SQL server";
                }
                else {
                    String query = "INSERT INTO Users (RoomNumber, Score) VALUES (" + params[0]+ "," + 0 + ")";
                    Statement stm = con.createStatement();
                    stm.executeUpdate(query);
                    query = "SELECT TOP 1 * FROM Users ORDER BY ID DESC";
                    Statement state = con.createStatement();
                    ResultSet rs = state.executeQuery(query);
                    while(rs.next()){
                        id = rs.getInt("ID");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return id;
        }

        @Override
        protected void onPostExecute(Integer i){
            startNewPlayer = new Intent(ChooseYourSide.this, PlayerScreen.class);
            startNewPlayer.putExtra("ID",i);
            ChooseYourSide.this.startActivity(startNewPlayer);
        }
    }

    public class RegisterNewGameMaster extends AsyncTask<Integer, Integer, Integer>{
        String msg;
        @Override
        protected Integer doInBackground(Integer... params){
            int id = 0;
            try{
                Connection con = connectionClass.CONN();
                if(con == null){
                    msg = "Error connecting to SQL server";
                }
                else {
                    String query = "INSERT INTO Users (RoomNumber, Score) VALUES (" + params[0]+ "," + 0 + ")";
                    Statement stm = con.createStatement();
                    stm.executeUpdate(query);
                    query = "SELECT TOP 1 * FROM Users ORDER BY ID DESC";
                    Statement state = con.createStatement();
                    ResultSet rs = state.executeQuery(query);
                    while(rs.next()){
                        id = rs.getInt("ID");
                        Scoreboard.setLocalID(id);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return id;
        }

        @Override
        protected void onPostExecute(Integer i){
            startNewPlayer = new Intent(ChooseYourSide.this, GameMasterScreen.class);
            ChooseYourSide.this.startActivity(startNewPlayer);
        }
    }

    public class EraseOldMembers extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params){
            try{
                Connection con = connectionClass.CONN();
                if(con == null){

                }
                else {
                    String query = "Truncate table Users";
                    Statement stm = con.createStatement();
                    stm.executeUpdate(query);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
