package net.tenbo.hubble.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
public final class MatchDao_Impl implements MatchDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MatchEntity> __insertionAdapterOfMatchEntity;

  private final SharedSQLiteStatement __preparedStmtOfTouch;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public MatchDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMatchEntity = new EntityInsertionAdapter<MatchEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `matches` (`hubbleId`,`name`,`age`,`city`,`photoPath`,`place`,`signingPublicKey`,`agreementPublicKey`,`rootKey`,`matchedAtEpochMs`,`lastActivityEpochMs`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MatchEntity entity) {
        statement.bindString(1, entity.getHubbleId());
        statement.bindString(2, entity.getName());
        statement.bindLong(3, entity.getAge());
        statement.bindString(4, entity.getCity());
        if (entity.getPhotoPath() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getPhotoPath());
        }
        statement.bindString(6, entity.getPlace());
        statement.bindBlob(7, entity.getSigningPublicKey());
        statement.bindBlob(8, entity.getAgreementPublicKey());
        statement.bindBlob(9, entity.getRootKey());
        statement.bindLong(10, entity.getMatchedAtEpochMs());
        statement.bindLong(11, entity.getLastActivityEpochMs());
      }
    };
    this.__preparedStmtOfTouch = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE matches SET lastActivityEpochMs = ? WHERE hubbleId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM matches WHERE hubbleId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final MatchEntity match, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMatchEntity.insert(match);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object touch(final String hubbleId, final long at,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfTouch.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, at);
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
          __preparedStmtOfTouch.release(_stmt);
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
  public Object all(final Continuation<? super List<MatchEntity>> $completion) {
    final String _sql = "SELECT * FROM matches ORDER BY lastActivityEpochMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MatchEntity>>() {
      @Override
      @NonNull
      public List<MatchEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfHubbleId = CursorUtil.getColumnIndexOrThrow(_cursor, "hubbleId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
          final int _cursorIndexOfCity = CursorUtil.getColumnIndexOrThrow(_cursor, "city");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final int _cursorIndexOfPlace = CursorUtil.getColumnIndexOrThrow(_cursor, "place");
          final int _cursorIndexOfSigningPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "signingPublicKey");
          final int _cursorIndexOfAgreementPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementPublicKey");
          final int _cursorIndexOfRootKey = CursorUtil.getColumnIndexOrThrow(_cursor, "rootKey");
          final int _cursorIndexOfMatchedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "matchedAtEpochMs");
          final int _cursorIndexOfLastActivityEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastActivityEpochMs");
          final List<MatchEntity> _result = new ArrayList<MatchEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MatchEntity _item;
            final String _tmpHubbleId;
            _tmpHubbleId = _cursor.getString(_cursorIndexOfHubbleId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpAge;
            _tmpAge = _cursor.getInt(_cursorIndexOfAge);
            final String _tmpCity;
            _tmpCity = _cursor.getString(_cursorIndexOfCity);
            final String _tmpPhotoPath;
            if (_cursor.isNull(_cursorIndexOfPhotoPath)) {
              _tmpPhotoPath = null;
            } else {
              _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            }
            final String _tmpPlace;
            _tmpPlace = _cursor.getString(_cursorIndexOfPlace);
            final byte[] _tmpSigningPublicKey;
            _tmpSigningPublicKey = _cursor.getBlob(_cursorIndexOfSigningPublicKey);
            final byte[] _tmpAgreementPublicKey;
            _tmpAgreementPublicKey = _cursor.getBlob(_cursorIndexOfAgreementPublicKey);
            final byte[] _tmpRootKey;
            _tmpRootKey = _cursor.getBlob(_cursorIndexOfRootKey);
            final long _tmpMatchedAtEpochMs;
            _tmpMatchedAtEpochMs = _cursor.getLong(_cursorIndexOfMatchedAtEpochMs);
            final long _tmpLastActivityEpochMs;
            _tmpLastActivityEpochMs = _cursor.getLong(_cursorIndexOfLastActivityEpochMs);
            _item = new MatchEntity(_tmpHubbleId,_tmpName,_tmpAge,_tmpCity,_tmpPhotoPath,_tmpPlace,_tmpSigningPublicKey,_tmpAgreementPublicKey,_tmpRootKey,_tmpMatchedAtEpochMs,_tmpLastActivityEpochMs);
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
  public Object byId(final String hubbleId, final Continuation<? super MatchEntity> $completion) {
    final String _sql = "SELECT * FROM matches WHERE hubbleId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, hubbleId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MatchEntity>() {
      @Override
      @Nullable
      public MatchEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfHubbleId = CursorUtil.getColumnIndexOrThrow(_cursor, "hubbleId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
          final int _cursorIndexOfCity = CursorUtil.getColumnIndexOrThrow(_cursor, "city");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final int _cursorIndexOfPlace = CursorUtil.getColumnIndexOrThrow(_cursor, "place");
          final int _cursorIndexOfSigningPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "signingPublicKey");
          final int _cursorIndexOfAgreementPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementPublicKey");
          final int _cursorIndexOfRootKey = CursorUtil.getColumnIndexOrThrow(_cursor, "rootKey");
          final int _cursorIndexOfMatchedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "matchedAtEpochMs");
          final int _cursorIndexOfLastActivityEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "lastActivityEpochMs");
          final MatchEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpHubbleId;
            _tmpHubbleId = _cursor.getString(_cursorIndexOfHubbleId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpAge;
            _tmpAge = _cursor.getInt(_cursorIndexOfAge);
            final String _tmpCity;
            _tmpCity = _cursor.getString(_cursorIndexOfCity);
            final String _tmpPhotoPath;
            if (_cursor.isNull(_cursorIndexOfPhotoPath)) {
              _tmpPhotoPath = null;
            } else {
              _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            }
            final String _tmpPlace;
            _tmpPlace = _cursor.getString(_cursorIndexOfPlace);
            final byte[] _tmpSigningPublicKey;
            _tmpSigningPublicKey = _cursor.getBlob(_cursorIndexOfSigningPublicKey);
            final byte[] _tmpAgreementPublicKey;
            _tmpAgreementPublicKey = _cursor.getBlob(_cursorIndexOfAgreementPublicKey);
            final byte[] _tmpRootKey;
            _tmpRootKey = _cursor.getBlob(_cursorIndexOfRootKey);
            final long _tmpMatchedAtEpochMs;
            _tmpMatchedAtEpochMs = _cursor.getLong(_cursorIndexOfMatchedAtEpochMs);
            final long _tmpLastActivityEpochMs;
            _tmpLastActivityEpochMs = _cursor.getLong(_cursorIndexOfLastActivityEpochMs);
            _result = new MatchEntity(_tmpHubbleId,_tmpName,_tmpAge,_tmpCity,_tmpPhotoPath,_tmpPlace,_tmpSigningPublicKey,_tmpAgreementPublicKey,_tmpRootKey,_tmpMatchedAtEpochMs,_tmpLastActivityEpochMs);
          } else {
            _result = null;
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
