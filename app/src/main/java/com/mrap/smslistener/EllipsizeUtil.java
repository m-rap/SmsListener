package com.mrap.smslistener;

import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.util.Log;

public class EllipsizeUtil {
    private static final String TAG = "EllipsizeUtil";
    private final CharSequence value;
    private final int startPos;
    private final int endPos;
    private final Paint paint;
    private final int targetWidth;
    private final String ellipsizeStr = "...";
    private final int ellipsizeWidth;
    private final int spareWidth;
    private int ellipsizeMode = 0;
    private String valueStr;

    public EllipsizeUtil(CharSequence value, int startPos, int endPos,
                         Paint paint, int targetWidth) {
        this.value = value;
        this.startPos = startPos;
        this.endPos = endPos;
        this.paint = paint;
        this.targetWidth = targetWidth;

        Rect bound = new Rect();
        paint.getTextBounds(ellipsizeStr, 0, ellipsizeStr.length(), bound);
        ellipsizeWidth = bound.width();

        paint.getTextBounds("__", 0, 2, bound);
        spareWidth = bound.width();

        ellipsizeMode = 0;

//        Log.d(TAG, "value is " + value.getClass().getSimpleName());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            valueStr = value.toString();
        }

//        Log.d(TAG, "now value is " + value.getClass().getSimpleName());
    }

    public CharSequence processEllipsize() {
//        String content;
        CharSequence content;
        if (value.length() - endPos < startPos) {
            content = processEllipsizeFromBack();
        } else {
            content = processEllipsizeFromFront();
        }

        if ((ellipsizeMode & 0x2) > 0) {
            if (content instanceof SpannableStringBuilder) {
                SpannableStringBuilder sp = (SpannableStringBuilder) content;
                sp.append(ellipsizeStr);
            } else {
                content += ellipsizeStr;
            }
        }
        if ((ellipsizeMode & 0x1) > 0) {
            if (content instanceof SpannableStringBuilder) {
                SpannableStringBuilder sp = (SpannableStringBuilder) content;
//                SpannableStringBuilder ellipsizeSp = new SpannableStringBuilder(ellipsizeStr);
//                ellipsizeSp.append(sp);
                sp.insert(0, ellipsizeStr);
            } else {
                content = ellipsizeStr + content;
            }
        }

        return content;
    }

    private int getEllipsizeCount() {
        int ellipsizeCount = 0;
        if ((ellipsizeMode & 0x1) > 0) {
            ellipsizeCount++;
        }
        if ((ellipsizeMode & 0x2) > 0) {
            ellipsizeCount++;
        }
        return ellipsizeCount;
    }

    private void getTextBounds(int subStart, int subEnd, Rect bound) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                paint.getTextBounds(value, subStart, subEnd, bound);
            } else {
                paint.getTextBounds(valueStr, subStart, subEnd, bound);
            }
        } catch (Exception e) {
            Log.d(TAG, String.format("index out of bounds %s %d %d",
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ? value : valueStr,
                    subStart, subEnd));
            e.printStackTrace();
        }
    }

    private CharSequence processEllipsizeFromBack() {
//        String content;
        int subEnd = endPos + 10;
        if (value.length() < subEnd) {
            subEnd = value.length();
        } else {
            ellipsizeMode |= 0x2;
        }

//        content = value.substring(0, subEnd);

        Rect bound = new Rect();

        int subStart = 0;
//        paint.getTextBounds(content, subStart, content.length(), bound);
        getTextBounds(subStart, subEnd, bound);
        int textWidth = bound.width();

        int ellipsizeCount = getEllipsizeCount();

        if (textWidth + ellipsizeWidth * ellipsizeCount + spareWidth > targetWidth) {
            ellipsizeMode |= 0x1;
        }

        while (true) {
//            paint.getTextBounds(content, subStart, content.length(), bound);
            getTextBounds(subStart, subEnd, bound);
            textWidth = bound.width();

            ellipsizeCount = getEllipsizeCount();

            if (textWidth + ellipsizeWidth * ellipsizeCount + spareWidth <= targetWidth) {
                break;
            }
            subStart++;
//            if (subStart >= content.length() - 1) {
            if (subStart >= subEnd - 1) {
                break;
            }
        }

//        content = content.substring(subStart);
//        return content;

        return value.subSequence(subStart, subEnd);
    }

    private CharSequence processEllipsizeFromFront() {
//        String content;
        int subStart = startPos - 10;
        if (subStart < 0) {
            subStart = 0;
        } else {
            ellipsizeMode |= 0x1;
        }

//        content = value.substring(subStart);

        Rect bound = new Rect();

//        int subEnd = content.length();
//        paint.getTextBounds(content, 0, subEnd, bound);
        int subEnd = value.length() - subStart;
        getTextBounds(subStart, subEnd, bound);
        int textWidth = bound.width();

        int ellipsizeCount = getEllipsizeCount();

        if (textWidth + ellipsizeWidth * ellipsizeCount + spareWidth > targetWidth) {
            ellipsizeMode |= 0x2;
        }

        while (true) {
//            paint.getTextBounds(content, 0, subEnd, bound);
            getTextBounds(subStart, subEnd, bound);
            textWidth = bound.width();

            ellipsizeCount = getEllipsizeCount();

            if (textWidth + ellipsizeWidth * ellipsizeCount + spareWidth <= targetWidth) {
                break;
            }

            subEnd--;
            if (subEnd <= subStart + 1) {
                break;
            }
        }

//        content = content.substring(0, subEnd);
//        return content;

        return value.subSequence(subStart, subEnd);
    }
}
