package com.sportchronoclock.ride

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sportchronoclock.db.RideDatabase

actual class SqlDriverFactory(private val context: Context) {
    actual fun create(): SqlDriver =
        AndroidSqliteDriver(RideDatabase.Schema, context, "rides.db")
}
