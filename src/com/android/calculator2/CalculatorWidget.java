package com.android.calculator2;

import org.javia.arity.SyntaxException;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class CalculatorWidget extends AppWidgetProvider {
    public static final String DIGIT_0 = "0";
    public static final String DIGIT_1 = "1";
    public static final String DIGIT_2 = "2";
    public static final String DIGIT_3 = "3";
    public static final String DIGIT_4 = "4";
    public static final String DIGIT_5 = "5";
    public static final String DIGIT_6 = "6";
    public static final String DIGIT_7 = "7";
    public static final String DIGIT_8 = "8";
    public static final String DIGIT_9 = "9";
    public static final String DOT = "dot";
    public static final String PLUS = "plus";
    public static final String MINUS = "minus";
    public static final String MUL = "mul";
    public static final String DIV = "div";
    public static final String EQUALS = "equals";
    public static final String CLR = "clear";

    private static final StringBuilder equation = new StringBuilder();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews remoteViews;
        ComponentName calcWidget;

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        calcWidget = new ComponentName(context, CalculatorWidget.class);
        setOnClickListeners(context, remoteViews);
        remoteViews.setTextViewText(R.id.display, equation.toString());
        appWidgetManager.updateAppWidget(calcWidget, remoteViews);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(equation.toString().equals(context.getResources().getString(R.string.error))) equation.setLength(0);

        if(intent.getAction().equals(DIGIT_0)) {
            equation.append(0);
        }
        else if(intent.getAction().equals(DIGIT_1)) {
            equation.append(1);
        }
        else if(intent.getAction().equals(DIGIT_2)) {
            equation.append(2);
        }
        else if(intent.getAction().equals(DIGIT_3)) {
            equation.append(3);
        }
        else if(intent.getAction().equals(DIGIT_4)) {
            equation.append(4);
        }
        else if(intent.getAction().equals(DIGIT_5)) {
            equation.append(5);
        }
        else if(intent.getAction().equals(DIGIT_6)) {
            equation.append(6);
        }
        else if(intent.getAction().equals(DIGIT_7)) {
            equation.append(7);
        }
        else if(intent.getAction().equals(DIGIT_8)) {
            equation.append(8);
        }
        else if(intent.getAction().equals(DIGIT_9)) {
            equation.append(9);
        }
        else if(intent.getAction().equals(DOT)) {
            equation.append(context.getResources().getString(R.string.dot));
        }
        else if(intent.getAction().equals(DIV)) {
            equation.append(context.getResources().getString(R.string.div));
        }
        else if(intent.getAction().equals(MUL)) {
            equation.append(context.getResources().getString(R.string.mul));
        }
        else if(intent.getAction().equals(MINUS)) {
            equation.append(context.getResources().getString(R.string.minus));
        }
        else if(intent.getAction().equals(PLUS)) {
            equation.append(context.getResources().getString(R.string.plus));
        }
        else if(intent.getAction().equals(EQUALS)) {
            final String input = equation.toString();
            final Logic mLogic = new Logic(context);
            mLogic.setLineLength(7);

            if(input.isEmpty()) return;

            equation.setLength(0);
            try {
                equation.append(mLogic.evaluate(input));
            }
            catch(SyntaxException e) {
                equation.append(context.getResources().getString(R.string.error));
            }
        }
        else if(intent.getAction().equals(CLR)) {
            equation.setLength(0);
        }
        ComponentName calcWidget = new ComponentName(context, CalculatorWidget.class);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        remoteViews.setTextViewText(R.id.display, equation.toString());
        setOnClickListeners(context, remoteViews);
        AppWidgetManager.getInstance(context).updateAppWidget(calcWidget, remoteViews);
        super.onReceive(context, intent);
    }

    private void setOnClickListeners(Context context, RemoteViews remoteViews) {
        final Intent intent = new Intent(context, CalculatorWidget.class);

        intent.setAction(DIGIT_0);
        remoteViews.setOnClickPendingIntent(R.id.digit0, PendingIntent.getBroadcast(context, 0, intent, 0));

        intent.setAction(DIGIT_1);
        remoteViews.setOnClickPendingIntent(R.id.digit1, PendingIntent.getBroadcast(context, 1, intent, 0));

        intent.setAction(DIGIT_2);
        remoteViews.setOnClickPendingIntent(R.id.digit2, PendingIntent.getBroadcast(context, 2, intent, 0));

        intent.setAction(DIGIT_3);
        remoteViews.setOnClickPendingIntent(R.id.digit3, PendingIntent.getBroadcast(context, 3, intent, 0));

        intent.setAction(DIGIT_4);
        remoteViews.setOnClickPendingIntent(R.id.digit4, PendingIntent.getBroadcast(context, 4, intent, 0));

        intent.setAction(DIGIT_5);
        remoteViews.setOnClickPendingIntent(R.id.digit5, PendingIntent.getBroadcast(context, 5, intent, 0));

        intent.setAction(DIGIT_6);
        remoteViews.setOnClickPendingIntent(R.id.digit6, PendingIntent.getBroadcast(context, 6, intent, 0));

        intent.setAction(DIGIT_7);
        remoteViews.setOnClickPendingIntent(R.id.digit7, PendingIntent.getBroadcast(context, 7, intent, 0));

        intent.setAction(DIGIT_8);
        remoteViews.setOnClickPendingIntent(R.id.digit8, PendingIntent.getBroadcast(context, 8, intent, 0));

        intent.setAction(DIGIT_9);
        remoteViews.setOnClickPendingIntent(R.id.digit9, PendingIntent.getBroadcast(context, 9, intent, 0));

        intent.setAction(DOT);
        remoteViews.setOnClickPendingIntent(R.id.dot, PendingIntent.getBroadcast(context, 10, intent, 0));

        intent.setAction(DIV);
        remoteViews.setOnClickPendingIntent(R.id.div, PendingIntent.getBroadcast(context, 11, intent, 0));

        intent.setAction(MUL);
        remoteViews.setOnClickPendingIntent(R.id.mul, PendingIntent.getBroadcast(context, 12, intent, 0));

        intent.setAction(MINUS);
        remoteViews.setOnClickPendingIntent(R.id.minus, PendingIntent.getBroadcast(context, 13, intent, 0));

        intent.setAction(PLUS);
        remoteViews.setOnClickPendingIntent(R.id.plus, PendingIntent.getBroadcast(context, 14, intent, 0));

        intent.setAction(EQUALS);
        remoteViews.setOnClickPendingIntent(R.id.equal, PendingIntent.getBroadcast(context, 15, intent, 0));

        intent.setAction(CLR);
        remoteViews.setOnClickPendingIntent(R.id.clear, PendingIntent.getBroadcast(context, 16, intent, 0));
    }
}
