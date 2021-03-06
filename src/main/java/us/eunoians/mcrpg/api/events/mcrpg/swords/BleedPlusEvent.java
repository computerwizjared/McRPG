package us.eunoians.mcrpg.api.events.mcrpg.swords;

import lombok.Getter;
import lombok.Setter;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.abilities.swords.BleedPlus;
import us.eunoians.mcrpg.api.events.mcrpg.AbilityActivateEvent;
import us.eunoians.mcrpg.api.util.FileManager;
import us.eunoians.mcrpg.api.util.Methods;
import us.eunoians.mcrpg.players.McRPGPlayer;
import us.eunoians.mcrpg.types.AbilityEventType;

public class BleedPlusEvent extends AbilityActivateEvent {

  @Getter @Setter
  private int damageBoost;

  public BleedPlusEvent(McRPGPlayer user, BleedPlus bleedPlus){
    super(bleedPlus, user, AbilityEventType.COMBAT);
	int tier = bleedPlus.getCurrentTier();
	this.damageBoost = McRPG.getInstance().getFileManager().getFile(FileManager.Files.SWORDS_CONFIG).getInt("Bleed+Config.Tier" + Methods.convertToNumeral(tier) + ".DamageBoost");
	this.isCancelled = !bleedPlus.isToggled();
  }
}
