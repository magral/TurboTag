package com.example.leah.edg_touch;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class QuestionChoice extends AppCompatActivity {
    CustomLayout bg;
    ImageView top, bot, banner;
    CustomButton questionpack1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_choice);
        bg = (CustomLayout) findViewById(R.id.qlayout);
        top = (ImageView) findViewById(R.id.qtop);
        bot = (ImageView) findViewById(R.id.qbot);
        banner =(ImageView) findViewById(R.id.choosePackBanner);
        questionpack1 = (CustomButton) findViewById(R.id.Questionp1);
        Picasso.with(this).load(R.drawable.questionpack_button).into(questionpack1);
        Picasso.with(this).load(R.drawable.title_bg).into(bg);
        Picasso.with(this).load(R.drawable.top_border).fit().into(top);
        Picasso.with(this).load(R.drawable.bottom_border).fit().into(bot);
        Picasso.with(this).load(R.drawable.choose_pack).fit().into(banner);
        questionpack1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startGame = new Intent(QuestionChoice.this, GameMasterScreen.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                Scoreboard.setQuestionNumber(1);
                QuestionChoice.this.startActivity(startGame);
            }
        });
    }
}
