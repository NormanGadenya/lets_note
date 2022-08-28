package com.neuralbit.letsnote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.neuralbit.letsnote.entities.NoteFire
import com.neuralbit.letsnote.repos.LabelFireRepo
import com.neuralbit.letsnote.repos.NoteFireRepo
import com.neuralbit.letsnote.repos.TagFireRepo

class LabelNotesViewModel(
    application: Application) :AndroidViewModel(application){
    val searchQuery: MutableLiveData<String> = MutableLiveData()
    var selectedNotes = HashSet<NoteFire>()
    var labelNotes = ArrayList<NoteFire>()
    var noteUids = ArrayList<String>()
    private val noteFireRepo : NoteFireRepo = NoteFireRepo()
    private val labelFireRepo : LabelFireRepo = LabelFireRepo()
    private val tagFireRepo : TagFireRepo = TagFireRepo()

    fun deleteNote (noteUid : String, labelColor : Int, tagList : List<String> ){
        noteFireRepo.deleteNote(noteUid)
        tagFireRepo.deleteNoteFromTags(tagList,noteUid)
        labelFireRepo.deleteNoteFromLabel(labelColor,noteUid)
    }
}