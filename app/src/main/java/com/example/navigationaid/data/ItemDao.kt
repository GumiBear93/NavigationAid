package com.example.navigationaid.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(placeItem: PlaceItem)
    @Update
    suspend fun update(placeItem: PlaceItem)
    @Delete
    suspend fun delete(placeItem: PlaceItem)
    @Query("SELECT * FROM placeItem WHERE id = :id")
    fun getPlaceItem(id: Int): Flow<PlaceItem>
    @Query("SELECT * FROM placeItem ORDER BY id ASC")
    fun getPlaceItems(): Flow<List<PlaceItem>>
}