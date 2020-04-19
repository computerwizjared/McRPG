package us.eunoians.mcrpg.players;

import com.cyr1en.flatdb.Database;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.database.McRPGDb;
import us.eunoians.mcrpg.skills.Skill;
import us.eunoians.mcrpg.skills.Woodcutting;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class McRPGPlayerTest {

  private Skill skill;
  private McRPGPlayer player;
  private Database database;

  @BeforeEach
  public void before() {
    skill = new Woodcutting(0, 0, null, null);

    database = mock(Database.class);

    McRPG mcRPG = mock(McRPG.class);
    McRPGDb db = mock(McRPGDb.class);

    when(db.getDatabase()).thenReturn(database);

    when(mcRPG.getMcRPGDb()).thenReturn(db);

    player = McRPGPlayer.builder().mcRPG(mcRPG).skills(Arrays.asList(skill)).build();
  }

  @Test
  public void testPlayerGetSkillByString() {
    assertEquals(player.getSkill("Woodcutting"), skill);
  }

  @Test
  public void testResetCooldowns() {
    player.resetCooldowns();

    verify(database).executeUpdate(anyString());
  }

}
