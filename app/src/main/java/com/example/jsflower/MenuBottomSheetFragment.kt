package com.example.jsflower

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jsflower.Model.MenuItem
import com.example.jsflower.adaptar.MenuAdapter
import com.example.jsflower.databinding.FragmentMenuBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MenuBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentMenuBottomSheetBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var menuItems: MutableList<MenuItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMenuBottomSheetBinding.inflate(inflater, container, false)

        binding.buttonBack.setOnClickListener {
            dismiss()
        }
        getMenuItems()

        return binding.root
    }

    private fun getMenuItems() {
        database = FirebaseDatabase.getInstance()
        val flowerRef : DatabaseReference = database.reference.child("list")
        menuItems = mutableListOf()

        flowerRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (flowerSnapshot in snapshot.children)
                {
                    val menuItem = flowerSnapshot.getValue(MenuItem::class.java)
                    menuItem?.let{
                        menuItems.add(it)
                    }
                    // Sau khi nhận được dữ liệu, hãy thiết lập bộ điều hợp adpater
                    setAdapter()
                }

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun setAdapter() {
        val adapter = MenuAdapter(menuItems, requireContext())
        binding.menuRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.menuRecyclerView.adapter = adapter
    }

    companion object {


    }

}