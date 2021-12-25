package com.neuralbit.letsnote

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.neuralbit.letsnote.entities.*
import com.neuralbit.letsnote.utilities.*
import com.teamwork.autocomplete.MultiAutoComplete
import com.teamwork.autocomplete.adapter.AutoCompleteTypeAdapter
import com.teamwork.autocomplete.tokenizer.PrefixTokenizer
import com.teamwork.autocomplete.view.MultiAutoCompleteEditText
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList


class AddEditNoteActivity : AppCompatActivity() ,TagRVInterface,GetTimeFromPicker, GetDateFromPicker, GetTagFromDialog{
    private lateinit var actionBarIcons: List<Int>
    private lateinit var restoreButton: ImageButton
    private lateinit var archiveButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var pinButton: ImageButton
    private lateinit var alertButton: ImageButton
    private lateinit var noteTitleEdit : EditText
    private lateinit var noteDescriptionEdit : MultiAutoCompleteEditText
    private lateinit var addTagBtn : ImageButton
    private lateinit var reminderIcon : ImageView
    private lateinit var reminderTV : TextView
    private var noteID : Long= -1
    private var tagID = -1
    private lateinit var viewModal :NoteViewModel
    private lateinit var noteType : String
    private lateinit var allNotes : List<Note>
    private lateinit var archivedNotes : List<Note>
    private lateinit var pinnedNotes : List<Note>
    private var noteColor : String ? = null
    private val TAG = "AddNoteActivity"
    private var deletable : Boolean = false
    private lateinit var tvTimeStamp : TextView
    private var textChanged : Boolean = false
    private var archived = false
    private lateinit var cm : Common
    private lateinit var noteDesc : String
    private lateinit var coordinatorlayout : View
    private var wordStart = 0
    private var wordEnd = 0
    private var tagString : String ? = null
    private var newTagTyped = false
    private var backPressed  = false
    private lateinit var tagListRV : RecyclerView
    private var isKeyBoardShowing = false
    private var deletedTag = ArrayList<String>()
    private var tagDeleted = false
    private lateinit var tagListAdapter : TagRVAdapter
    private lateinit var alertBottomSheet : BottomSheetDialog
    private lateinit var labelBottomSheet : BottomSheetDialog
    private var pinBtnClicked = false
    private lateinit var labelBtn : ImageButton
    private  var reminder: Reminder? = null
    private lateinit var lifecycleOwner : LifecycleOwner
    private lateinit var calendar: Calendar
    private lateinit var timeTitleTV :TextView
    private lateinit var dateTitleTV :TextView



    override fun onCreate(savedInstanceState: Bundle?)  {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_note)
        noteTitleEdit = findViewById(R.id.noteEditTitle)
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setDisplayShowCustomEnabled(true)
        actionBarIcons = listOf(R.drawable.ic_baseline_arrow_back_24,R.drawable.ic_baseline_archive_24,R.drawable.ic_baseline_restore_24)
        cm= Common()
        supportActionBar?.setCustomView(R.layout.note_action_bar)
        noteDescriptionEdit = findViewById(R.id.noteEditDesc)
        tvTimeStamp = findViewById(R.id.tvTimeStamp)
        tagListRV = findViewById(R.id.tagListRV)
        labelBtn = findViewById(R.id.labelBtn)
        coordinatorlayout = findViewById(R.id.coordinatorlayout)
        alertButton = findViewById(R.id.alertButton)
        addTagBtn = findViewById(R.id.addTagBtn)
        reminderTV = findViewById(R.id.reminderTV)
        reminderIcon = findViewById(R.id.reminderIcon)
        val layoutManager = LinearLayoutManager(applicationContext,LinearLayoutManager.HORIZONTAL,false)
        calendar = Calendar.getInstance()
        layoutManager.orientation = HORIZONTAL
        tagListAdapter= TagRVAdapter(applicationContext,this)
        tagListRV.layoutManager= layoutManager
        tagListRV.adapter = tagListAdapter
        noteType = intent.getStringExtra("noteType").toString()
        lifecycleOwner = this
        viewModal = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(NoteViewModel::class.java)
        viewModal.allNotes.observe(this) {
            allNotes = it
            if(noteType != "Edit"){
                noteID = it.size.toLong() + 1

            }
        }

