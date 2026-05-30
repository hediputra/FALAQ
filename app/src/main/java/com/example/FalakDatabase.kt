package com.example

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [FalakLocation::class], version = 1, exportSchema = false)
abstract class FalakDatabase : RoomDatabase() {
    abstract fun falakLocationDao(): FalakLocationDao

    companion object {
        @Volatile
        private var INSTANCE: FalakDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FalakDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FalakDatabase::class.java,
                    "falak_database"
                )
                .addCallback(FalakDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class FalakDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.falakLocationDao()
                    // Prepopulate standard default locations
                    dao.insertLocation(
                        FalakLocation(
                            name = "Padepokan Pasantren An-Nirwana (Banten)",
                            latitude = -6.9463,
                            longitude = 106.0150,
                            altitude = 20.0,
                            timeZone = 7.0,
                            isDefault = true
                        )
                    )
                    dao.insertLocation(
                        FalakLocation(
                            name = "Puncak Sosok (Bantul, DIY)",
                            latitude = -7.8763,
                            longitude = 110.3981,
                            altitude = 120.0,
                            timeZone = 7.0,
                            isDefault = false
                        )
                    )
                    dao.insertLocation(
                        FalakLocation(
                            name = "Condrodipo Observatory (Gresik, Jatim)",
                            latitude = -7.1683,
                            longitude = 112.6075,
                            altitude = 120.0,
                            timeZone = 7.0,
                            isDefault = false
                        )
                    )
                    dao.insertLocation(
                        FalakLocation(
                            name = "Lhok Nga Baitul Hilal (Aceh)",
                            latitude = 5.4744,
                            longitude = 95.2322,
                            altitude = 15.0,
                            timeZone = 7.0,
                            isDefault = false
                        )
                    )
                }
            }
        }
    }
}
