package com.roxxer.zenith

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favoritos")
data class FavoritoEntity(
    @PrimaryKey val idCancion: String
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val idPlaylist: Int = 0,
    val nombre: String
)

@Entity(tableName = "playlist_canciones", primaryKeys = ["playlistId", "cancionUri"])
data class PlaylistCancionCrossRef(
    val playlistId: Int,
    val cancionUri: String
)

@Dao
interface FavoritosDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun agregarFavorito(favorito: FavoritoEntity)

    @Delete
    fun quitarFavorito(favorito: FavoritoEntity)

    @Query("SELECT idCancion FROM favoritos")
    fun obtenerTodosLosFavoritos(): Flow<List<String>>
}

@Dao
interface PlaylistsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun crearPlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists")
    fun obtenerTodasLasPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun agregarCancionAPlaylist(cruce: PlaylistCancionCrossRef)

    @Query("SELECT cancionUri FROM playlist_canciones WHERE playlistId = :idPlaylist")
    fun obtenerCancionesDePlaylist(idPlaylist: Int): Flow<List<String>>

    // --- NUEVOS PODERES: Renombrar ---
    @Query("UPDATE playlists SET nombre = :nuevoNombre WHERE idPlaylist = :idPlaylist")
    fun actualizarNombrePlaylist(idPlaylist: Int, nuevoNombre: String)

    @Query("DELETE FROM playlist_canciones WHERE playlistId = :idPlaylist AND cancionUri = :cancionUri")
    fun eliminarCancionDePlaylist(idPlaylist: Int, cancionUri: String)

    @Query("DELETE FROM playlists WHERE idPlaylist = :idPlaylist")
    fun eliminarPlaylist(idPlaylist: Int)

    @Query("DELETE FROM playlist_canciones WHERE playlistId = :idPlaylist")
    fun vaciarPlaylist(idPlaylist: Int)

    @Query("SELECT playlistId FROM playlist_canciones WHERE cancionUri = :cancionUri")
    fun obtenerPlaylistsDeCancion(cancionUri: String): Flow<List<Int>>
}

@Database(entities = [FavoritoEntity::class, PlaylistEntity::class, PlaylistCancionCrossRef::class], version = 3, exportSchema = false)
abstract class ZenithDatabase : RoomDatabase() {

    abstract fun favoritosDao(): FavoritosDao
    abstract fun playlistsDao(): PlaylistsDao

    companion object {
        @Volatile private var INSTANCE: ZenithDatabase? = null
        fun getDatabase(context: Context): ZenithDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, ZenithDatabase::class.java, "boveda_zenith")
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}