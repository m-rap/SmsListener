package com.mrap.smslistener;

import android.graphics.Paint;
import android.graphics.Rect;

import com.mrap.smslistener.model.MergedSmsSqliteHandler;

public class EllipsizeUtil {
    private final String bodySingleLine;
    private final MergedSmsSqliteHandler.SearchResult searchResult;
    private final Paint paint;
    private final int targetWidth;
    private final String ellipsizeStr = "...";
    private final int ellipsizeWidth;
    private final int spareWidth;
    private int ellipsizeMode = 0;

    public EllipsizeUtil(String bodySingleLine, MergedSmsSqliteHandler.SearchResult searchResult,
                         Paint paint, int targetWidth) {
        this.bodySingleLine = bodySingleLine;
        this.searchResult = searchResult;
        this.paint = paint;
        this.targetWidth = targetWidth;

        Rect bound = new Rect();
        paint.getTextBounds(ellipsizeStr, 0, ellipsizeStr.length(), bound);
        ellipsizeWidth = bound.width();

        paint.getTextBounds("__", 0, 2, bound);
        spareWidth = bound.width();

        ellipsizeMode = 0;
    }

    public String processEllipsize(
            ) {
        String content;
        if (bodySingleLine.length() - searchResult.charEndPos < searchResult.charStartPos) {
            content = processEllipsizeFromBack(bodySingleLine, searchResult, paint, targetWidth);
        } else {
            content = processEllipsizeFromFront(bodySingleLine, searchResult, paint, targetWidth);
        }

        if ((ellipsizeMode & 0x2) > 0) {
            content += ellipsizeStr;
        }
        if ((ellipsizeMode & 0x1) > 0) {
            content = ellipsizeStr + content;
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

    private String processEllipsizeFromBack(
            String bodySingleLine, MergedSmsSqliteHandler.SearchResult searchResult,
            Paint paint, int targetWidth) {
        String content;
        int subEnd = searchResult.charEndPos + 10;
        if (bodySingleLine.length() < subEnd) {
            subEnd = bodySingleLine.length();
        } else {
            ellipsizeMode |= 0x2;
        }

        content = bodySingleLine.substring(0, subEnd);

        Rect bound = new Rect();

        int subStart = 0;
        paint.getTextBounds(content, subStart, content.length(), bound);
        int textWidth = bound.width();

        int ellipsizeCount = getEllipsizeCount();

        if (textWidth + ellipsizeWidth * ellipsizeCount + spareWidth > targetWidth) {
            ellipsizeMode |= 0x1;
        }

        while (true) {
            paint.getTextBounds(content, subStart, content.length(), bound);
            textWidth = bound.width();

            ellipsizeCount = getEllipsizeCount();

            if (textWidth + ellipsizeWidth * ellipsizeCount + spareWidth <= targetWidth) {
                break;
            }
            subStart++;
            if (subStart >= content.length() - 1) {
                break;
            }
        }

        content = content.substring(subStart);

        return content;
    }

    private String processEllipsizeFromFront(
            String bodySingleLine, MergedSmsSqliteHandler.SearchResult searchResult,
            Paint paint, int targetWidth) {
        String content;
        int subStart = searchResult.charStartPos - 10;
        if (subStart < 0) {
            subStart = 0;
        } else {
            ellipsizeMode |= 0x1;
        }

        content = bodySingleLine.substring(subStart);

        Rect bound = new Rect();

        int subEnd = content.length();
        paint.getTextBounds(content, 0, subEnd, bound);
        int textWidth = bound.width();

        int ellipsizeCount = getEllipsizeCount();

        if (textWidth + ellipsizeWidth * ellipsizeCount + spareWidth > targetWidth) {
            ellipsizeMode |= 0x2;
        }

        while (true) {
            paint.getTextBounds(content, 0, subEnd, bound);
            textWidth = bound.width();

            ellipsizeCount = getEllipsizeCount();

            if (textWidth + ellipsizeWidth * ellipsizeCount + spareWidth <= targetWidth) {
                break;
            }

            subEnd--;
            if (subEnd <= 1) {
                break;
            }
        }

        content = content.substring(0, subEnd);

        return content;
    }
}
