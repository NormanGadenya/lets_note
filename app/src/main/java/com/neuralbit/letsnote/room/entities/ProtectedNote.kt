package com.neuralbit.letsnote.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class ProtectedNote(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "noteID") var noteID: Long
)