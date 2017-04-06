package com.example.leah.edg_touch;


public class PlayerDefinition {
    private int userID;
    private int points;
    private String answer;

    public PlayerDefinition(int uid){
        userID = uid;
        points = 0;
        answer = "";
    }

    public int getUserID(){
        return userID;
    }

    public void setAnswer( String ans){
        answer += (ans + ",");
    }

    public String getAnswer(){
        return answer;
    }

    public void addPoint(){
        points++;
    }

    public int getPoints(){
        return points;
    }


}
