package com.example.jsflower

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jsflower.adapter.MenuAdapter
import com.example.jsflower.databinding.FragmentMenuBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MenuBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentMenuBottomSheetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       binding =  FragmentMenuBottomSheetBinding.inflate(inflater, container, false)

        binding.buttonBack.setOnClickListener{
            dismiss()
        }

        val menuFlowerNames = listOf("Hoa hồng", "Hoa hướng dương", "Hoa xuyến chi", "Hoa hồng", "Hoa hướng dương", "Hoa xuyến chi")
        val menuItemPrices = listOf("19.999 VND", "299.999 VND", "999.999 VND", "19.999 VND", "299.999 VND", "999.999 VND")
        val menuImages = listOf(
            R.drawable.hoaly_,
            R.drawable.hoababy_,
            R.drawable.hoalandiep_,
            R.drawable.hoaly_,
            R.drawable.hoababy_,
            R.drawable.hoalandiep_
        )

        val adapter = MenuAdapter(
            ArrayList(menuFlowerNames),
            ArrayList(menuItemPrices),
            ArrayList(menuImages)
        )

        binding.menuRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.menuRecyclerView.adapter = adapter

        return binding.root
    }

    companion object {
    }

}