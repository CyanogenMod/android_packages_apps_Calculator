package com.android2.calculator3.dao;

import android.database.Cursor;
import android.database.SQLException;

public interface DataSource {
    public void open() throws SQLException;

    public void close();

    public String[] getColumns();

    public Cursor getRows();

    public String getTableName();
}
