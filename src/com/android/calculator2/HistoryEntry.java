/*
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class HistoryEntry {
    private static final int VERSION_1 = 1;
    private String mBase;
    private String mEdited;

    HistoryEntry(String base, String edited) {
        mBase = base;
        mEdited = edited;
    }

    HistoryEntry(int version, DataInput in) throws IOException {
        if (version >= VERSION_1) {
            mBase = in.readUTF();
            mEdited = in.readUTF();
        } else {
            throw new IOException("invalid version " + version);
        }
    }

    void write(DataOutput out) throws IOException {
        out.writeUTF(mBase);
        out.writeUTF(mEdited);
    }

    @Override
    public String toString() {
        return mBase;
    }

    void clearEdited() {
        mEdited = mBase;
    }

    public String getEdited() {
        return mEdited;
    }

    void setEdited(String edited) {
        mEdited = edited;
    }

    public String getBase() {
        return mBase;
    }
}
