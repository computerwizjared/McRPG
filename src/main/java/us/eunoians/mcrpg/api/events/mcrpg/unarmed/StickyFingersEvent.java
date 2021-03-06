package us.eunoians.mcrpg.api.events.mcrpg.unarmed;

import us.eunoians.mcrpg.abilities.unarmed.StickyFingers;
import us.eunoians.mcrpg.api.events.mcrpg.AbilityActivateEvent;
import us.eunoians.mcrpg.players.McRPGPlayer;
import us.eunoians.mcrpg.types.AbilityEventType;

public class StickyFingersEvent extends AbilityActivateEvent {

  public StickyFingersEvent(McRPGPlayer player, StickyFingers stickyFingers){
    super(stickyFingers, player, AbilityEventType.COMBAT);
  }
}
