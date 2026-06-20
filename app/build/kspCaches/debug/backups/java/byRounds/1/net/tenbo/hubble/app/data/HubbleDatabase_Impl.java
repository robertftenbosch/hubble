package net.tenbo.hubble.app.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HubbleDatabase_Impl extends HubbleDatabase {
  private volatile FriendDao _friendDao;

  private volatile PostDao _postDao;

  private volatile EncounterDao _encounterDao;

  private volatile MatchDao _matchDao;

  private volatile MessageDao _messageDao;

  private volatile BlockDao _blockDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(7) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `friends` (`hubbleId` TEXT NOT NULL, `displayName` TEXT NOT NULL, `signingPublicKey` BLOB NOT NULL, `agreementPublicKey` BLOB NOT NULL, `rootKey` BLOB NOT NULL, `establishedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`hubbleId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `posts` (`id` TEXT NOT NULL, `authorHubbleId` TEXT NOT NULL, `authorName` TEXT NOT NULL, `text` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, `expiresAtEpochMs` INTEGER NOT NULL, `mine` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `encounters` (`hubbleId` TEXT NOT NULL, `name` TEXT NOT NULL, `age` INTEGER NOT NULL, `city` TEXT NOT NULL, `bio` TEXT NOT NULL, `promptAnswer` TEXT NOT NULL, `photoPath` TEXT, `place` TEXT NOT NULL, `lastSeenEpochMs` INTEGER NOT NULL, `count` INTEGER NOT NULL, `decided` INTEGER NOT NULL, `liked` INTEGER NOT NULL, `likesYouBack` INTEGER NOT NULL, `signingPublicKey` BLOB NOT NULL, `agreementPublicKey` BLOB NOT NULL, `rootKey` BLOB NOT NULL, PRIMARY KEY(`hubbleId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `matches` (`hubbleId` TEXT NOT NULL, `name` TEXT NOT NULL, `age` INTEGER NOT NULL, `city` TEXT NOT NULL, `photoPath` TEXT, `place` TEXT NOT NULL, `signingPublicKey` BLOB NOT NULL, `agreementPublicKey` BLOB NOT NULL, `rootKey` BLOB NOT NULL, `matchedAtEpochMs` INTEGER NOT NULL, `lastActivityEpochMs` INTEGER NOT NULL, PRIMARY KEY(`hubbleId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` TEXT NOT NULL, `matchHubbleId` TEXT NOT NULL, `fromMe` INTEGER NOT NULL, `text` TEXT NOT NULL, `sentAtEpochMs` INTEGER NOT NULL, `kind` TEXT NOT NULL, `audioPath` TEXT, `durationMs` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `blocks` (`hubbleId` TEXT NOT NULL, `reported` INTEGER NOT NULL, `atEpochMs` INTEGER NOT NULL, PRIMARY KEY(`hubbleId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '61e587c93d284f17fb90a10c9c4734d3')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `friends`");
        db.execSQL("DROP TABLE IF EXISTS `posts`");
        db.execSQL("DROP TABLE IF EXISTS `encounters`");
        db.execSQL("DROP TABLE IF EXISTS `matches`");
        db.execSQL("DROP TABLE IF EXISTS `messages`");
        db.execSQL("DROP TABLE IF EXISTS `blocks`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsFriends = new HashMap<String, TableInfo.Column>(6);
        _columnsFriends.put("hubbleId", new TableInfo.Column("hubbleId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFriends.put("displayName", new TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFriends.put("signingPublicKey", new TableInfo.Column("signingPublicKey", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFriends.put("agreementPublicKey", new TableInfo.Column("agreementPublicKey", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFriends.put("rootKey", new TableInfo.Column("rootKey", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFriends.put("establishedAtEpochMs", new TableInfo.Column("establishedAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFriends = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFriends = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFriends = new TableInfo("friends", _columnsFriends, _foreignKeysFriends, _indicesFriends);
        final TableInfo _existingFriends = TableInfo.read(db, "friends");
        if (!_infoFriends.equals(_existingFriends)) {
          return new RoomOpenHelper.ValidationResult(false, "friends(net.tenbo.hubble.app.data.FriendEntity).\n"
                  + " Expected:\n" + _infoFriends + "\n"
                  + " Found:\n" + _existingFriends);
        }
        final HashMap<String, TableInfo.Column> _columnsPosts = new HashMap<String, TableInfo.Column>(7);
        _columnsPosts.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("authorHubbleId", new TableInfo.Column("authorHubbleId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("authorName", new TableInfo.Column("authorName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("text", new TableInfo.Column("text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("createdAtEpochMs", new TableInfo.Column("createdAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("expiresAtEpochMs", new TableInfo.Column("expiresAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPosts.put("mine", new TableInfo.Column("mine", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPosts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPosts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPosts = new TableInfo("posts", _columnsPosts, _foreignKeysPosts, _indicesPosts);
        final TableInfo _existingPosts = TableInfo.read(db, "posts");
        if (!_infoPosts.equals(_existingPosts)) {
          return new RoomOpenHelper.ValidationResult(false, "posts(net.tenbo.hubble.app.data.PostEntity).\n"
                  + " Expected:\n" + _infoPosts + "\n"
                  + " Found:\n" + _existingPosts);
        }
        final HashMap<String, TableInfo.Column> _columnsEncounters = new HashMap<String, TableInfo.Column>(16);
        _columnsEncounters.put("hubbleId", new TableInfo.Column("hubbleId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("age", new TableInfo.Column("age", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("city", new TableInfo.Column("city", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("bio", new TableInfo.Column("bio", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("promptAnswer", new TableInfo.Column("promptAnswer", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("photoPath", new TableInfo.Column("photoPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("place", new TableInfo.Column("place", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("lastSeenEpochMs", new TableInfo.Column("lastSeenEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("count", new TableInfo.Column("count", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("decided", new TableInfo.Column("decided", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("liked", new TableInfo.Column("liked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("likesYouBack", new TableInfo.Column("likesYouBack", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("signingPublicKey", new TableInfo.Column("signingPublicKey", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("agreementPublicKey", new TableInfo.Column("agreementPublicKey", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEncounters.put("rootKey", new TableInfo.Column("rootKey", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEncounters = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesEncounters = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoEncounters = new TableInfo("encounters", _columnsEncounters, _foreignKeysEncounters, _indicesEncounters);
        final TableInfo _existingEncounters = TableInfo.read(db, "encounters");
        if (!_infoEncounters.equals(_existingEncounters)) {
          return new RoomOpenHelper.ValidationResult(false, "encounters(net.tenbo.hubble.app.data.EncounterEntity).\n"
                  + " Expected:\n" + _infoEncounters + "\n"
                  + " Found:\n" + _existingEncounters);
        }
        final HashMap<String, TableInfo.Column> _columnsMatches = new HashMap<String, TableInfo.Column>(11);
        _columnsMatches.put("hubbleId", new TableInfo.Column("hubbleId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("age", new TableInfo.Column("age", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("city", new TableInfo.Column("city", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("photoPath", new TableInfo.Column("photoPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("place", new TableInfo.Column("place", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("signingPublicKey", new TableInfo.Column("signingPublicKey", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("agreementPublicKey", new TableInfo.Column("agreementPublicKey", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("rootKey", new TableInfo.Column("rootKey", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("matchedAtEpochMs", new TableInfo.Column("matchedAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMatches.put("lastActivityEpochMs", new TableInfo.Column("lastActivityEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMatches = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMatches = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMatches = new TableInfo("matches", _columnsMatches, _foreignKeysMatches, _indicesMatches);
        final TableInfo _existingMatches = TableInfo.read(db, "matches");
        if (!_infoMatches.equals(_existingMatches)) {
          return new RoomOpenHelper.ValidationResult(false, "matches(net.tenbo.hubble.app.data.MatchEntity).\n"
                  + " Expected:\n" + _infoMatches + "\n"
                  + " Found:\n" + _existingMatches);
        }
        final HashMap<String, TableInfo.Column> _columnsMessages = new HashMap<String, TableInfo.Column>(8);
        _columnsMessages.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("matchHubbleId", new TableInfo.Column("matchHubbleId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("fromMe", new TableInfo.Column("fromMe", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("text", new TableInfo.Column("text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("sentAtEpochMs", new TableInfo.Column("sentAtEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("kind", new TableInfo.Column("kind", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("audioPath", new TableInfo.Column("audioPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("durationMs", new TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMessages = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMessages = new TableInfo("messages", _columnsMessages, _foreignKeysMessages, _indicesMessages);
        final TableInfo _existingMessages = TableInfo.read(db, "messages");
        if (!_infoMessages.equals(_existingMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "messages(net.tenbo.hubble.app.data.MessageEntity).\n"
                  + " Expected:\n" + _infoMessages + "\n"
                  + " Found:\n" + _existingMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsBlocks = new HashMap<String, TableInfo.Column>(3);
        _columnsBlocks.put("hubbleId", new TableInfo.Column("hubbleId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlocks.put("reported", new TableInfo.Column("reported", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlocks.put("atEpochMs", new TableInfo.Column("atEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBlocks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBlocks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBlocks = new TableInfo("blocks", _columnsBlocks, _foreignKeysBlocks, _indicesBlocks);
        final TableInfo _existingBlocks = TableInfo.read(db, "blocks");
        if (!_infoBlocks.equals(_existingBlocks)) {
          return new RoomOpenHelper.ValidationResult(false, "blocks(net.tenbo.hubble.app.data.BlockEntity).\n"
                  + " Expected:\n" + _infoBlocks + "\n"
                  + " Found:\n" + _existingBlocks);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "61e587c93d284f17fb90a10c9c4734d3", "5bade70b67b6a55aa984732a62b424fe");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "friends","posts","encounters","matches","messages","blocks");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `friends`");
      _db.execSQL("DELETE FROM `posts`");
      _db.execSQL("DELETE FROM `encounters`");
      _db.execSQL("DELETE FROM `matches`");
      _db.execSQL("DELETE FROM `messages`");
      _db.execSQL("DELETE FROM `blocks`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(FriendDao.class, FriendDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PostDao.class, PostDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(EncounterDao.class, EncounterDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MatchDao.class, MatchDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MessageDao.class, MessageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BlockDao.class, BlockDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public FriendDao friendDao() {
    if (_friendDao != null) {
      return _friendDao;
    } else {
      synchronized(this) {
        if(_friendDao == null) {
          _friendDao = new FriendDao_Impl(this);
        }
        return _friendDao;
      }
    }
  }

  @Override
  public PostDao postDao() {
    if (_postDao != null) {
      return _postDao;
    } else {
      synchronized(this) {
        if(_postDao == null) {
          _postDao = new PostDao_Impl(this);
        }
        return _postDao;
      }
    }
  }

  @Override
  public EncounterDao encounterDao() {
    if (_encounterDao != null) {
      return _encounterDao;
    } else {
      synchronized(this) {
        if(_encounterDao == null) {
          _encounterDao = new EncounterDao_Impl(this);
        }
        return _encounterDao;
      }
    }
  }

  @Override
  public MatchDao matchDao() {
    if (_matchDao != null) {
      return _matchDao;
    } else {
      synchronized(this) {
        if(_matchDao == null) {
          _matchDao = new MatchDao_Impl(this);
        }
        return _matchDao;
      }
    }
  }

  @Override
  public MessageDao messageDao() {
    if (_messageDao != null) {
      return _messageDao;
    } else {
      synchronized(this) {
        if(_messageDao == null) {
          _messageDao = new MessageDao_Impl(this);
        }
        return _messageDao;
      }
    }
  }

  @Override
  public BlockDao blockDao() {
    if (_blockDao != null) {
      return _blockDao;
    } else {
      synchronized(this) {
        if(_blockDao == null) {
          _blockDao = new BlockDao_Impl(this);
        }
        return _blockDao;
      }
    }
  }
}
