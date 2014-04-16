/*
 * Copyright (C) 2014 The CyanogenMod Project
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class SpellContext {
    private static String mySuffixText[] = {
            "", // Dummy! no level 0
            "", // Nothing for level 1
            " Thousand", " Million", " Billion", " Trillion", " (Thousand Trillion)",
            " (Million Trillion)", " (Billion Trillion)", };

    private static String myTeenText[] = { "Zero", "One", "Two", "Three", "Four", "Five", "Six",
            "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
            "Sixteen", "Seventeen", "Eighteen", "Ninteen", };

    // Used appropriately for under-cent values:
    private static String myCentText[] = { "Twenty", "Thirty", "Forty", "Fifty", "Sixty",
            "Seventy", "Eighty", "Ninety" };

    // Used appropriately for under-mil values.
    private static String myMilText[] = { "One Hundred", "Two Hundred", "Three Hundred",
            "Four Hundred", "Five Hundred", "Six Hundred", "Seven Hundred", "Eight Hundred",
            "Nine Hundred" };

    private static String[] myBelowThousandWords = { "zero", "one", "two", "three", "four", "five",
            "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen",
            "fifteen", "sixteen", "seventeen", "eighteen", "ninteen", "twenty", "thirty", "forty",
            "fifty", "sixty", "seventy", "eighty", "ninety", "hundred" };

    private static ArrayList<String> myBelowThousandWordList = new ArrayList<String>(
            Arrays.asList(myBelowThousandWords));

    private static long[] myBelowThousandValuess = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
            14, 15, 16, 17, 18, 19, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

    private static String[] mySuffixWords = { "trillion", "billion", "million", "thousand" };
    private static long[] mySuffixValues = { 1000000000000L, 1000000000L, 1000000L, 1000L };

    public static String replaceAllWithNumbers(String input) {
        String result = "";
        goingForward: for(int i = 0; i < input.length(); i++) {
            // Start reading character by character forwards
            String goingForward = input.substring(i);
            for (int j = 0; j < goingForward.length(); j++) {
                // And, in each loop, character by character backwards
                String goingBackward = goingForward.substring(0, goingForward.length() - j);

                // Attempt to parse words as numbers (ie: three)
                try {
                    long value = SpellContext.parse(goingBackward);
                    result += value;

                    // This worked. Add the length of goingBackward to the init loop
                    i += goingBackward.length() - 1;
                    continue goingForward;
                } catch(SpellException e) {
                    // Do nothing here
                }
            }

            result += input.charAt(i);
        }

        return result;
    }

    public static String spell(long number) throws SpellException {
        String text;
        if (number < 0L) {
            text = "Minus " + spell(-number, 1);
        } else {
            text = spell(number, 1);
        }

        int index_amp, index_perc;

        index_amp = text.lastIndexOf("$");
        index_perc = text.lastIndexOf("%");

        if (index_amp >= 0) {
            if (index_perc < 0 || index_amp > index_perc) {
                String text1 = text.substring(0, index_amp);
                String text2 = text.substring(index_amp + 1, text.length());
                text = text1 + " and " + text2;
            }
        }

        text = text.replaceAll("\\$", ", ");
        text = text.replaceAll("%", " and ");

        return text;
    }

    // WithSeparator () function:
    // It converts a number to string using 1000's separator.
    // It uses a simple recursive algorithm.
    public static String WithSeparator(long number) {
        if (number < 0) {
            return "-" + WithSeparator(-number);
        }

        if (number / 1000L > 0) {
            return WithSeparator(number / 1000L) + "," + String.format("%1$03d", number % 1000L);
        } else {
            return String.format(Locale.US, "%1$d", number);
        }
    }

    private static String SpellBelow1000(long number) throws SpellException {
        if (number < 0 || number >= 1000) throw new SpellException(
                "Expecting a number between 0 and 999: " + number);

        if (number < 20L) {
            return myTeenText[(int) number];
        } else if (number < 100L) {
            int div = (int) number / 10;
            int rem = (int) number % 10;

            if (rem == 0) {
                return myCentText[div - 2];
            } else {
                return myCentText[div - 2] + " " + SpellBelow1000(rem);
            }
        } else {
            int div = (int) number / 100;
            int rem = (int) number % 100;

            if (rem == 0) {
                return myMilText[div - 1];
            } else {
                return myMilText[div - 1] + "%" + SpellBelow1000(rem);
            }
        }
    }

    private static String spell(long number, int level) throws SpellException {
        long div = number / 1000L;
        long rem = number % 1000L;

        if (div == 0) {
            return SpellBelow1000(rem) + mySuffixText[level];
        } else {
            if (rem == 0) {
                return spell(div, level + 1);
            } else {
                return spell(div, level + 1) + "$" + SpellBelow1000(rem) + mySuffixText[level];
            }
        }
    }

    public static long parseBelow1000(String text) throws SpellException {
        long value = 0;
        String[] words = text.replaceAll(" and ", " ").split("\\s");

        for (String word : words) {
            if (!myBelowThousandWordList.contains(word)) {
                throw new SpellException("Unknown token : " + word);
            }

            long subval = getValueOf(word);

            if (subval == 100) {
                if (value == 0) {
                    value = 100;
                } else {
                    value *= 100;
                }
            } else {
                value += subval;
            }
        }

        return value;
    }

    private static long getValueOf(String word) {
        return myBelowThousandValuess[myBelowThousandWordList.indexOf(word)];
    }

    public static long parse(String text) throws SpellException {
        text = text.toLowerCase(Locale.US).replaceAll("[,]", " ").replaceAll(" and ", " ");

        long totalValue = 0;
        boolean processed = false;
        for (int n = 0; n < mySuffixWords.length; n++) {
            int index = text.indexOf(mySuffixWords[n]);
            if (index >= 0) {
                String text1 = text.substring(0, index).trim();
                String text2 = text.substring(index + mySuffixWords[n].length()).trim();

                if (text1.equals("")) {
                    text1 = "one";
                }
                if (text2.equals("")) {
                    text2 = "zero";
                }

                totalValue = parseBelow1000(text1) * mySuffixValues[n] + parse(text2);
                processed = true;
                break;
            }
        }

        if (processed) {
            return totalValue;
        } else {
            return parseBelow1000(text);
        }
    }
}
