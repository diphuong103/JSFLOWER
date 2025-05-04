package com.example.jsflower.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.denzcoskun.imageslider.models.SlideModel
import com.example.jsflower.MenuBottomSheetFragment
import com.example.jsflower.Model.MenuItem
import com.example.jsflower.R
import com.example.jsflower.adaptar.MenuAdapter
import com.example.jsflower.adapter.PopularAdapter
import com.example.jsflower.databinding.FragmentHomeBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding

    private lateinit var database: FirebaseDatabase
    private lateinit var menuItems: MutableList<MenuItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.viewAllMenu.setOnClickListener {
            val bottomSheetDialog = MenuBottomSheetFragment()
            bottomSheetDialog.show(parentFragmentManager, "Test")
        }

// Lay du lieu hien thi san pham pho bien
        getAndDisplayPopularItems()

        return binding.root


    }

    private fun getAndDisplayPopularItems() {
        database = FirebaseDatabase.getInstance()
        val flowerRef: DatabaseReference = database.reference.child("list")
        menuItems = mutableListOf()

        // laays du lieu menu item tu db
        flowerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (flowerSnapshot in snapshot.children) {
                    val menuItem = flowerSnapshot.getValue(MenuItem::class.java)
                    menuItem?.let {
                        menuItems.add(it)
                    }
                    // lay ngau nhien item tu db
                    randomPopularItems()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })


    }

    private fun randomPopularItems() {
        val index = menuItems.indices.toList().shuffled()
        val numItemToShow = 6
        val subsetMenuItems = index.take(numItemToShow).map {
            menuItems[it]
        }
        setPopularItemAdapter(subsetMenuItems)

    }

    private fun setPopularItemAdapter(subsetMenuItems: List<MenuItem>) {
        val adapter = MenuAdapter(subsetMenuItems, requireContext())
        binding.PupulerRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.PupulerRecyclerView.adapter = adapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
// slider banner home_activity
        val imageList = ArrayList<SlideModel>()
        imageList.add(SlideModel(R.drawable.bannerlanghoadep, ScaleTypes.FIT))
        imageList.add(SlideModel(R.drawable.banner_jsflower, ScaleTypes.FIT))
        imageList.add(SlideModel(R.drawable.banner_js, ScaleTypes.FIT))

        val imageSlider = binding.imageSlider
        imageSlider.setImageList(imageList)
        imageSlider.setImageList(imageList, ScaleTypes.FIT)

        imageSlider.setItemClickListener(object : ItemClickListener {
            override fun doubleClick(position: Int) {
                TODO("Not yet implemented")
            }

            override fun onItemSelected(position: Int) {
                val itemPosition = imageList[position]
                val itemMessage = "Selected Image $position"
                Toast.makeText(requireContext(), itemMessage, Toast.LENGTH_SHORT).show()
            }
        })

        binding.PupulerRecyclerView.addItemDecoration(PopularAdapter.VerticalSpaceItemDecoration(32))

    }
}
