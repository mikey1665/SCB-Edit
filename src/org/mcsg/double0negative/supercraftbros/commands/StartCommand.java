package org.mcsg.double0negative.supercraftbros.commands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.mcsg.double0negative.supercraftbros.GameManager;

public class StartCommand implements SubCommand{

	@Override
	public boolean onCommand(Player player, String[] args) {
		if(player.hasPermission("scb.start")){
			int game = GameManager.getInstance().getPlayerGameId(player);
			if(game != -1){
				GameManager.getInstance().getGame(game).countdown(2);
				player.sendMessage(ChatColor.AQUA + " Arena Started!");
			}
		}
		return true;
	}

	@Override
	public String help(Player p) {
		// TODO Auto-generated method stub
		return null;
	}

}
