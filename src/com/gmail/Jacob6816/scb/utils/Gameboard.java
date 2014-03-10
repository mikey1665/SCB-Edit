package com.gmail.Jacob6816.scb.utils;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.mcsg.double0negative.supercraftbros.Game;
import org.mcsg.double0negative.supercraftbros.classes.PlayerClass;
import org.mcsg.double0negative.supercraftbros.classes.PlayerClass.ClassType;

public class Gameboard {
    private final String lives = ChatColor.AQUA + "" + ChatColor.BOLD + "Lives" + ChatColor.RESET;
    private Game game;
    private Scoreboard board = null;
    private ClassType[] classes;
    
    public Gameboard(Game game) {
        this.game = game;
        classes = PlayerClass.ClassType.values();
        setup(true);
    }
    
    public void setup(boolean teams) {
        if (board == null) board = Bukkit.getScoreboardManager().getNewScoreboard();
        if (board.getObjective(ChatColor.stripColor(lives)) == null) board.registerNewObjective(ChatColor.stripColor(lives), "dummy");
        board.getObjective(ChatColor.stripColor(lives)).setDisplayName(lives);
        board.clearSlot(DisplaySlot.SIDEBAR);
        board.getObjective(ChatColor.stripColor(lives)).setDisplaySlot(DisplaySlot.SIDEBAR);
        for (ClassType t : classes) {
            String team = localeCaps(t.toString());
            if (board.getTeam(team) == null) board.registerNewTeam(team);
            board.getTeam(team).setAllowFriendlyFire(true);
            board.getTeam(team).setCanSeeFriendlyInvisibles(false);
            board.getTeam(team).setSuffix(ChatColor.RESET + "");
            board.getTeam(team).setPrefix(ChatColor.WHITE + "[" + team + "]" + ChatColor.RESET);
        }
        if (teams) reloadTeams();
        reloadLives();
        loadInPlayers();
    }
    
    private void loadInPlayers() {
        if (game.getActivePlayers().size() <= 0) return;
        for (Player p : game.getActivePlayers()) {
            p.setScoreboard(board);
        }
    }
    
    private void reloadTeams() {
        if (board.getTeams().size() <= 0) {
            setup(true);
            return;
        }
        for (Team t : board.getTeams()) {
            Iterator<OfflinePlayer> iter = t.getPlayers().iterator();
            while (iter.hasNext()) {
                t.getPlayers().remove(iter.next());
            }
        }
        if (game.getActivePlayers().size() == 0) return;
        for (Player p : game.getActivePlayers()) {
            PlayerClass c = game.getPlayerClass(p);
            if (c != null) board.getTeam(localeCaps(c.getType().toString())).addPlayer(p);
        }
    }
    
    private void reloadLives() {
        if (game.getActivePlayers().size() >= 1) {
            for (Player p : game.getActivePlayers()) {
                board.resetScores(p);
                board.getObjective(DisplaySlot.SIDEBAR).getScore(p).setScore(game.getPlayers().get(p));
            }
        }
    }
    
    private String localeCaps(String in) {
        if (in.length() <= 1) return in.toUpperCase();
        String t = in.toLowerCase();
        char c = t.charAt(0);
        String s = String.valueOf(c).toUpperCase();
        t.substring(1);
        return s + t;
    }
    
}