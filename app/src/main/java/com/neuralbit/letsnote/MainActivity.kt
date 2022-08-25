package com.neuralbit.letsnote

import android.annotation.TargetApi
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neuralbit.letsnote.databinding.ActivityMainBinding
import com.neuralbit.letsnote.ui.allNotes.AllNotesViewModel
import com.neuralbit.letsnote.ui.archived.ArchivedViewModel
import com.neuralbit.letsnote.ui.deletedNotes.DeletedNotesViewModel
import com.neuralbit.letsnote.ui.settings.SettingsViewModel
import com.neuralbit.letsnote.ui.tag.TagViewModel
import com.neuralbit.letsnote.utilities.AlertReceiver


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"
    private val allNotesViewModal : AllNotesViewModel by viewModels()
    private val archivedViewModel : ArchivedViewModel by viewModels()
    private val settingsViewModel : SettingsViewModel by viewModels()
    private val deleteVieModel : DeletedNotesViewModel by viewModels()
    private lateinit var viewModal : MainActivityViewModel
    private val tagViewModel : TagViewModel by viewModels()
    private lateinit var mAuth: FirebaseAuth
    private var actionMode : ActionMode? = null
    private var fUser : FirebaseUser? = null
    private lateinit var settingsPref: SharedPreferences
    val lifecycleOwner = this



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        fUser= mAuth.currentUser
        if (fUser == null) {
            val intent = Intent(applicationContext, SignInActivity::class.java)
            startActivity(intent)
        }
        if (Build.VERSION.SDK_INT >= 25) {
            createShortcut();
        }else{
            removeShortcuts();
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        settingsPref =  getSharedPreferences("Settings", MODE_PRIVATE)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_arch , R.id.nav_tags ,R.id.nav_labels , R.id.nav_deleted , R.id.nav_settings
            ), drawerLayout
        )
        viewModal = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(MainActivityViewModel::class.java)
        allNotesViewModal.itemSelectEnabled.observe(this){
            if (it){
                actionMode = startSupportActionMode(MActionModeCallBack())
            }else{
                actionMode?.finish()

            }
        }
        viewModal.getAllFireNotes().observe(this){
            allNotesViewModal.allFireNotes.value = it
        }

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val profileUrl = fUser?.photoUrl
        val headerLayout = navView.getHeaderView(0)
        val profileIV = headerLayout.findViewById<ImageView>(R.id.profilePic)
        val nameTV = headerLayout.findViewById<TextView>(R.id.accountName)
        val emailTV = headerLayout.findViewById<TextView>(R.id.emailAddress)
        if(profileUrl != null){
            Glide.with(applicationContext).load(profileUrl).into(profileIV)
        }

        val name = fUser?.displayName
        if (name != null){
            nameTV.text = name
        }

        val emailAdd = fUser?.email
        if (emailAdd != null){
            emailTV.text = emailAdd
        }

    }

    @TargetApi(25)
    private fun createShortcut() {
//        val sM = getSystemService(ShortcutManager::class.java)
//        val intent1 = Intent(applicationContext, AddEditNoteActivity::class.java)
//        intent1.action = Intent.ACTION_VIEW
//        val shortcut1 = ShortcutInfo.Builder(this, "newNote")
//            .setIntent(intent1)
//            .setShortLabel(getString(R.string.shortcut_short_label))
//            .setLongLabel(getString(R.string.shortcut_long_label))
//            .setIcon(Icon.createWithResource(this, R.mipmap.shortcut_icon))
//            .build()
//        sM.dynamicShortcuts = listOf(shortcut1)

        val intent = Intent(applicationContext, AddEditNoteActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        val shortcutInfo = ShortcutInfoCompat.Builder(applicationContext, "newNote")
            .setShortLabel(getString(R.string.shortcut_short_label))
            .setLongLabel(getString(R.string.shortcut_long_label))
            .setIntent(intent) // Push the shortcut
            .build()

// Push the shortcut
        ShortcutManagerCompat.pushDynamicShortcut(applicationContext, shortcutInfo)
    }

    @TargetApi(25)
    private fun removeShortcuts() {
        val shortcutManager = getSystemService(ShortcutManager::class.java)
        shortcutManager.disableShortcuts(listOf("newNote"))
        shortcutManager.removeAllDynamicShortcuts()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_activity2, menu)
        val searchViewMenuItem = menu.findItem(R.id.search)
        val layoutViewBtn = menu.findItem(R.id.layoutStyle)
        val signOutButton = menu.findItem(R.id.signOut)
        val deleteButton = menu.findItem(R.id.trash)
        allNotesViewModal.deleteFrag.observe(this){
            deleteButton.isVisible = it
        }
        settingsViewModel.settingsFrag.observe(this){
            searchViewMenuItem.isVisible = !it
            layoutViewBtn.isVisible = !it
        }


        val searchView = searchViewMenuItem.actionView as SearchView
        val searchIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_button)
        searchIcon.setImageDrawable(ContextCompat.getDrawable(applicationContext,R.drawable.ic_baseline_search_24))
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                if (p0 != null) {
                    allNotesViewModal.searchQuery.value = p0
                    archivedViewModel.searchQuery.value = p0
                    tagViewModel.searchQuery.value = p0
                }
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                if (p0 != null) {
                    allNotesViewModal.searchQuery.value = p0
                    archivedViewModel.searchQuery.value = p0
                    tagViewModel.searchQuery.value = p0

                }
                return false
            }
        })


        deleteButton.setOnMenuItemClickListener {
            if (deleteVieModel.deletedNotes.isNotEmpty()){
                val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
                alertDialog.setTitle("Are you sure about this ?")
                alertDialog.setPositiveButton("Yes"
                ) { _, _ ->
                    deleteVieModel.clearTrash.value = true
                }
                alertDialog.setNegativeButton("Cancel"
                ) { dialog, _ -> dialog.cancel() }
                alertDialog.show()
            }
            return@setOnMenuItemClickListener true
        }

        layoutViewBtn.setOnMenuItemClickListener {
            allNotesViewModal.staggeredView.value = !allNotesViewModal.staggeredView.value!!
            return@setOnMenuItemClickListener true
        }

        signOutButton.setOnMenuItemClickListener {
            val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
            alertDialog.setTitle("Are you sure about this ?")
            alertDialog.setPositiveButton("Yes"
            ) { _, _ ->
                allNotesViewModal.getAllFireNotes().observe(this){
                    for ( note in it){
                        cancelAlarm(note.reminderDate.toInt())
                    }
                }


                AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener{
                        val intent = Intent(this@MainActivity,SignInActivity::class.java)
                        startActivity(intent)
                    }
            }

            alertDialog.setNegativeButton("Cancel"
            ) { dialog, _ -> dialog.cancel() }
            alertDialog.show()
            return@setOnMenuItemClickListener true
        }
        allNotesViewModal.staggeredView.observe(this){
            if (layoutViewBtn != null){
                if (it){
                    layoutViewBtn.setIcon(R.drawable.baseline_format_list_bulleted_24)
                }else{
                    layoutViewBtn.setIcon(R.drawable.baseline_grid_view_24)

                }
            }
        }
        val emptyTrashImmediately = settingsPref.getBoolean("EmptyTrashImmediately",false)
        if (emptyTrashImmediately){
            allNotesViewModal.notesToDelete.observe(lifecycleOwner){
                it.noteUid?.let { uid -> viewModal.deleteNote(uid,it.label,it.tags) }

            }
            allNotesViewModal.selectedNotes.clear()
        }

        return true
    }

    override fun onBackPressed() {
        val a = Intent(Intent.ACTION_MAIN)
        a.addCategory(Intent.CATEGORY_HOME)
        a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(a)
    }
    private inner class MActionModeCallBack : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.action_menu,menu)
            mode?.title = "Delete or Archive notes"
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val restoreItem = menu?.findItem(R.id.restore)
            restoreItem?.isVisible = allNotesViewModal.deleteFrag.value == true
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (item?.itemId == R.id.archive ){
                allNotesViewModal.itemArchiveClicked.value = true
                mode?.finish()
                return true
            }else if (item?.itemId == R.id.delete){
                allNotesViewModal.itemDeleteClicked.value = true

                mode?.finish()
                return true
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            allNotesViewModal.itemSelectEnabled.value = false
            allNotesViewModal.selectedNotes.clear()
            actionMode = null
        }

    }

    private fun cancelAlarm(reminder : Int){
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlertReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, reminder, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}