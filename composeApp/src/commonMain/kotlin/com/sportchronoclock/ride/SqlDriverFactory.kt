package com.sportchronoclock.ride

import app.cash.sqldelight.db.SqlDriver

expect class SqlDriverFactory {
    fun create(): SqlDriver
}