        manipulateNoteDescLines()

        viewModal.archivedNote.observe(this) {
            archivedNotes = it
        }
        viewModal.pinnedNotes.observe(this) {
            pinnedNotes = it
        }
        KeyboardUtils.addKeyboardToggleListener(this, KeyboardUtils.SoftKeyboardToggleListener {
            onKeyboardVisibilityChanged(it)
        })

        deleteButton = findViewById(R.id.deleteButton)
        backButton = findViewById(R.id.backButton)
        archiveButton = findViewById(R.id.archiveButton)
        restoreButton = findViewById(R.id.restoreButton)
        alertBottomSheet =  BottomSheetDialog(this)
        labelBottomSheet = BottomSheetDialog(this)
        
        addTagBtn.setOnClickListener {
            val addTagDialog = AddTagDialog(this)
            addTagDialog.show(supportFragmentManager,"addTagDialog")
            
        }

        coordinatorlayout = findViewById(R.id.coordinatorlayout)
        pinButton = findViewById(R.id.pinButton)

        archived = intent.getBooleanExtra("archivedNote",false)
        viewModal.archived.value = archived
        var pinnedNote = intent.getBooleanExtra("pinnedNote",false)
        viewModal.pinned.value = pinnedNote
        noteColor = intent.getStringExtra("noteColor")
        if(noteColor!=null){
            setBgColor()
         }
        viewModal.pinned.observe(this){
            pinnedNote = it
            val pN= PinnedNote(noteID)
            var snackbar = Snackbar.make(coordinatorlayout, "Note pinned", Snackbar.LENGTH_LONG)

            if (it){
                pinButton.setImageResource(R.drawable.ic_baseline_push_pin_24)
                viewModal.pinNote(pN)
                if(pinBtnClicked){
                    snackbar.setAction(
                        "UNDO"
                    ) {
                        pinBtnClicked = false
                        viewModal.pinned.value = !pinnedNote
                        viewModal.removePin(PinnedNote(noteID))
                        Toast.makeText(this, "Note unpinned", Toast.LENGTH_SHORT).show()
                    }
                    snackbar.show()
                }


            }else{
                viewModal.removePin(pN)
                pinButton.setImageResource(R.drawable.ic_outline_push_pin_24)
                if(pinBtnClicked){
                    snackbar = Snackbar.make(coordinatorlayout, "Note unpinned", Snackbar.LENGTH_LONG)

                    snackbar.setAction(
                        "UNDO"
                    ) {
                        pinBtnClicked = false
                        viewModal.pinned.value = !pinnedNote
                        viewModal.pinNote(PinnedNote(noteID))
                        Toast.makeText(this, "Note unpinned", Toast.LENGTH_SHORT).show()
                    }
                    snackbar.show()
                }

            }
        }
        viewModal.archived.observe(this){
            if (it){
                archiveButton.setImageResource(R.drawable.ic_baseline_unarchive_24)
            }else{
                archiveButton.setImageResource(R.drawable.ic_outline_archive_24)
            }
        }

        when (noteType) {
            "Edit" -> {
                val noteTitle = intent.getStringExtra("noteTitle")
                noteDesc = intent.getStringExtra("noteDescription").toString()
                val noteTimeStamp = intent.getLongExtra("noteTimeStamp",0)
                tvTimeStamp.text= getString(R.string.timeStamp,cm.convertLongToTime(noteTimeStamp)[0],cm.convertLongToTime(noteTimeStamp)[1])
                tvTimeStamp.visibility =VISIBLE
                noteID = intent.getLongExtra("noteID", -1)
                noteTitleEdit.setText(noteTitle)
                noteDescriptionEdit.setText(noteDesc)
                if(archived) {
                    archiveButton.visibility = GONE
                    restoreButton.visibility = VISIBLE
                }
                lifecycleScope.launch {
                    for (tag in viewModal.getTagsWithNote(noteID).last().tags){
                        viewModal.addTagToList(tag)
                    }
                    tagListAdapter.updateList(viewModal.tagList)

                }
            }
            else -> {
                tvTimeStamp.visibility =GONE
            }
        }


