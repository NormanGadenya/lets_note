package com.neuralbit.letsnote

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neuralbit.letsnote.databinding.ActivityMainBinding
import com.neuralbit.letsnote.ui.allNotes.AllNotesViewModel
import com.neuralbit.letsnote.ui.archived.ArchivedViewModel
import com.neuralbit.letsnote.ui.tag.TagViewModel


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"
    private val allNotesViewModal : AllNotesViewModel by viewModels()
    private val archivedViewModel : ArchivedViewModel by viewModels()
    private val tagViewModel : TagViewModel by viewModels()
    private lateinit var mAuth: FirebaseAuth
    var fUser : FirebaseUser? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        fUser= mAuth.currentUser
        if (fUser == null) {

            val intent = Intent(applicationContext, SignInActivity::class.java)
            startActivity(intent)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_arch , R.id.nav_tags ,R.id.nav_labels , R.id.nav_deleted , R.id.nav_settings
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        val profileUrl = fUser?.photoUrl
        val headerLayout = navView.getHeaderView(0)
        val profileIV = headerLayout.findViewById<ImageView>(R.id.profilePic)
        val nameTV = headerLayout.findViewById<TextView>(R.id.accountName)
        val emailTV = headerLayout.findViewById<TextView>(R.id.emailAddress)
//        val navController.navigate(R.id.profilePic)
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


    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.main_activity2, menu)
        val searchViewMenuItem = menu.findItem(R.id.search)
        val searchView = searchViewMenuItem.actionView as SearchView
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
        return true
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}