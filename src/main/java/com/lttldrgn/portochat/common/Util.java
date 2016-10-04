/*
 *  This file is a part of port-o-chat.
 * 
 *  port-o-chat is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lttldrgn.portochat.common;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Mike
 */
public class Util {

    public static final int MASK_1_BIT = 0x1;
    public static final int MASK_2_BIT = 0x3;
    public static final int MASK_3_BIT = 0x7;
    public static final int MASK_4_BIT = 0xF;
    public static final int MASK_8_BIT = 0xFF;
    private static final SimpleDateFormat formatDate =
            new SimpleDateFormat("MMM-dd hh:mm.ssa");

    public static int getBitValue(int value, int bitStart, int bitMask) {
        if (bitStart > 0) {
            value = value >> bitStart;
        }
        return value & bitMask;
    }

    public static String getTimestamp() {
        Date currentDate = new Date();
        return formatDate.format(currentDate);
    }

    public static String byteArrayToHexString(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            if ((i % 24) == 0) {
                sb.append("\n\t");
            }
            int v = data[i] & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
            sb.append(" ");
        }
        return sb.toString().toUpperCase();
    }

    public static byte[] concat(byte[] A, byte[] B) {
        byte[] C = new byte[A.length + B.length];
        System.arraycopy(A, 0, C, 0, A.length);
        System.arraycopy(B, 0, C, A.length, B.length);

        return C;
    }
}