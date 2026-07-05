package com.paycheckpilot.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.paycheckpilot.MainActivity
import com.paycheckpilot.R

class SafeToSpendWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> WidgetUpdater.updateSafeToSpend(context, manager, id) }
    }
}

class BillsBeforePaydayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> WidgetUpdater.updateBillsBeforePayday(context, manager, id) }
    }
}

object WidgetUpdater {
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        manager.getAppWidgetIds(ComponentName(context, SafeToSpendWidgetProvider::class.java)).forEach {
            updateSafeToSpend(context, manager, it)
        }
        manager.getAppWidgetIds(ComponentName(context, BillsBeforePaydayWidgetProvider::class.java)).forEach {
            updateBillsBeforePayday(context, manager, it)
        }
    }

    fun updateSafeToSpend(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val snapshot = WidgetSnapshotStore.load(context)
        val views = RemoteViews(context.packageName, R.layout.widget_safe_to_spend)
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        views.setTextViewText(R.id.widget_safe_amount, snapshot.safeToSpend)
        views.setTextViewText(R.id.widget_status, snapshot.status)
        views.setTextViewText(R.id.widget_days, snapshot.daysUntilPayday)
        views.setTextViewText(R.id.widget_payday, snapshot.nextPayday)
        views.setViewVisibility(R.id.widget_setup_hint, if (snapshot.hasSetup) View.GONE else View.VISIBLE)
        manager.updateAppWidget(widgetId, views)
    }

    fun updateBillsBeforePayday(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val snapshot = WidgetSnapshotStore.load(context)
        val views = RemoteViews(context.packageName, R.layout.widget_bills_before_payday)
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        views.setTextViewText(R.id.widget_bills_total, snapshot.billsBeforePayday)
        views.setTextViewText(R.id.widget_left_after_bills, snapshot.leftAfterBills)
        views.setTextViewText(R.id.widget_upcoming_bills, snapshot.upcomingBills)
        views.setTextViewText(R.id.widget_status, snapshot.status)
        views.setViewVisibility(R.id.widget_setup_hint, if (snapshot.hasSetup) View.GONE else View.VISIBLE)
        manager.updateAppWidget(widgetId, views)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
