package com.example.jsflower.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jsflower.R
import com.example.jsflower.adapter.MenuAdapter
import com.example.jsflower.databinding.FragmentSearchBinding
import androidx.appcompat.widget.SearchView  // Ensure correct import

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var adapter: MenuAdapter

    private val origalMenuFlowerName = listOf("Hoa hồng", "Hoa hướng dương", "Hoa xuyến chi", "Hoa hồng", "Hoa hướng dương", "Hoa xuyến chi")
    private val origalMenuItemPrices = listOf("19.999 VND", "299.999 VND", "999.999 VND", "19.999 VND", "299.999 VND", "999.999 VND")
    private val origalMenuImages = listOf(
        R.drawable.hoaly_,
        R.drawable.hoababy_,
        R.drawable.hoalandiep_,
        R.drawable.hoaly_,
        R.drawable.hoababy_,
        R.drawable.hoalandiep_
    )

    private val filterMenuFlowerName = mutableListOf<String>()
    private val filterMenuItemPrice = mutableListOf<String>()
    private val filterMenuImage = mutableListOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        adapter = MenuAdapter(filterMenuFlowerName, filterMenuItemPrice, filterMenuImage, requireContext())

        binding.menuRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.menuRecyclerView.adapter = adapter

        // Setup search view
        setupSearchView()

        // Show all menu
        showAllMenu()

        return binding.root
    }

    private fun showAllMenu() {
        filterMenuFlowerName.clear()
        filterMenuItemPrice.clear()
        filterMenuImage.clear()

        filterMenuFlowerName.addAll(origalMenuFlowerName)
        filterMenuItemPrice.addAll(origalMenuItemPrices)
        filterMenuImage.addAll(origalMenuImages)

        adapter.notifyDataSetChanged()
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterMenuItems(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterMenuItems(newText)
                return true
            }
        })
    }

    private fun filterMenuItems(query: String?) {
        filterMenuFlowerName.clear()
        filterMenuItemPrice.clear()
        filterMenuImage.clear()

        if (query != null) {
            origalMenuFlowerName.forEachIndexed { index, flowerName ->
                if (flowerName.contains(query, ignoreCase = true)) {
                    filterMenuFlowerName.add(flowerName)
                    filterMenuItemPrice.add(origalMenuItemPrices[index])
                    filterMenuImage.add(origalMenuImages[index])
                }
            }
        }

        adapter.notifyDataSetChanged()
    }

    companion object {
        // You can add any static variables or methods here if needed.
    }
}