        viewModal.getReminder(noteID).observe(this) {
            Log.d(TAG, "onCreate: $it")
            if(it!=null){
                alertButton.setBackgroundResource(R.drawable.ic_baseline_add_alert_24)
                reminder = it
                reminderTV.visibility =  VISIBLE
                reminderIcon.visibility = VISIBLE
                reminderTV.text = resources.getString(R.string.reminder,cm.convertLongToTime(it.dateTime)[0],cm.convertLongToTime(it.dateTime)[1])

            }else{
                alertButton.setBackgroundResource(R.drawable.ic_outline_add_alert_24)
                reminderTV.visibility =  GONE
                reminderIcon.visibility = GONE
            }

        }

        alertButton.setOnClickListener {
            if (reminder==null){
                showAlertSheetDialog()

            }else{
                viewModal.deleteReminder(reminder!!)
                reminder = null
            }
        }


        viewModal.wordEnd.observe(this) { wordEnd = it }

        viewModal.backPressed.observe(this) { backPressed = it }

        viewModal.wordStart.observe(this) { wordStart = it }

        viewModal.noteDescString.observe(this) { noteDescStr ->
            if (newTagTyped) {

                viewModal.allTags.observe(this) {

                    val adapter: AutoCompleteTypeAdapter<Tag> =
                        AutoCompleteTypeAdapter.Build.from(TagViewBinder(), TagTokenFilter())
                    it?.let { adapter.setItems(it) }
                    val multiAutoComplete = MultiAutoComplete.Builder()
                        .tokenizer(PrefixTokenizer('#'))
                        .addTypeAdapter(adapter)
                        .build()
                    multiAutoComplete.onViewAttached(noteDescriptionEdit)

                    if (noteDescStr.isNotEmpty()) {
                        if (noteDescStr.length >= 2) {

                            if (noteDescStr[noteDescStr.length - 1] == ' ') {
                                val tag = Tag(noteDescStr.substring(0, noteDescStr.length - 1))
                                if (tag !in it) {
                                    viewModal.addTag(tag)
                                }
                                viewModal.addTagToList(tag)
                                tagListAdapter.updateList(viewModal.tagList)
                            }

                        }
                    }


                }


                if (noteDescStr != null) {
                    tagString = noteDescStr

                }
            }

        }

        alertBottomSheet.setOnDismissListener {
            //TODO set bottom sheet On dismiss behaviour
        }

