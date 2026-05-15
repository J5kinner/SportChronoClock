package com.sportchronoclock.ride

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.sportchronoclock.db.RideDatabase

actual class SqlDriverFactory {
    actual fun create(): SqlDriver =
        NativeSqliteDriver(RideDatabase.Schema, "rides.db")
}
