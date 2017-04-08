package com.example.leah.edg_touch;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PlayerStartScreen extends AppCompatActivity {
    ConnectionDefinition connectionClass;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_start_screen);
        connectionClass = new ConnectionDefinition();
        CustomLayout bg = (CustomLayout) findViewById(R.id.content_player_start_screen);
        ImageView tp = (ImageView) findViewById(R.id.sp_top_border);
        ImageView bt = (ImageView) findViewById(R.id.sp_bottom_border);
        ImageView qbg = (ImageView) findViewById(R.id.sp_bg);
        final EditText RoomNumber = (EditText) findViewById(R.id.playerRoomNumber);
        CustomButton startGame = (CustomButton) findViewById(R.id.joinGame);

        Picasso.with(this).load(R.drawable.beam_send_button).into(startGame);
        Picasso.with(this).load(R.drawable.question_answer_bg).fit().into(qbg);
        Picasso.with(this).load(R.drawable.top_border).fit().into(tp);
        Picasso.with(this).load(R.drawable.bottom_border).fit().into(bt);
        Picasso.with(this).load(R.drawable.title_bg).into(bg);

        startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterNewPlayer rp = new RegisterNewPlayer();
                rp.execute(Integer.parseInt(RoomNumber.getText().toString()));
            }
        });
    }

    //Registers a new player to the database
    public class RegisterNewPlayer extends AsyncTask<Integer, Integer, Integer> {
        @Override
        protected Integer doInBackground(Integer... params){
            int id = 0;
            try{
                Connection con = connectionClass.CONN();
                if(con == null){
                    //msg = "Error connecting to SQL server";
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
            Intent start = new Intent(PlayerStartScreen.this, PlayerScreen.class);
            PlayerStartScreen.this.startActivity(start);
        }
    }
}
