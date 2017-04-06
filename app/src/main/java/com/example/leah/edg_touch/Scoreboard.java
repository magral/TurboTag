package com.example.leah.edg_touch;


import java.util.ArrayList;

public final class Scoreboard {
    private static ArrayList<PlayerDefinition> players;
    private static int questionNumber;
    private static String currentAnswer;

    private Scoreboard(){
        questionNumber = 1;
        currentAnswer = "";
        players = new ArrayList<>();
    }

    public static void addPlayer(PlayerDefinition newPlayer){
        players.add(newPlayer);
    }

    public static PlayerDefinition determineWinner(){
        PlayerDefinition winner = players.get(0);
        for(int i = 0; i < players.size() - 1; i++) {
            if (players.get(i + 1).getPoints() > players.get(i).getPoints()) {
                winner = players.get(i + 1);
            }
        }
        return winner;
    }

    public static void setQuestionNumber(int qn){
        questionNumber = qn;
    }

    public static int getQuestionNumber(){
        return questionNumber;
    }

    public static int advanceNextQuestion(){
        questionNumber++;
        return questionNumber;
    }
    public static String getCurrentAnswer(){
        return currentAnswer;
    }
    public static int compareAnswers(String ans){
        if(ans.equalsIgnoreCase(currentAnswer)){
            return 0;
        }
        else{
            return -1;
        }
    }

    public static void clearPlayers(){
        players.clear();
    }

    public static void AddPoint(int id){
        for(int i = 0; i < players.size(); i++){
            if(players.get(i).getUserID() == id){
                players.get(i).addPoint();
            }
        }
    }
}