        labelBtn.setOnClickListener {
            showLabelBottomSheetDialog()
        }
        noteTitleEdit.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if(p3>0){
                    viewModal.noteChanged(true)
                    if(!tagListAdapter.deleteIgnored){
                        tagListAdapter.deleteIgnored = true
                        tagListAdapter.notifyDataSetChanged()

                    }
                }

            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })
        noteDescriptionEdit.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                getTagFromString(p0,p3)

            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        noteDescriptionEdit.setOnKeyListener { p0, p1, p2 ->
            viewModal.backPressed.value = p1 == KeyEvent.KEYCODE_DEL
            false
        }


        viewModal.texChanged.observe(this) {
            textChanged = it
        }
        viewModal.deleted.observe(this) {
            deletable = it
        }
        viewModal.archived.observe(this) {
            if (it) {
                pinButton.visibility = GONE
                archiveButton.visibility = GONE
                alertButton.visibility = GONE
                restoreButton.visibility = VISIBLE
            } else {
                pinButton.visibility = VISIBLE
                archiveButton.visibility = VISIBLE
                alertButton.visibility = VISIBLE
                restoreButton.visibility = GONE
            }
        }




        backButton.setOnClickListener {
            goToMain()
        }



        deleteButton.setOnClickListener {
            if(noteType == "Edit"){
                if(archived){
                    val archivedNote = ArchivedNote(noteID)
                    viewModal.removeArchive(archivedNote)
                }
                if(pinnedNote){
                    viewModal.removePin(PinnedNote(noteID))
                }
                for(tag in viewModal.tagList){
                    viewModal.deleteNoteTagCrossRef(NoteTagCrossRef(noteID,tag.tagTitle))
                }
                for ( note in allNotes) {
                    if (note.noteID == noteID) {
                        viewModal.deleteNote(note)
                    }
                }

                Toast.makeText(this,"Note deleted",Toast.LENGTH_SHORT).show()
                viewModal.deleted.value = true
                goToMain()
            }

        }
        archiveButton.setOnClickListener {
            viewModal.archived.value = true
            val archivedNote = ArchivedNote(noteID)
            viewModal.archiveNote(archivedNote)
            val snackbar = Snackbar.make(coordinatorlayout,"Note Achieved",Snackbar.LENGTH_LONG)
            snackbar.setAction("UNDO"
            ) {
                viewModal.archived.value = false
                viewModal.removeArchive(archivedNote)
                Toast.makeText(this,"Note Unarchived", Toast.LENGTH_SHORT).show()
            }
            snackbar.show()
            

        }
        pinButton.setOnClickListener {
            pinBtnClicked = true

            viewModal.pinned.value = !pinnedNote
            Log.d(TAG, "onCreate: $pinnedNote")

            
        }

        restoreButton.setOnClickListener {
            val archivedNote = ArchivedNote(noteID)
            viewModal.removeArchive(archivedNote)
            viewModal.archived.value = false
            Toast.makeText(this,"Note Unarchived", Toast.LENGTH_SHORT).show()


        }
    }

    private fun getTagFromString(p0: CharSequence?, p3: Int) {
        if(p3>0){
            viewModal.noteChanged(true)
            if(!tagListAdapter.deleteIgnored){
                tagListAdapter.deleteIgnored = true
                tagListAdapter.notifyDataSetChanged()

            }

        }
        if(!backPressed){
            tagDeleted = false
            if(p0?.get(p0.length - 1) == '#'){
                viewModal.wordStart.value = p0.length
                viewModal.newTagTyped.value = true
            }

            if(wordStart> 0) {
                viewModal.wordEnd.value = p0?.length
                viewModal.getTagString(p0.toString())

                if(p0?.get(p0.length - 1) == ' '){
                    viewModal.newTagTyped.value = false
                }


            }

        }
    }

    private fun showLabelBottomSheetDialog() {
        labelBottomSheet.setContentView(R.layout.note_label_bottom_sheet)
        labelBottomSheet.show()
        val l1Btn = labelBottomSheet.findViewById<ImageButton>(R.id.l1Btn)
        val l2Btn = labelBottomSheet.findViewById<ImageButton>(R.id.l2Btn)
        val l3Btn = labelBottomSheet.findViewById<ImageButton>(R.id.l3Btn)
        val l4Btn = labelBottomSheet.findViewById<ImageButton>(R.id.l4Btn)
        val l5Btn = labelBottomSheet.findViewById<ImageButton>(R.id.l5Btn)
        val l6Btn = labelBottomSheet.findViewById<ImageButton>(R.id.l6Btn)

        l1Btn?.setOnClickListener {
            coordinatorlayout.setBackgroundColor(resources.getColor(R.color.white))
            viewModal.insertLabel(Label(noteID,1))
            labelBottomSheet.dismiss()
        }

        l2Btn?.setOnClickListener {
            coordinatorlayout.setBackgroundColor(resources.getColor(R.color.Wild_orchid))
            viewModal.insertLabel(Label(noteID,2))
            labelBottomSheet.dismiss()

        }
        l3Btn?.setOnClickListener {
            coordinatorlayout.setBackgroundColor(resources.getColor(R.color.Honeydew))
            viewModal.insertLabel(Label(noteID,3))
            labelBottomSheet.dismiss()

        }

        l4Btn?.setOnClickListener {
            coordinatorlayout.setBackgroundColor(resources.getColor(R.color.English_violet))
            viewModal.insertLabel(Label(noteID,4))
            labelBottomSheet.dismiss()

        }
        l5Btn?.setOnClickListener {
            coordinatorlayout.setBackgroundColor(resources.getColor(R.color.Celadon))
            viewModal.insertLabel(Label(noteID,5))
            labelBottomSheet.dismiss()

        }
        l6Btn?.setOnClickListener {
            coordinatorlayout.setBackgroundColor(resources.getColor(R.color.Apricot))
            viewModal.insertLabel(Label(noteID,6))
            labelBottomSheet.dismiss()
        }
    }

    private fun manipulateNoteDescLines() {
        coordinatorlayout.viewTreeObserver.addOnGlobalLayoutListener { ViewTreeObserver.OnGlobalLayoutListener {
            val r = Rect()
            coordinatorlayout.getWindowVisibleDisplayFrame(r)
            val screenHeight =  coordinatorlayout.rootView.height
            val keypadHeight = screenHeight - r.bottom
            if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                // keyboard is opened
                if (!isKeyBoardShowing) {
                    isKeyBoardShowing = true
                    onKeyboardVisibilityChanged(true)
                }
            }
            else {
                // keyboard is closed
                if (isKeyBoardShowing) {

                    isKeyBoardShowing = false
                    onKeyboardVisibilityChanged(false)
                }
            }

        } }
    }

    private fun showAlertSheetDialog() {
        alertBottomSheet.setContentView(R.layout.alert_bottom_sheet)
        alertBottomSheet.show()
        val c = Calendar.getInstance()
        var currentHr = c.get(Calendar.HOUR_OF_DAY)
        val opt1 = alertBottomSheet.findViewById<View>(R.id.auto1)
        val opt2 = alertBottomSheet.findViewById<View>(R.id.auto2)
        val opt3 = alertBottomSheet.findViewById<View>(R.id.auto3)
        val opt1Desc = alertBottomSheet.findViewById<TextView>(R.id.date1)
        val opt2Desc = alertBottomSheet.findViewById<TextView>(R.id.date2)
        val opt3Desc = alertBottomSheet.findViewById<TextView>(R.id.date3)
        val customDT = alertBottomSheet.findViewById<View>(R.id.customDateTime)
        opt2Desc?.text = resources.getString(R.string.opt2n3Desc,"morning","8:00am")
        opt3Desc?.text = resources.getString(R.string.opt2n3Desc,"evening","6:00pm")


        customDT?.setOnClickListener {
            openDateTimeDialog()
        }
        when(currentHr){
            in 0..4 -> {
                opt1Desc?.text = resources.getString(R.string.opt1Desc,"8:00am")
                opt1?.visibility = VISIBLE

            }
            in 5..8 -> {
                opt1Desc?.text = resources.getString(R.string.opt1Desc,"2:00pm")

                opt1?.visibility = VISIBLE

            }
            in 9..14 ->{
                opt1Desc?.text = resources.getString(R.string.opt1Desc,"6:00pm")
                opt1?.visibility = VISIBLE

            }
            in 15..18 -> {
                opt1Desc?.text = resources.getString(R.string.opt1Desc,"8:00pm")
                opt1?.visibility = VISIBLE

            }
            in 19..23->  opt1?.visibility = GONE

        }

        opt1?.setOnClickListener {
            if(noteDescriptionEdit.length() > 0 || noteTitleEdit.length() >0 ){
                viewModal.texChanged.value = true

            }
            alertBottomSheet.dismiss()
            when(currentHr){
                in 0..4 -> {
                    c[Calendar.HOUR_OF_DAY] = 8
                    c[Calendar.MINUTE] = 0
                    Toast.makeText(this, "Reminder set for today at 8:00 am", Toast.LENGTH_SHORT).show()

                }
                in 5..8 -> {
                    c[Calendar.HOUR_OF_DAY] = 14
                    c[Calendar.MINUTE] = 0
                    Toast.makeText(this, "Reminder set for today at 2:00 pm", Toast.LENGTH_SHORT).show()

                }
                in 9..14 ->{
                    c[Calendar.HOUR_OF_DAY] = 18
                    c[Calendar.MINUTE] = 0
                    Toast.makeText(this, "Reminder set for today at 6:00 pm", Toast.LENGTH_SHORT).show()

                }
                in 15..18 -> {
                    c[Calendar.HOUR_OF_DAY] = 20
                    c[Calendar.MINUTE] = 0
                    Toast.makeText(this, "Reminder set for today at 8:00 pm", Toast.LENGTH_SHORT).show()

                }
            }
            viewModal.insertReminder(Reminder(noteID, c.timeInMillis))
            startAlarm(c)
        }

        opt2?.setOnClickListener {
            alertBottomSheet.dismiss()
            opt2Desc?.text = resources.getString(R.string.opt2n3Desc,"morning","8:00am")
            c.add(Calendar.DAY_OF_MONTH,1)
            c[Calendar.HOUR_OF_DAY] = 8
            c[Calendar.MINUTE] = 0
            if(noteDescriptionEdit.length() > 0 || noteTitleEdit.length() >0 ){
                viewModal.texChanged.value = true

            }
            Toast.makeText(this, "Reminder set for tomorrow at 8:00am", Toast.LENGTH_SHORT).show()

            viewModal.insertReminder(Reminder(noteID, c.timeInMillis))
            startAlarm(c)
            viewModal.getReminder(noteID).observe(this) {r->
                Log.d(TAG, "onCreate: $r")
                if(r!=null){
                    alertButton.setBackgroundResource(R.drawable.ic_baseline_add_alert_24)
                    reminder = r
                    reminderTV.visibility =  VISIBLE
                    reminderIcon.visibility = VISIBLE
                    reminderTV.text = resources.getString(R.string.reminder,cm.convertLongToTime(r.dateTime)[0],cm.convertLongToTime(r.dateTime)[1])

                }else{
                    alertButton.setBackgroundResource(R.drawable.ic_outline_add_alert_24)
                    reminderTV.visibility =  GONE
                    reminderIcon.visibility = GONE
                }

            }
        }
        opt3?.setOnClickListener {
            alertBottomSheet.dismiss()
            c.add(Calendar.DAY_OF_MONTH,1)
            c[Calendar.HOUR_OF_DAY] = 18
            c[Calendar.MINUTE] = 0
            if(noteDescriptionEdit.length() > 0 || noteTitleEdit.length() >0 ){
                viewModal.texChanged.value = true

            }
            viewModal.insertReminder(Reminder(noteID, c.timeInMillis))
            startAlarm(c)
            Toast.makeText(this, "Reminder set for tomorrow at 6:00pm", Toast.LENGTH_SHORT).show()

        }
    }

    private fun onKeyboardVisibilityChanged(b: Boolean) {
        if(b){
            noteDescriptionEdit.maxLines = 11
        }else{
            noteDescriptionEdit.maxLines = 23

        }
    }


    private fun setBgColor(){
        val window = window

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        var colorID= R.color.white
        var textColorID =R.color.black
        var buttonColorID = R.color.white
        when(noteColor) {
            "White" -> {
                colorID = R.color.white
                buttonColorID = Color.BLACK
            }
            "English_violet" -> {
                colorID = R.color.English_violet
                textColorID = Color.WHITE

            }
            "Wild_orchid" -> { colorID = R.color.Wild_orchid }
            "Celadon" -> { colorID = R.color.Celadon }
            "Honeydew" -> { colorID = R.color.Honeydew }
            "Apricot" -> { colorID = R.color.Apricot }
        }

        noteTitleEdit.setTextColor(textColorID)
        noteDescriptionEdit.setTextColor(textColorID)
        tvTimeStamp.setTextColor(textColorID)
//            for ( drawable in actionBarIcons){
//                changeIconColor(buttonColorID,drawable)
//            }
//            window.statusBarColor = resources.getColor(colorID)
//            supportActionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(colorID)))
        coordinatorlayout.setBackgroundColor(resources.getColor(colorID))
    }

    private fun openDateTimeDialog(){
        val alertDialog: AlertDialog? = this?.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setPositiveButton("ok"
                ) { _, _ ->
                    // User clicked OK button
                    if(noteDescriptionEdit.length() > 0 || noteTitleEdit.length() >0 ){
                        viewModal.texChanged.value = true

                    }
                    Toast.makeText(context, "Reminder set", Toast.LENGTH_SHORT).show()

                    viewModal.insertReminder(Reminder(noteID, calendar.timeInMillis))
                    startAlarm(calendar)

                }
                setNegativeButton("cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        // User cancelled the dialog
                    })
                setView(R.layout.custom_datetime_dialog)
                setTitle("Choose date and time")
            }
            builder.create()
        }
        alertDialog?.show()
        val timePickerBtn=alertDialog?.findViewById<View>(R.id.timePickButton)
        val datePickerBtn = alertDialog?.findViewById<ImageButton>(R.id.datePickButton)
        timeTitleTV = alertDialog?.findViewById(R.id.timeTitle)!!
        dateTitleTV = alertDialog.findViewById(R.id.dateTitle)

        timePickerBtn?.setOnClickListener {
            TimePickerFragment(this).show(supportFragmentManager,"timePicker")
        }
        datePickerBtn?.setOnClickListener {
            val newFragment = DatePickerFragment(this,this)
            newFragment.show(supportFragmentManager, "datePicker")
        }



    }

    private fun startAlarm(calendar: Calendar) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlertReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0)
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    private fun changeIconColor(iconColorID : Int, drawable : Int){
        val unwrappedDrawable = AppCompatResources.getDrawable(applicationContext, drawable)
        val wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable!!)
        DrawableCompat.setTint(wrappedDrawable, iconColorID)
    }

    private fun saveNote(){
        val noteTitle = noteTitleEdit.text.toString()
        val noteDescription = noteDescriptionEdit.text.toString()

        val currentDate= cm.currentTimeToLong()
        if(!deletable){
            if(textChanged){
                if (noteTitle.isNotEmpty() || noteDescription.isNotEmpty()){
                    val note = Note(noteTitle,noteDescription,currentDate)
                    if(noteType == "Edit"){
                        note.noteID = noteID
                        viewModal.updateNote(note)
                        Toast.makeText(this,"Note updated .. " , Toast.LENGTH_SHORT).show()

                    }else{
                        viewModal.addNote(note)
                        Toast.makeText(this,"Note added .. " , Toast.LENGTH_SHORT).show()
                        
                    }
                    for(tag in viewModal.tagList){

                        val crossRef = NoteTagCrossRef(noteID,tag.tagTitle)
                        viewModal.insertNoteTagCrossRef(crossRef)
                    }

                    Log.d(TAG, "saveNote: $deletedTag")

                }


            }
        }

    }



    override fun onBackPressed() {
        super.onBackPressed()
        goToMain()

    }


    private fun goToMain() {
        saveNote()

        val intent = Intent(this@AddEditNoteActivity,MainActivity::class.java)
        startActivity(intent)
    }

    

    override fun deleteTag(tag: Tag) {
        viewModal.tagList.remove(tag)
        if (noteType == "Edit"){
            viewModal.deleteNoteTagCrossRef(NoteTagCrossRef(noteID,tag.tagTitle))
        }
        tagListAdapter.updateList(viewModal.tagList)

    }

    override fun getTimeInfo(calendar : Calendar) {
        val timeConverter = DateTimeConverter()
//        reminderTime = timeConverter.fromTime(Time(hour,min,0))!!
        this.calendar[Calendar.HOUR]= calendar[Calendar.HOUR]
        this.calendar[Calendar.MINUTE]= calendar[Calendar.MINUTE]
        this.calendar[Calendar.SECOND]= calendar[Calendar.SECOND]
        timeTitleTV.text="Time set:" + DateFormat.getTimeFormat(this).format(calendar.time)
    }

    override fun getDateInfo(calendar : Calendar) {
        this.calendar[Calendar.DAY_OF_MONTH] = calendar[Calendar.DAY_OF_MONTH]
        this.calendar[Calendar.MONTH] = calendar[Calendar.MONTH]
        this.calendar[Calendar.YEAR] = calendar[Calendar.YEAR]
        dateTitleTV.text="Date set:" + DateFormat.getDateFormat(this).format(calendar.time)

//        reminderDate =dateConverter.fromDate(Date(year, month, day))!!

    }

    override fun getTag(tag: Tag) {
        Log.d(TAG, "getTag: $tag")
        viewModal.addTag(tag)
        viewModal.addTagToList(tag)
        tagListAdapter.updateList(viewModal.tagList)
        viewModal.noteChanged(true)

    }


}