package com.onyx.cashflow.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Long;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PendingTransactionDao_Impl implements PendingTransactionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PendingTransaction> __insertionAdapterOfPendingTransaction;

  private final EntityDeletionOrUpdateAdapter<PendingTransaction> __deletionAdapterOfPendingTransaction;

  public PendingTransactionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPendingTransaction = new EntityInsertionAdapter<PendingTransaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pending_transactions` (`id`,`amount`,`merchant`,`senderAddress`,`rawSms`,`date`,`type`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PendingTransaction entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getAmount());
        statement.bindString(3, entity.getMerchant());
        statement.bindString(4, entity.getSenderAddress());
        statement.bindString(5, entity.getRawSms());
        statement.bindLong(6, entity.getDate());
        statement.bindString(7, __TransactionType_enumToString(entity.getType()));
      }
    };
    this.__deletionAdapterOfPendingTransaction = new EntityDeletionOrUpdateAdapter<PendingTransaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `pending_transactions` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PendingTransaction entity) {
        statement.bindLong(1, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final PendingTransaction pending,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfPendingTransaction.insertAndReturnId(pending);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final PendingTransaction pending,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPendingTransaction.handle(pending);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PendingTransaction>> getAll() {
    final String _sql = "SELECT * FROM pending_transactions ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pending_transactions"}, new Callable<List<PendingTransaction>>() {
      @Override
      @NonNull
      public List<PendingTransaction> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfSenderAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "senderAddress");
          final int _cursorIndexOfRawSms = CursorUtil.getColumnIndexOrThrow(_cursor, "rawSms");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final List<PendingTransaction> _result = new ArrayList<PendingTransaction>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PendingTransaction _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpMerchant;
            _tmpMerchant = _cursor.getString(_cursorIndexOfMerchant);
            final String _tmpSenderAddress;
            _tmpSenderAddress = _cursor.getString(_cursorIndexOfSenderAddress);
            final String _tmpRawSms;
            _tmpRawSms = _cursor.getString(_cursorIndexOfRawSms);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final TransactionType _tmpType;
            _tmpType = __TransactionType_stringToEnum(_cursor.getString(_cursorIndexOfType));
            _item = new PendingTransaction(_tmpId,_tmpAmount,_tmpMerchant,_tmpSenderAddress,_tmpRawSms,_tmpDate,_tmpType);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getCount() {
    final String _sql = "SELECT COUNT(*) FROM pending_transactions";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pending_transactions"}, new Callable<Integer>() {
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
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __TransactionType_enumToString(@NonNull final TransactionType _value) {
    switch (_value) {
      case EXPENSE: return "EXPENSE";
      case INCOME: return "INCOME";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private TransactionType __TransactionType_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "EXPENSE": return TransactionType.EXPENSE;
      case "INCOME": return TransactionType.INCOME;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
