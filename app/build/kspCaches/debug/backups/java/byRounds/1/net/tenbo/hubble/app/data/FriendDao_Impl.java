package net.tenbo.hubble.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
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
public final class FriendDao_Impl implements FriendDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FriendEntity> __insertionAdapterOfFriendEntity;

  public FriendDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFriendEntity = new EntityInsertionAdapter<FriendEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `friends` (`hubbleId`,`displayName`,`signingPublicKey`,`agreementPublicKey`,`rootKey`,`establishedAtEpochMs`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FriendEntity entity) {
        statement.bindString(1, entity.getHubbleId());
        statement.bindString(2, entity.getDisplayName());
        statement.bindBlob(3, entity.getSigningPublicKey());
        statement.bindBlob(4, entity.getAgreementPublicKey());
        statement.bindBlob(5, entity.getRootKey());
        statement.bindLong(6, entity.getEstablishedAtEpochMs());
      }
    };
  }

  @Override
  public Object upsert(final FriendEntity friend, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFriendEntity.insert(friend);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object all(final Continuation<? super List<FriendEntity>> $completion) {
    final String _sql = "SELECT * FROM friends ORDER BY establishedAtEpochMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<FriendEntity>>() {
      @Override
      @NonNull
      public List<FriendEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfHubbleId = CursorUtil.getColumnIndexOrThrow(_cursor, "hubbleId");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfSigningPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "signingPublicKey");
          final int _cursorIndexOfAgreementPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "agreementPublicKey");
          final int _cursorIndexOfRootKey = CursorUtil.getColumnIndexOrThrow(_cursor, "rootKey");
          final int _cursorIndexOfEstablishedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "establishedAtEpochMs");
          final List<FriendEntity> _result = new ArrayList<FriendEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FriendEntity _item;
            final String _tmpHubbleId;
            _tmpHubbleId = _cursor.getString(_cursorIndexOfHubbleId);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final byte[] _tmpSigningPublicKey;
            _tmpSigningPublicKey = _cursor.getBlob(_cursorIndexOfSigningPublicKey);
            final byte[] _tmpAgreementPublicKey;
            _tmpAgreementPublicKey = _cursor.getBlob(_cursorIndexOfAgreementPublicKey);
            final byte[] _tmpRootKey;
            _tmpRootKey = _cursor.getBlob(_cursorIndexOfRootKey);
            final long _tmpEstablishedAtEpochMs;
            _tmpEstablishedAtEpochMs = _cursor.getLong(_cursorIndexOfEstablishedAtEpochMs);
            _item = new FriendEntity(_tmpHubbleId,_tmpDisplayName,_tmpSigningPublicKey,_tmpAgreementPublicKey,_tmpRootKey,_tmpEstablishedAtEpochMs);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
