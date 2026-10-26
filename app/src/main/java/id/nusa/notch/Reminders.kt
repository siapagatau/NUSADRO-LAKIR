package id.nusa.notch

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object Reminders {
    const val CH_SERVICE = "nusa_service"
    const val CH_REMINDER = "nusa_reminders"

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_SERVICE, "Layanan Nusa", NotificationManager.IMPORTANCE_MIN)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDER, "Pengingat", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun reqCode(id: Long): Int = (id and 0x7fffffff).toInt()

    private fun pending(ctx: Context, id: Long, title: String, flags: Int): PendingIntent? {
        val i = Intent(ctx, ReminderReceiver::class.java)
            .putExtra("id", id)
            .putExtra("title", title)
        return PendingIntent.getBroadcast(ctx, reqCode(id), i, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(ctx: Context, t: Task) {
        val due = t.dueAt ?: return
        if (t.done || due <= System.currentTimeMillis()) return
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, t.id, t.title, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pi)
    }

    fun cancel(ctx: Context, id: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, id, "", PendingIntent.FLAG_NO_CREATE) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    fun rescheduleAll(ctx: Context) {
        Store.tasks.value.forEach { schedule(ctx, it) }
    }

    fun notify(ctx: Context, id: Int, title: String, text: String) {
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CH_REMINDER)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(id, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.ensureChannels(context)
        val id = intent.getLongExtra("id", 0L)
        val title = intent.getStringExtra("title") ?: "Pengingat"
        Reminders.notify(context, (id and 0x7fffffff).toInt(), "Pengingat tugas", title)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context)
        Reminders.rescheduleAll(context)
    }
}
