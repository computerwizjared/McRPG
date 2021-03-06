package us.eunoians.mcrpg.api.util;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.api.exceptions.McRPGPlayerNotFoundException;
import us.eunoians.mcrpg.api.leaderboards.LeaderboardManager;
import us.eunoians.mcrpg.api.leaderboards.PlayerLeaderboardData;
import us.eunoians.mcrpg.players.McRPGPlayer;
import us.eunoians.mcrpg.players.PlayerManager;
import us.eunoians.mcrpg.types.Skills;

public class McRPGPlaceHolders extends PlaceholderExpansion {
  @Override
  public String getIdentifier() {
    return "mcrpg";
  }

  @Override
  public String getPlugin() {
    return McRPG.getInstance().getName();
  }

  @Override
  public String getAuthor() {
    return "DiamondDagger590";
  }

  @Override
  public String getVersion() {
    return "0.2";
  }

  @Override
  public String onPlaceholderRequest(Player player, String identifier) {

    if(player == null) {
      return null;
    }
    McRPGPlayer mp;
    try{
      mp = PlayerManager.getPlayer(player.getUniqueId());
    }
    catch(McRPGPlayerNotFoundException exception){
      mp = new McRPGPlayer(player.getUniqueId());
    }
    String [] args = identifier.split("_");
    if(identifier.equalsIgnoreCase("power_level")){
      return Integer.toString(mp.getPowerLevel());
    }
    else if(identifier.contains("_level")){
      Skills skill = Skills.fromString(args[0]);
      return Integer.toString(mp.getSkill(skill).getCurrentLevel());
    }
    else if(identifier.contains("_exp_needed")){
      Skills skill = Skills.fromString(args[0]);
      return Integer.toString(mp.getSkill(skill).getExpToLevel());
    }
    else if(identifier.contains("_exp")){
      Skills skill = Skills.fromString(args[0]);
      return Integer.toString(mp.getSkill(skill).getCurrentExp());
    }
    else if(identifier.contains("_rank")){
      LeaderboardManager manager = McRPG.getInstance().getLeaderboardManager();
      if(!Methods.isInt(args[2])){
        return "ERROR";
      }
      int rank = Integer.parseInt(args[2]);
      if(args[0].equalsIgnoreCase("power")){
        PlayerLeaderboardData data = manager.getPowerPlayer(rank);
        return data == null ? "N/A" : identifier.contains("name") ? Bukkit.getOfflinePlayer(  data.getUUID()).getName() : Integer.toString(data.getLevel());
      }
      else if(Skills.isSkill(args[0])){
        Skills skill = Skills.fromString(args[0]);
        PlayerLeaderboardData data = manager.getSkillPlayer(rank, skill);
        return data == null ? "N/A" : identifier.contains("name") ? Bukkit.getOfflinePlayer(data.getUUID()).getName() : Integer.toString(data.getLevel());
      }
    }
    return "";
  }
}
