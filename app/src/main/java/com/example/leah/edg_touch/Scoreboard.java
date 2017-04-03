package com.example.leah.edg_touch;


public final class Scoreboard {
    private static int redTeamPoints;
    private static int blueTeamPoints;
    private static int questionNumber;
    private static int answerNumber;
    private static String currentAnswer;

    private Scoreboard(){
        redTeamPoints = 0;
        blueTeamPoints = 0;
        questionNumber = 1;
        answerNumber = 1;
        currentAnswer = "";
    }

    public static void addRedTeamPoint(int point){
        redTeamPoints += point;
    }

    public static void addBlueTeamPoint(int point){
        blueTeamPoints += point;
    }

    public static int getRedTeamPoints(){
        return redTeamPoints;
    }

    public static int getBlueTeamPoints(){
        return blueTeamPoints;
    }

    public static int determineWinner(){
        if(redTeamPoints > blueTeamPoints){
            return 0;
        }
        else if(blueTeamPoints > redTeamPoints){
            return 1;
        }
        else {
            return 2;
        }
    }

    public static void setQuestionNumber(int qn){
        questionNumber = qn;
    }

    public static int getQuestionNumber(){
        return questionNumber;
    }

    public static void setCurrentAnswer(String a){
        currentAnswer = a;
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
}
