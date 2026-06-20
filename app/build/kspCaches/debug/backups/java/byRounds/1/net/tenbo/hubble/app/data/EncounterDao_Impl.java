package net.tenbo.hubble.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class EncounterDao_Impl implements EncounterDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<EncounterEntity> __insertionAdapterOfEncounterEntity;

  private final SharedSQLiteStatement __preparedStmtOfDecide;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public EncounterDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEncounterEntity = new EntityInsertionAdapter<EncounterEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `encounters` (`hubbleId`,`name`,`age`,`city`,`bio`,`promptAnswer`,`photoPath`,`place`,`lastSeenEpochMs`,`count`,`decided`,`liked`,`likesYouBack`,`signingPublicKey`,`agreementPublicKey`,`rootKey`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EncounterEntity entity) {
        statement.bindString(1, entity.getHubbleId());
        statement.bindString(2, entity.getName());
        statement.bindLong(3, entity.getAge());
        statement.bindString(4, entity.getCity());
        statement.bindString(5, entity.getBio());
        statement.bindString(6, entity.getPromptAnswer());
        if (entity.getPhotoPath() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getPhotoPath());
        }
        statement.bindString(8, entity.getPlace());
        statement.bindLong(9, entity.getLastSeenEpochMs());
        statement.bindLong(10, entity.getCount());
        final int _tmp = entity.getDecided() ? 1 : 0;
        statement.bindLong(11, _tmp);
        final int _tmp_1 = entity.getLiked() ? 1 : 0;
        statement.bindLong(12, _tmp_1);
        final int _tmp_2 = entity.getLikesYouBack() ? 1 : 0;
        statement.bindLong(13, _tmp_2);
        statement.bindBlob(14, entity.getSigningPublicKey());
        statement.bindBlob(15, entity.getAgreementPublicKey());
        statement.bindBlob(16, entity.getRootKey());
      }
    };
    this.__preparedStmtOfDecide = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE encounters SET decided = 1, liked = ? WHERE hubbleId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM encounters WHERE hubbleId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertNew(final EncounterEntity encounter,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfEncounterEntity.insert(encounter);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object decide(final String hubbleId, final boolean liked,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDecide.acquire();
        int _argIndex = 1;
        final int _tmp = liked ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, hubbleId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDecide.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final String hubbleId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, hubbleId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object pending(final Continuation<? super List<EncounterEntity>> $completion) {
    final String _sql = "SELECT * FROM encounters WHERE decided = 0 ORDER BY lastSeenEpochMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<EncounterEntity>>() {
      @Override
      @NonNull
      public List<EncounterEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfHubbleId = CursorUtil.getColumnIndexOrThrow(_cursor, "hubbleId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
          final int _cursorIndexOfCity = CursorUtil.getColumnIndexOrThrow(_cursor, "city");
          final int _cursorIndexOfBio = CursorUtil.getColumnIndexOrThrow(_cursor, "bio");
          final int _cursorIndexOfPromptAnswer = CursorUtil.getColumnIndexOrThrow(_cursor, "promptAnswer");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final int _cursorIndexOfPlace = CursorUtil.getColumnIndexOrThrow(_cursor, "place");
          final int _cursorIndexOfLastSeenEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeenEpochMs");
          final int _cursorIndexOfCount = CursorUtil.getColumnIndexOrThrow(_cursor, "count");
          final int _cursorIndexOfDecided = CursorUtil.getColumnIndexOrThrow(_cursor, "decided");
          final int _cursorIndexOfLiked = CursorUtil.getColumnIndexOrThrow(_cursor, "liked");
          final int _cursorIndexOfLikesYouBack = CursorUtil.getColumnIndexOrThrow(_cursor, "likesYouBack");
          final int _cursorIndexOfSigningPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "signingPublicKey");
          final int _cursorIndexOfAgreementPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementPublicKey");
          final int _cursorIndexOfRootKey = CursorUtil.getColumnIndexOrThrow(_cursor, "rootKey");
          final List<EncounterEntity> _result = new ArrayList<EncounterEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EncounterEntity _item;
            final String _tmpHubbleId;
            _tmpHubbleId = _cursor.getString(_cursorIndexOfHubbleId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpAge;
            _tmpAge = _cursor.getInt(_cursorIndexOfAge);
            final String _tmpCity;
            _tmpCity = _cursor.getString(_cursorIndexOfCity);
            final String _tmpBio;
            _tmpBio = _cursor.getString(_cursorIndexOfBio);
            final String _tmpPromptAnswer;
            _tmpPromptAnswer = _cursor.getString(_cursorIndexOfPromptAnswer);
            final String _tmpPhotoPath;
            if (_cursor.isNull(_cursorIndexOfPhotoPath)) {
              _tmpPhotoPath = null;
            } else {
              _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            }
            final String _tmpPlace;
            _tmpPlace = _cursor.getString(_cursorIndexOfPlace);
            final long _tmpLastSeenEpochMs;
            _tmpLastSeenEpochMs = _cursor.getLong(_cursorIndexOfLastSeenEpochMs);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            final boolean _tmpDecided;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfDecided);
            _tmpDecided = _tmp != 0;
            final boolean _tmpLiked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfLiked);
            _tmpLiked = _tmp_1 != 0;
            final boolean _tmpLikesYouBack;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfLikesYouBack);
            _tmpLikesYouBack = _tmp_2 != 0;
            final byte[] _tmpSigningPublicKey;
            _tmpSigningPublicKey = _cursor.getBlob(_cursorIndexOfSigningPublicKey);
            final byte[] _tmpAgreementPublicKey;
            _tmpAgreementPublicKey = _cursor.getBlob(_cursorIndexOfAgreementPublicKey);
            final byte[] _tmpRootKey;
            _tmpRootKey = _cursor.getBlob(_cursorIndexOfRootKey);
            _item = new EncounterEntity(_tmpHubbleId,_tmpName,_tmpAge,_tmpCity,_tmpBio,_tmpPromptAnswer,_tmpPhotoPath,_tmpPlace,_tmpLastSeenEpochMs,_tmpCount,_tmpDecided,_tmpLiked,_tmpLikesYouBack,_tmpSigningPublicKey,_tmpAgreementPublicKey,_tmpRootKey);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object total(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM encounters";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
