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
    CustomButton gameMasterButt, redTeamButt, blueTeamButt;
    ConnectionDefinition connectionClass;
    Intent startNewPlayer;
    int iD;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_your_side);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            System.out.println("On New Intent Called");
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
                    System.out.println(string);
                }
            } else {
                System.out.println("ndef null");
                Toast.makeText(this, "Received Blank Parcel", Toast.LENGTH_LONG).show();
            }
            finish();
            return;
        }

        bg = (CustomLayout) findViewById(R.id.custom_layout_CYS);
        tp = (ImageView) findViewById(R.id.tp);
        bt = (ImageView) findViewById(R.id.bt);
        banner = (ImageView) findViewById(R.id.choosesidebanner);
        gameMasterButt = (CustomButton) findViewById(R.id.GameMaster);
        redTeamButt = (CustomButton) findViewById(R.id.RedTeam);
        blueTeamButt = (CustomButton) findViewById(R.id.BlueTeam);

        Picasso.with(this).load(R.drawable.title_bg).into(bg);
        Picasso.with(this).load(R.drawable.top_border).fit().into(tp);
        Picasso.with(this).load(R.drawable.bottom_border).fit().into(bt);
        Picasso.with(this).load(R.drawable.chooseaside).fit().into(banner);
        Picasso.with(this).load(R.drawable.questionpack_button).into(gameMasterButt);
        Picasso.with(this).load(R.drawable.questionpack_button).into(redTeamButt);
        Picasso.with(this).load(R.drawable.questionpack_button).into(blueTeamButt);

        connectionClass = new ConnectionDefinition();

        final Integer redTeamID = 1;
        final Integer blueTeamID = 2;

        //Create a game master
        gameMasterButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EraseOldMembers eraseOldMembers = new EraseOldMembers();
                eraseOldMembers.execute();
                Intent newGame = new Intent(ChooseYourSide.this, QuestionChoice.class);
                ChooseYourSide.this.startActivity(newGame);
            }
        });

        //Create a red player
        redTeamButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterNew register = new RegisterNew();
                register.execute(redTeamID);
            }
        });

        //Create a blue player
        blueTeamButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent blueAct = new Intent(ChooseYourSide.this, BlueAnswerScreen.class);
                RegisterNew reg = new RegisterNew();
                reg.execute(blueTeamID);
                ChooseYourSide.this.startActivity(blueAct);
            }
        });
    }

    //Registers a new player to the database
    public class RegisterNew extends AsyncTask<Integer, Integer, Integer>{
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
                    String query = "INSERT INTO Users (TeamID) VALUES (" + params[0]+ ")";
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
            startNewPlayer = new Intent(ChooseYourSide.this, RedAnswerScreen.class);
            startNewPlayer.putExtra("ID",i);
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
