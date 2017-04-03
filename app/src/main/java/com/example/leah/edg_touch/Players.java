package com.example.leah.edg_touch;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Players {
    private ArrayList<PlayerDefinition> players;
    private ArrayList<PlayerDefinition> redTeam;
    private ArrayList<PlayerDefinition> blueTeam;

    public Players() {
        players = new ArrayList<PlayerDefinition>();
        blueTeam = new ArrayList<PlayerDefinition>();
        redTeam = new ArrayList<PlayerDefinition>();
    }

    public ArrayList<PlayerDefinition> getPlayers() {
        return players;
    }

    public void addPlayer(PlayerDefinition p){
        players.add(p);
    }
    public void addRedPlayer(PlayerDefinition p) {redTeam.add(p);}
    public void addBluePlayer(PlayerDefinition p) {blueTeam.add(p);}

    public ArrayList<PlayerDefinition> getRedTeam(){
        return redTeam;
    }

    public ArrayList<PlayerDefinition> getBlueTeam(){
        return blueTeam;
    }
    public void clear(){
        players.clear();
    }
}
