package com.neuralbit.letsnote.ui.label

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.neuralbit.letsnote.ui.addEditNote.NoteViewModel
import com.neuralbit.letsnote.ui.adapters.LabelRVAdapter
import com.neuralbit.letsnote.databinding.LabelFragmentBinding
import com.neuralbit.letsnote.entities.LabelFire
import com.neuralbit.letsnote.ui.allNotes.AllNotesViewModel
import com.neuralbit.letsnote.ui.settings.SettingsViewModel
import java.util.*

class LabelFragment : Fragment(), LabelRVAdapter.LabelClick {
    private val noteViewModel: NoteViewModel by activityViewModels()
    private val labelViewModel: LabelViewModel by activityViewModels()
    private val allNotesViewModel: AllNotesViewModel by activityViewModels()
    private val settingsViewModel: SettingsViewModel by activityViewModels()
    private var _binding:LabelFragmentBinding ? = null
    private lateinit var labelRV:RecyclerView
    private val binding get() = _binding!!
    val TAG = "LabelFragment"



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = LabelFragmentBinding.inflate(inflater,container,false)
        val root: View = binding.root
        labelRV = binding.noteLabelRV
        val labelRVAdapter = context?.let { LabelRVAdapter(it,this) }
        labelRV.adapter= labelRVAdapter
        val settingsSharedPref = context?.getSharedPreferences("Settings", AppCompatActivity.MODE_PRIVATE)
        val staggeredLayoutManagerAll = StaggeredGridLayoutManager( 2,LinearLayoutManager.VERTICAL)
        labelRV.layoutManager = staggeredLayoutManagerAll
        allNotesViewModel.deleteFrag.value = false
        settingsViewModel.settingsFrag.value = false
        allNotesViewModel.staggeredView.value = settingsSharedPref?.getBoolean("staggered",true)
        allNotesViewModel.staggeredView.observe(viewLifecycleOwner){
            val editor: SharedPreferences.Editor ?= settingsSharedPref?.edit()
            editor?.putBoolean("staggered",it)
            editor?.apply()
            if (it){
                labelRV.layoutManager = staggeredLayoutManagerAll
            }else{

                labelRV.layoutManager = LinearLayoutManager(context)
            }
        }

        labelViewModel.searchQuery.observe(viewLifecycleOwner){
            if(it!=null){
                labelRVAdapter?.updateLabelList(filterLabels(it))

            }else{
                labelRVAdapter?.updateLabelList(labelViewModel.labelList)
            }
        }

        noteViewModel.allFireLabels().observe(viewLifecycleOwner){
            val pref = context?.getSharedPreferences("DeletedNotes", AppCompatActivity.MODE_PRIVATE)
            val deletedNotes = pref?.getStringSet("noteUids", HashSet())
            val labelList = HashSet<Label>()
            val labelFireList = HashSet<LabelFire>()
            allNotesViewModel.allFireNotes.observe(viewLifecycleOwner){ allNotes ->
                
                val archivedNotes = ArrayList<String>()
                for ( n in allNotes){
                    if (n.archived){
                        n.noteUid?.let { it1 -> archivedNotes.add(it1) }
                    }
                }
                for ( l in it){
                    val label = Label(l.labelColor,l.noteUids.size,l.labelTitle)

                    for ( n in l.noteUids){
                        if(archivedNotes.contains(n) || deletedNotes?.contains(n) == true){
                            label.labelCount-=1
                        }
                        if (!archivedNotes.contains(n)){
                            if (deletedNotes != null){

                                if (!deletedNotes.contains(n)){
                                    labelList.add(label)
                                    labelFireList.add(l)
                                }


                            }else{
                                labelList.add(label)
                                labelFireList.add(l)
                            }
                        }

                    }

                }
                val sortedLabelList = labelList.sortedBy { i -> i.labelCount }.reversed()
                val sortedLabelFireList = labelFireList.sortedBy { i -> i.noteUids.size }.reversed()
                val welcomeIcon = binding.welcomeIcon
                val welcomeText = binding.allNotesText
                if (sortedLabelList.isEmpty()){
                    welcomeIcon.visibility = View.VISIBLE
                    welcomeText.visibility = View.VISIBLE
                }else{
                    welcomeIcon.visibility = View.GONE
                    welcomeText.visibility = View.GONE
                }
                labelViewModel.labelFire = sortedLabelFireList
                labelViewModel.labelList = ArrayList(sortedLabelList)
                labelRVAdapter?.updateLabelList(ArrayList(sortedLabelList))
            }

        }

        return root
    }

    private fun filterLabels(text: String): ArrayList<Label> {
        val newList = ArrayList<Label>()
        val textLower= text.lowercase(Locale.ROOT)
        for ( l in labelViewModel.labelList){
            if (l.labelTitle != null){
                if(l.labelTitle!!.lowercase(Locale.ROOT).contains(textLower)){
                    newList.add(l)
                }
            }
        }

        return newList
    }

    override fun onLabelClick(labelColor: Int) {
        for (label in labelViewModel.labelFire){
            if (label.labelColor == labelColor){
                val intent = Intent(context, LabelNotesActivity::class.java)
                intent.putExtra("labelColor",labelColor)
                intent.putStringArrayListExtra("noteUids", label.noteUids)
                intent.putExtra("labelTitle",label.labelTitle)

                startActivity(intent)
            }
        }
    }


}