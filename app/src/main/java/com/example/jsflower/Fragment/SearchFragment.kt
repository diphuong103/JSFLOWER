package com.example.jsflower.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import com.example.jsflower.Model.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jsflower.adaptar.MenuAdapter
import com.example.jsflower.databinding.FragmentSearchBinding
import androidx.appcompat.widget.SearchView
import com.google.firebase.database.*

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var adapter: MenuAdapter
    private lateinit var database: FirebaseDatabase
    private val originalMenuItems = mutableListOf<MenuItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        database = FirebaseDatabase.getInstance()

        // Start loading indicator
        binding.searchProgressBar.visibility = View.VISIBLE

        // Retrieve all menu items from all categories
        retrieveAllMenuItems()
        setupSearchView()

        return binding.root
    }

    private fun retrieveAllMenuItems() {
        val categoryRef: DatabaseReference = database.reference.child("category")

        // First get all categories
        categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Track how many categories we've processed
                val totalCategories = snapshot.childrenCount
                var processedCategories = 0

                if (totalCategories == 0L) {
                    // No categories found
                    binding.searchProgressBar.visibility = View.GONE
                    binding.noResultsText.visibility = View.VISIBLE
                    return
                }

                // For each category, get its menu items
                for (categorySnapshot in snapshot.children) {
                    val categoryId = categorySnapshot.key

                    // Get menu items for this category
                    val menuRef: DatabaseReference = database.reference.child("list").child(categoryId!!)

                    menuRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(menuSnapshot: DataSnapshot) {
                            for (itemSnapshot in menuSnapshot.children) {
                                val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                                menuItem?.let {
                                    // Add category information to menu item for better filtering
                                    it.categoryId = categoryId
                                    originalMenuItems.add(it)
                                }
                            }

                            // Increment processed count
                            processedCategories++

                            // If all categories processed, show the menu
                            if (processedCategories >= totalCategories) {
                                binding.searchProgressBar.visibility = View.GONE
                                showAllMenu()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            processedCategories++
                            if (processedCategories >= totalCategories) {
                                binding.searchProgressBar.visibility = View.GONE
                                showAllMenu()
                            }
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.searchProgressBar.visibility = View.GONE
                binding.noResultsText.visibility = View.VISIBLE
            }
        })

        // Also retrieve items from the "list" node for backward compatibility
        val flowerRef: DatabaseReference = database.reference.child("list")
        flowerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (flowerSnapshot in snapshot.children) {
                    val menuItem = flowerSnapshot.getValue(MenuItem::class.java)
                    menuItem?.let {
                        // Add a special category marker
                        it.categoryId = "list"
                        originalMenuItems.add(it)
                    }
                }
                // Update UI if we have items
                if (originalMenuItems.isNotEmpty()) {
                    binding.searchProgressBar.visibility = View.GONE
                    showAllMenu()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error here if needed
            }
        })
    }

    private fun showAllMenu() {
        if (originalMenuItems.isEmpty()) {
            binding.noResultsText.visibility = View.VISIBLE
            binding.menuRecyclerView.visibility = View.GONE
        } else {
            binding.noResultsText.visibility = View.GONE
            binding.menuRecyclerView.visibility = View.VISIBLE
            setAdapter(ArrayList(originalMenuItems))
        }
    }

    private fun setAdapter(filteredMenuItems: ArrayList<MenuItem>) {
        adapter = MenuAdapter(filteredMenuItems, requireContext())
        binding.menuRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.menuRecyclerView.adapter = adapter
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
        if (query.isNullOrBlank()) {
            // If query is empty, show all items
            showAllMenu()
            return
        }

        val filteredItems = originalMenuItems.filter { menuItem ->
            // Search in flower name
            (menuItem.flowerName?.contains(query, ignoreCase = true) == true) ||
                    // Also search in description if available
                    (menuItem.flowerDescription?.contains(query, ignoreCase = true) == true)
        }

        if (filteredItems.isEmpty()) {
            binding.noResultsText.visibility = View.VISIBLE
            binding.menuRecyclerView.visibility = View.GONE
        } else {
            binding.noResultsText.visibility = View.GONE
            binding.menuRecyclerView.visibility = View.VISIBLE
            setAdapter(ArrayList(filteredItems))
        }
    }
}