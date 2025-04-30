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
import com.example.jsflower.R
import com.example.jsflower.adapter.PopularAdapter
import com.example.jsflower.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)

         binding.viewAllMenu.setOnClickListener{
             val bottomSheetDialog= MenuBottomSheetFragment()
             bottomSheetDialog.show(parentFragmentManager, "Test")
         }
        return binding.root


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
        val flowerName = listOf("Hoa Hong", "Hoa huong duong", "Hoa xuyen chi")
        val Price = listOf("10 vnd", "20 vnd", "999 vnd")
        val populerFoodImages = listOf(R.drawable.hoaly_, R.drawable.hoalandiep_, R.drawable.hoababy_)

        val adapter = PopularAdapter(flowerName, Price, populerFoodImages, requireContext())
binding.PupulerRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.PupulerRecyclerView.adapter = adapter

        binding.PupulerRecyclerView.addItemDecoration(PopularAdapter.VerticalSpaceItemDecoration(32))

    }
}
