package com.android2.calculator3;

import org.javia.arity.Symbols;
import org.javia.arity.SyntaxException;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class CalculatorWidget extends AppWidgetProvider {
    private static final StringBuilder equation = new StringBuilder();
    private static String error;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews remoteViews;
        ComponentName calcWidget;

        error = context.getResources().getString(R.string.error);
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        calcWidget = new ComponentName(context, CalculatorWidget.class);
        setOnClickListeners(context, remoteViews);
        remoteViews.setTextViewText(R.id.display, equation.toString());
        appWidgetManager.updateAppWidget(calcWidget, remoteViews);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(equation.toString().equals(error)) equation.setLength(0);

        if(intent.getAction().equals("0")) {
            equation.append(0);
        }
        else if(intent.getAction().equals("1")) {
            equation.append(1);
        }
        else if(intent.getAction().equals("2")) {
            equation.append(2);
        }
        else if(intent.getAction().equals("3")) {
            equation.append(3);
        }
        else if(intent.getAction().equals("4")) {
            equation.append(4);
        }
        else if(intent.getAction().equals("5")) {
            equation.append(5);
        }
        else if(intent.getAction().equals("6")) {
            equation.append(6);
        }
        else if(intent.getAction().equals("7")) {
            equation.append(7);
        }
        else if(intent.getAction().equals("8")) {
            equation.append(8);
        }
        else if(intent.getAction().equals("9")) {
            equation.append(9);
        }
        else if(intent.getAction().equals(".")) {
            equation.append(".");
        }
        else if(intent.getAction().equals("/")) {
            equation.append("/");
        }
        else if(intent.getAction().equals("*")) {
            equation.append("*");
        }
        else if(intent.getAction().equals("-")) {
            equation.append("-");
        }
        else if(intent.getAction().equals("+")) {
            equation.append("+");
        }
        else if(intent.getAction().equals("=")) {
            final String input = equation.toString();
            final Symbols mSymbols = new Symbols();

            if(input.isEmpty()) return;

            equation.setLength(0);
            try {
                String result = Logic.tryFormattingWithPrecision(mSymbols.eval(input), 8, 10, error);
                equation.append(result);
            } catch (SyntaxException e) {
                equation.append(error);
            }
        }
        else if(intent.getAction().equals("clear")) {
            equation.setLength(0);
        }
        ComponentName calcWidget = new ComponentName(context, CalculatorWidget.class);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        remoteViews.setTextViewText(R.id.display, equation.toString());
        AppWidgetManager.getInstance(context).updateAppWidget(calcWidget, remoteViews);
        super.onReceive(context, intent);
    }

    private void setOnClickListeners(Context context, RemoteViews remoteViews) {
        final Intent intent = new Intent(context, CalculatorWidget.class);

        intent.setAction("0");
        remoteViews.setOnClickPendingIntent(R.id.digit0, PendingIntent.getBroadcast(context, 0, intent, 0));

        intent.setAction("1");
        remoteViews.setOnClickPendingIntent(R.id.digit1, PendingIntent.getBroadcast(context, 1, intent, 0));

        intent.setAction("2");
        remoteViews.setOnClickPendingIntent(R.id.digit2, PendingIntent.getBroadcast(context, 2, intent, 0));

        intent.setAction("3");
        remoteViews.setOnClickPendingIntent(R.id.digit3, PendingIntent.getBroadcast(context, 3, intent, 0));

        intent.setAction("4");
        remoteViews.setOnClickPendingIntent(R.id.digit4, PendingIntent.getBroadcast(context, 4, intent, 0));

        intent.setAction("5");
        remoteViews.setOnClickPendingIntent(R.id.digit5, PendingIntent.getBroadcast(context, 5, intent, 0));

        intent.setAction("6");
        remoteViews.setOnClickPendingIntent(R.id.digit6, PendingIntent.getBroadcast(context, 6, intent, 0));

        intent.setAction("7");
        remoteViews.setOnClickPendingIntent(R.id.digit7, PendingIntent.getBroadcast(context, 7, intent, 0));

        intent.setAction("8");
        remoteViews.setOnClickPendingIntent(R.id.digit8, PendingIntent.getBroadcast(context, 8, intent, 0));

        intent.setAction("9");
        remoteViews.setOnClickPendingIntent(R.id.digit9, PendingIntent.getBroadcast(context, 9, intent, 0));

        intent.setAction(".");
        remoteViews.setOnClickPendingIntent(R.id.dot, PendingIntent.getBroadcast(context, 10, intent, 0));

        intent.setAction("/");
        remoteViews.setOnClickPendingIntent(R.id.div, PendingIntent.getBroadcast(context, 11, intent, 0));

        intent.setAction("*");
        remoteViews.setOnClickPendingIntent(R.id.mul, PendingIntent.getBroadcast(context, 12, intent, 0));

        intent.setAction("-");
        remoteViews.setOnClickPendingIntent(R.id.minus, PendingIntent.getBroadcast(context, 13, intent, 0));

        intent.setAction("+");
        remoteViews.setOnClickPendingIntent(R.id.plus, PendingIntent.getBroadcast(context, 14, intent, 0));

        intent.setAction("=");
        remoteViews.setOnClickPendingIntent(R.id.equal, PendingIntent.getBroadcast(context, 15, intent, 0));

        intent.setAction("clear");
        remoteViews.setOnClickPendingIntent(R.id.clear, PendingIntent.getBroadcast(context, 16, intent, 0));
    }
}
