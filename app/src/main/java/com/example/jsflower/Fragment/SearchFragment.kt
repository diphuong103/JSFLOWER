package com.example.jsflower.Fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jsflower.Model.CartItems
import com.example.jsflower.Model.MenuItem
import com.example.jsflower.adaptar.MenuAdapter
import com.example.jsflower.adaptar.FilterChipAdapter
import com.example.jsflower.databinding.FragmentSearchBinding
import androidx.appcompat.widget.SearchView
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.*

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var adapter: MenuAdapter
    private lateinit var filterChipAdapter: FilterChipAdapter
    private lateinit var database: FirebaseDatabase
    private val menuItems = mutableListOf<MenuItem>() // Changed to List<MenuItem>

    private var categoryFilters = mutableSetOf<String>()
    private var minPrice = 0
    private var maxPrice = 1000000
    private var currentMinPrice = 0
    private var currentMaxPrice = maxPrice
    private var searchQuery: String? = null

    private var filtersVisible = false
    private var lastFilterApplied = 0L

    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance()

        binding.filtersCardView.visibility = View.GONE

        binding.toggleFiltersButton.setOnClickListener { toggleFilters() }
        setupPriceSlider()
        binding.applyFiltersButton.setOnClickListener { applyFilters() }
        binding.resetFiltersButton.setOnClickListener { resetFilters() }
        setupActiveFiltersRecycler()

        binding.searchProgressBar.visibility = View.VISIBLE
        binding.noResultsText.visibility = View.GONE
        binding.menuRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        retrieveCategories()
        retrieveAllMenuItems()
        setupSearchView()

        return binding.root
    }

    private fun setupPriceSlider() {
        binding.priceRangeSlider.max = 100
        binding.priceRangeSlider.progress = 100
        binding.maxPriceText.text = formatPrice(maxPrice)
        binding.minPriceText.text = formatPrice(minPrice)

        binding.priceRangeSlider.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                currentMaxPrice = if (progress > 0) {
                    (maxPrice * progress) / 100
                } else 0
                binding.maxPriceText.text = formatPrice(currentMaxPrice)
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun formatPrice(price: Int): String {
        val format = NumberFormat.getNumberInstance(Locale.getDefault())
        return format.format(price) + "đ"
    }

    private fun toggleFilters() {
        filtersVisible = !filtersVisible
        if (filtersVisible) {
            binding.filtersCardView.visibility = View.VISIBLE
            binding.toggleFiltersButton.text = "Ẩn bộ lọc"
        } else {
            binding.filtersCardView.visibility = View.GONE
            binding.toggleFiltersButton.text = "Hiện bộ lọc"
        }
    }

    private fun setupActiveFiltersRecycler() {
        filterChipAdapter = FilterChipAdapter(arrayListOf()) { filter ->
            when {
                filter.startsWith("Danh mục:") -> {
                    val category = filter.substringAfter("Danh mục: ")
                    categoryFilters.remove(category)
                    for (i in 0 until binding.categoryChipGroup.childCount) {
                        val chip = binding.categoryChipGroup.getChildAt(i) as Chip
                        if (chip.text.toString() == category) {
                            chip.isChecked = false
                            break
                        }
                    }
                }
                filter.startsWith("Giá tối đa:") -> {
                    binding.priceRangeSlider.progress = 100
                    currentMaxPrice = maxPrice
                    binding.maxPriceText.text = formatPrice(maxPrice)
                }
            }
            applyFilters()
        }
        binding.activeFiltersRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.activeFiltersRecyclerView.adapter = filterChipAdapter
    }

    private fun sanitizeCategoryName(name: String): String {
        return name.replace("💌", "").trim()
    }

    private fun retrieveCategories() {
        val categoryRef: DatabaseReference = database.reference.child("category")
        categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.categoryChipGroup.removeAllViews()
                if (!snapshot.exists() || snapshot.childrenCount.toInt() == 0) {
                    Toast.makeText(context, "Không tìm thấy danh mục", Toast.LENGTH_SHORT).show()
                    return
                }
                for (categorySnapshot in snapshot.children) {
                    val categoryId = categorySnapshot.key ?: continue
                    val categoryNameRaw = categorySnapshot.child("categoryName").getValue(String::class.java) ?: categoryId
                    val categoryName = sanitizeCategoryName(categoryNameRaw)

                    val chip = Chip(requireContext())
                    chip.text = categoryName
                    chip.isCheckable = true
                    chip.tag = categoryId

                    chip.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            categoryFilters.add(categoryName)
                        } else {
                            categoryFilters.remove(categoryName)
                        }
                        applyFilters()
                    }
                    binding.categoryChipGroup.addView(chip)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Không thể tải danh mục: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun retrieveAllMenuItems() {
        val categoryRef = database.reference.child("category")

        categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val totalCategories = snapshot.childrenCount
                var processedCategories = 0L

                if (totalCategories == 0L) {
                    binding.searchProgressBar.visibility = View.GONE
                    binding.noResultsText.visibility = View.VISIBLE
                    Toast.makeText(context, "Không tìm thấy danh mục sản phẩm", Toast.LENGTH_SHORT).show()
                    return
                }

                for (categorySnapshot in snapshot.children) {
                    val categoryId = categorySnapshot.key ?: continue
                    val categoryNameRaw = categorySnapshot.child("categoryName").getValue(String::class.java) ?: categoryId
                    val categoryName = sanitizeCategoryName(categoryNameRaw)

                    val categoryProductsRef = database.reference.child("category").child(categoryId).child("products")

                    categoryProductsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(productsSnapshot: DataSnapshot) {
                            if (productsSnapshot.exists()) {
                                val productCount = productsSnapshot.childrenCount
                                var loadedCount = 0

                                if (productCount == 0L) {
                                    processedCategories++
                                    if (processedCategories >= totalCategories) {
                                        binding.searchProgressBar.visibility = View.GONE
                                        showAllMenu()
                                    }
                                    return
                                }

                                for (productSnapshot in productsSnapshot.children) {
                                    val productId = productSnapshot.key ?: continue

                                    database.reference.child("list").child(productId)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(productDataSnapshot: DataSnapshot) {
                                                val menuItem = productDataSnapshot.getValue(MenuItem::class.java)
                                                menuItem?.let {
                                                    it.categoryId = categoryId
                                                    it.categoryName = categoryName
                                                    if (it.key.isNullOrEmpty()) {
                                                        it.key = productId
                                                    }
                                                    // Thêm dữ liệu giá khuyến mãi nếu có
                                                    val discountPrice = productDataSnapshot.child("discountPrice").getValue(String::class.java)
                                                    if (!discountPrice.isNullOrEmpty()) {
                                                        it.discountedPrice = discountPrice
                                                    }
                                                    menuItems.add(it)
                                                }
                                                loadedCount++
                                                if (loadedCount.toLong() == productCount) {
                                                    processedCategories++
                                                    if (processedCategories >= totalCategories) {
                                                        binding.searchProgressBar.visibility = View.GONE
                                                        showAllMenu()
                                                    }
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                loadedCount++
                                                if (loadedCount.toLong() == productCount) {
                                                    processedCategories++
                                                    if (processedCategories >= totalCategories) {
                                                        binding.searchProgressBar.visibility = View.GONE
                                                        showAllMenu()
                                                    }
                                                }
                                            }
                                        })
                                }
                            } else {
                                processedCategories++
                                if (processedCategories >= totalCategories) {
                                    binding.searchProgressBar.visibility = View.GONE
                                    showAllMenu()
                                }
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
                Toast.makeText(context, "Không thể tải dữ liệu: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAllMenu() {
        if (menuItems.isEmpty()) {
            binding.noResultsText.visibility = View.VISIBLE
            binding.menuRecyclerView.visibility = View.GONE
            binding.resultCountText.text = "0 sản phẩm"
        } else {
            binding.noResultsText.visibility = View.GONE
            binding.menuRecyclerView.visibility = View.VISIBLE
            val sortedItems = menuItems.sortedBy { it.flowerName } // Using List<MenuItem>
            setAdapter(sortedItems)
            binding.resultCountText.text = "${menuItems.size} sản phẩm"
        }
    }

    private fun setAdapter(filteredMenuItems: List<MenuItem>) { // Changed to List<MenuItem>
        // Khởi tạo adapter với callback addToCart để xử lý thêm vào giỏ hàng
        adapter = MenuAdapter(
            ArrayList(filteredMenuItems), // Convert List to ArrayList if MenuAdapter requires ArrayList
            requireContext(),
            onAddToCart = { menuItem -> addItemToCart(menuItem) },
            listener = null  // Not using the listener for SearchFragment
        )
        binding.menuRecyclerView.adapter = adapter
    }

    private fun addItemToCart(menuItem: MenuItem) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(context, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
            return
        }

        // Create CartItems object - matching the same format as HomeFragment
        val cartItem = CartItems(
            menuItem.flowerName.toString(),
            menuItem.flowerPrice.toString(),
            menuItem.flowerDescription.toString(),
            menuItem.flowerImage.toString(),
            1, // Item ID is set to 1 as in the HomeFragment
            menuItem.flowerIngredient,
            quantity = 1,
            flowerKey = menuItem.key,
            discountedPrice = menuItem.discountedPrice ?: menuItem.flowerPrice
        )

        // Save to Firebase - using the same path structure as HomeFragment
        val database = FirebaseDatabase.getInstance()
        database.reference.child("users").child(userId).child("CartItems").push().setValue(cartItem)
            .addOnSuccessListener {
                Toast.makeText(context, "Đã thêm ${menuItem.flowerName} vào giỏ hàng", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Không thể thêm vào giỏ hàng: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("CartError", "Lỗi thêm vào giỏ hàng", e)
            }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query
                applyFilters()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastFilterApplied > 300) {
                    applyFilters()
                    lastFilterApplied = currentTime
                }
                return true
            }
        })
    }

    private fun applyFilters() {
        binding.searchProgressBar.visibility = View.VISIBLE
        var filteredItems = menuItems.toList() // Using List<MenuItem>

        if (!searchQuery.isNullOrBlank()) {
            filteredItems = filteredItems.filter { menuItem ->
                val nameMatch = menuItem.flowerName?.contains(searchQuery!!, ignoreCase = true) == true
                val descMatch = menuItem.flowerDescription?.contains(searchQuery!!, ignoreCase = true) == true
                nameMatch || descMatch
            }
        }

        if (categoryFilters.isNotEmpty()) {
            val normalizedFilters = categoryFilters.map { sanitizeCategoryName(it).lowercase(Locale.getDefault()) }
            filteredItems = filteredItems.filter { menuItem ->
                val categoryNormalized = menuItem.categoryName?.let { sanitizeCategoryName(it).lowercase(Locale.getDefault()) }
                categoryNormalized != null && normalizedFilters.contains(categoryNormalized)
            }
        }

        if (currentMaxPrice < maxPrice) {
            filteredItems = filteredItems.filter { menuItem ->
                // Sử dụng discountedPrice nếu có, không thì dùng flowerPrice để lọc giá
                val price = (menuItem.discountedPrice ?: menuItem.flowerPrice)?.toIntOrNull() ?: 0
                price <= currentMaxPrice && price >= currentMinPrice
            }
        }

        updateActiveFilters()
        binding.searchProgressBar.visibility = View.GONE

        if (filteredItems.isEmpty()) {
            binding.noResultsText.visibility = View.VISIBLE
            binding.menuRecyclerView.visibility = View.GONE
            binding.resultCountText.text = "0 sản phẩm"
        } else {
            binding.noResultsText.visibility = View.GONE
            binding.menuRecyclerView.visibility = View.VISIBLE
            val sortedItems = filteredItems.sortedBy { it.flowerName } // Using List<MenuItem>
            setAdapter(sortedItems)
            binding.resultCountText.text = "${filteredItems.size} sản phẩm"
        }
    }

    private fun updateActiveFilters() {
        val activeFilters = mutableListOf<String>()
        for (category in categoryFilters) {
            activeFilters.add("Danh mục: $category")
        }
        if (currentMaxPrice < maxPrice) {
            activeFilters.add("Giá tối đa: ${formatPrice(currentMaxPrice)}")
        }
        filterChipAdapter.updateFilters(activeFilters)
    }

    private fun resetFilters() {
        binding.searchView.setQuery("", false)
        searchQuery = null
        categoryFilters.clear()
        for (i in 0 until binding.categoryChipGroup.childCount) {
            val chip = binding.categoryChipGroup.getChildAt(i) as Chip
            chip.isChecked = false
        }
        binding.priceRangeSlider.progress = 100
        currentMaxPrice = maxPrice
        currentMinPrice = 0
        binding.maxPriceText.text = formatPrice(maxPrice)
        applyFilters()
        Toast.makeText(context, "Đã xóa tất cả bộ lọc", Toast.LENGTH_SHORT).show()
    }
}