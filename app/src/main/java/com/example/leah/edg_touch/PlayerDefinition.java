package com.example.leah.edg_touch;


public class PlayerDefinition {
    private int userID;
    private int teamID;
    private String answer;

    public PlayerDefinition(int uid, int tID){
        userID = uid;
        teamID = tID;
        answer = "";
    }

    public PlayerDefinition(int uid, int tID, String ans){
        userID = uid;
        teamID = tID;
        answer = ans;
    }

    public int getUserID(){
        return userID;
    }

    public int getTeamID(){
        return teamID;
    }

    public void setAnswer( String ans){
        answer += (ans + ",");
    }

    public String getAnswer(){
        return answer;
    }


}
