/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xlythe.math;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Persist {
    private static final int LAST_VERSION = 3;
    private static final String FILE_NAME = "calculator.data";
    private final Context mContext;
    History mHistory = new History();
    private int mDeleteMode;
    private Base mMode;

    public Persist(Context context) {
        this.mContext = context;
    }

    public int getDeleteMode() {
        return mDeleteMode;
    }

    public void setDeleteMode(int mode) {
        mDeleteMode = mode;
    }

    public Base getMode() {
        return mMode;
    }

    public void setMode(Base mode) {
        this.mMode = mode;
    }

    public void load() {
        try {
            InputStream is = new BufferedInputStream(mContext.openFileInput(FILE_NAME), 8192);
            DataInputStream in = new DataInputStream(is);
            int version = in.readInt();
            if(version > LAST_VERSION) {
                throw new IOException("data version " + version + "; expected " + LAST_VERSION);
            }
            if(version > 1) {
                mDeleteMode = in.readInt();
            }
            if(version > 2) {
                int quickSerializable = in.readInt();
                for(Base m : Base.values()) {
                    if(m.getQuickSerializable() == quickSerializable) this.mMode = m;
                }
            }
            mHistory = new History(version, in);
            in.close();
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            OutputStream os = new BufferedOutputStream(mContext.openFileOutput(FILE_NAME, 0), 8192);
            DataOutputStream out = new DataOutputStream(os);
            out.writeInt(LAST_VERSION);
            out.writeInt(mDeleteMode);
            out.writeInt(mMode == null ? Base.DECIMAL.getQuickSerializable() : mMode.getQuickSerializable());
            mHistory.write(out);
            out.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public History getHistory() {
        return mHistory;
    }
}
