package com.xptlabs.varliktakibi.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Add purchaseRate column to assets table
        // Default to purchasePrice for existing records
        database.execSQL(
            "ALTER TABLE assets ADD COLUMN purchaseRate REAL NOT NULL DEFAULT 0.0"
        )

        // Update existing records: set purchaseRate = purchasePrice
        database.execSQL(
            "UPDATE assets SET purchaseRate = purchasePrice"
        )

        // 2. Create asset_price_history table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS asset_price_history (
                id TEXT PRIMARY KEY NOT NULL,
                assetId TEXT NOT NULL,
                assetType TEXT NOT NULL,
                date INTEGER NOT NULL,
                price REAL NOT NULL,
                amount REAL NOT NULL,
                totalValue REAL NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Create indices for asset_price_history
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_asset_price_history_assetId_date ON asset_price_history(assetId, date)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_asset_price_history_assetType ON asset_price_history(assetType)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_asset_price_history_date ON asset_price_history(date)"
        )

        // 3. Create asset_transaction_history table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS asset_transaction_history (
                id TEXT PRIMARY KEY NOT NULL,
                assetId TEXT NOT NULL,
                assetType TEXT NOT NULL,
                date INTEGER NOT NULL,
                transactionType TEXT NOT NULL,
                amount REAL NOT NULL,
                totalAmount REAL NOT NULL,
                price REAL NOT NULL,
                totalValue REAL NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Create indices for asset_transaction_history
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_asset_transaction_history_assetId ON asset_transaction_history(assetId)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_asset_transaction_history_assetType ON asset_transaction_history(assetType)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_asset_transaction_history_date ON asset_transaction_history(date)"
        )
    }
}
