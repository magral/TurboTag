package com.example.leah.edg_touch;


import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;

public class launchActivity extends AppCompatActivity {
    CustomButton play;
    ImageView title, top, btm;
    CustomLayout bg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        //SET VIEWS
        title = (ImageView) findViewById(R.id.title);
        play = (CustomButton) findViewById(R.id.playButton);
        top = (ImageView) findViewById(R.id.topbrd);
        btm = (ImageView) findViewById(R.id.btmbrd);
        bg = (CustomLayout) findViewById(R.id.custom_layout);

        //LOAD ASSETS
        bg = (CustomLayout) findViewById(R.id.custom_layout);
        Picasso.with(this).load(R.drawable.top_border).fit().into(top);
        Picasso.with(this).load(R.drawable.play_button).into(play);
        Picasso.with(this).load(R.drawable.bottom_border).fit().into(btm);
        Picasso.with(this).load(R.drawable.titlecard).into(title);
        Picasso.with(this).load(R.drawable.title_bg).into(bg);

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Picasso.with(launchActivity.this).load(R.drawable.play_button_onpress).into(play);
                Intent playGame = new Intent(launchActivity.this, ChooseYourSide.class);
                launchActivity.this.startActivity(playGame);
            }
        });

    }




}
