package io.quarkus.qson.test;

import java.util.List;
import java.util.Map;

public class Team {
    private String name;
    private Player quarterback;
    private List<Player> receivers;
    private Map<String, Player> players;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Player getQuarterback() {
        return quarterback;
    }

    public void setQuarterback(Player quarterback) {
        this.quarterback = quarterback;
    }

    public List<Player> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<Player> receivers) {
        this.receivers = receivers;
    }

    public Map<String, Player> getPlayers() {
        return players;
    }

    public void setPlayers(Map<String, Player> players) {
        this.players = players;
    }
}
