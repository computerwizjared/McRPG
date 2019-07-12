package us.eunoians.mcrpg.database;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DbBackup {

  @Getter private ScheduledExecutorService backupThread;

  public DbBackup() {
    backupThread = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("db-backup-thread").build());
  }
}
