/*
 * Copyright (c) 2021-2024, cxxwl96.com (cxxwl96@sina.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cxxwl96.updater.api.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PrettyUtil
 *
 * @author cxxwl96
 * @since 2024/04/08 23:18
 */
public class PrettyUtil {

    private static final long tB = 1024L * 1024 * 1024 * 1024;

    private static final long gB = 1024L * 1024 * 1024;

    private static final long mB = 1024L * 1024;

    private static final long kB = 1024L;

    private static final long[] sizeArray = new long[] {tB, gB, mB, kB};

    private static final String[] unit = new String[] {"TB", "GB", "MB", "KB"};

    public static String prettySize(long size) {
        int index = sizeArray.length - 1;
        for (int i = 0; i < sizeArray.length; i++) {
            if (size >= sizeArray[i]) {
                index = i;
                break;
            }
        }
        double val = new BigDecimal(size * 1.0 / sizeArray[index]).setScale(2, RoundingMode.HALF_UP).doubleValue();
        if (((int) val) == val) {
            return ((int) val) + unit[index];
        }
        return val + unit[index];
    }
}
