package com.devekene.muzik.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        EqPresetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MuzikDatabase : RoomDatabase() {
    abstract fun muzikDao(): MuzikDao

    companion object {
        @Volatile
        private var INSTANCE: MuzikDatabase? = null

        fun getInstance(context: Context): MuzikDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MuzikDatabase::class.java,
                    "muzik_studio.db"
                ).build().also { db ->
                    INSTANCE = db
                    CoroutineScope(Dispatchers.IO).launch {
                        seedBuiltInEqPresetsIfEmpty(db.muzikDao())
                    }
                }
            }
        }

        private suspend fun seedBuiltInEqPresetsIfEmpty(dao: MuzikDao) {
            if (dao.getEqPresetCount() == 0) {
                dao.insertEqPresets(
                    listOf(
                        EqPresetEntity(
                            name = "Studio Flat",
                            bandLevelsCsv = "0,0,0,0,0",
                            bassBoostStrength = 0,
                            virtualizerStrength = 0,
                            isBuiltIn = true
                        ),
                        EqPresetEntity(
                            name = "Warm Bass",
                            bandLevelsCsv = "550,350,0,-100,150",
                            bassBoostStrength = 480,
                            virtualizerStrength = 180,
                            isBuiltIn = true
                        ),
                        EqPresetEntity(
                            name = "Acoustic Clarity",
                            bandLevelsCsv = "150,100,250,450,500",
                            bassBoostStrength = 120,
                            virtualizerStrength = 320,
                            isBuiltIn = true
                        ),
                        EqPresetEntity(
                            name = "Vocal Presence",
                            bandLevelsCsv = "-200,100,550,400,150",
                            bassBoostStrength = 80,
                            virtualizerStrength = 240,
                            isBuiltIn = true
                        ),
                        EqPresetEntity(
                            name = "Electronic Punch",
                            bandLevelsCsv = "600,250,-150,350,550",
                            bassBoostStrength = 550,
                            virtualizerStrength = 420,
                            isBuiltIn = true
                        ),
                        EqPresetEntity(
                            name = "Hi-Res Classical",
                            bandLevelsCsv = "250,100,-50,250,450",
                            bassBoostStrength = 100,
                            virtualizerStrength = 500,
                            isBuiltIn = true
                        ),
                        EqPresetEntity(
                            name = "Late Night",
                            bandLevelsCsv = "300,150,200,100,-150",
                            bassBoostStrength = 200,
                            virtualizerStrength = 120,
                            isBuiltIn = true
                        )
                    )
                )
            }
        }
    }
}
