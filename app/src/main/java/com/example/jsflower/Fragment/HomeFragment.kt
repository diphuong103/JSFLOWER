package com.example.jsflower.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.denzcoskun.imageslider.models.SlideModel
import com.example.jsflower.MenuBottomSheetFragment
import com.example.jsflower.Model.CategoryModel
import com.example.jsflower.Model.MenuItem
import com.example.jsflower.R
import com.example.jsflower.adaptar.CategoryAdapter
import com.example.jsflower.adaptar.MenuAdapter
import com.example.jsflower.adaptar.PopularAdapter
import com.example.jsflower.databinding.FragmentHomeBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.min

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding

    private lateinit var database: FirebaseDatabase
    private lateinit var menuItems: MutableList<MenuItem>
    private lateinit var categoryList: ArrayList<CategoryModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Initialize database
        database = FirebaseDatabase.getInstance()

        // Lay du lieu hien thi san pham pho bien
        getAndDisplayPopularItems()

        // Lay du lieu categories tu database
        setupCategoriesRecyclerView()

        return binding.root
    }

    private fun getAndDisplayPopularItems() {
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
                }
                if (menuItems.isNotEmpty()) {
                    randomPopularItems()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Không thể tải dữ liệu sản phẩm: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun randomPopularItems() {
        val index = menuItems.indices.toList().shuffled()
        val numItemToShow = min(6, menuItems.size)
        val subsetMenuItems = index.take(numItemToShow).map {
            menuItems[it]
        }
        setPopularItemAdapter(subsetMenuItems)
    }

    private fun setPopularItemAdapter(subsetMenuItems: List<MenuItem>) {
        val adapter = PopularAdapter(subsetMenuItems, requireContext()) { menuItem ->
            addToCart(menuItem) // Thêm vào giỏ hàng
        }
        binding.popularRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.popularRecyclerView.adapter = adapter
    }

    private fun addToCart(menuItem: MenuItem) {
        // Handle adding to cart
        Toast.makeText(requireContext(), "Thêm vào giỏ hàng: ${menuItem.flowerName}", Toast.LENGTH_SHORT).show()

        // Optionally, save to SharedPreferences or Firebase for cart persistence
    }

    private fun setupCategoriesRecyclerView() {
        categoryList = ArrayList<CategoryModel>()
        binding.categoriesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val adapter = CategoryAdapter(
            requireContext(),
            categoryList,
            object : CategoryAdapter.OnCategoryClickListener {
                override fun onCategoryClick(category: CategoryModel, position: Int) {
                    Toast.makeText(
                        requireContext(),
                        "Đã chọn: ${category.name}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Load products by category
                    loadCategoryProducts(category)
                }
            })
        binding.categoriesRecyclerView.adapter = adapter

        loadCategoriesFromFirebase()
    }

    private fun loadCategoriesFromFirebase() {
        val categoryRef = database.reference.child("category")

        categoryRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryList.clear()

                for (categorySnapshot in snapshot.children) {
                    val categoryId = categorySnapshot.key ?: ""
                    val categoryName =
                        categorySnapshot.child("categoryName").getValue(String::class.java) ?: ""
                    val categoryImage =
                        categorySnapshot.child("categoryImage").getValue(String::class.java) ?: ""

                    val category = CategoryModel(categoryId, categoryName, categoryImage)
                    categoryList.add(category)
                }

                (binding.categoriesRecyclerView.adapter as CategoryAdapter).updateCategories(
                    categoryList
                )

                if (categoryList.isNotEmpty()) {
                    loadCategoryProducts(categoryList[0])
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Không thể tải dữ liệu danh mục: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadCategoryProducts(category: CategoryModel) {
        binding.categoryProductsHeader.text = "Sản phẩm ${category.name}"

        val categoryProductsRef =
            database.reference.child("category").child(category.id).child("products")

        categoryProductsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categoryProducts = mutableListOf<MenuItem>()

                val totalProducts = snapshot.childrenCount
                var loadedProducts = 0

                if (totalProducts == 0L) {
                    setProductItemAdapter(emptyList())
                    return
                }

                for (productSnapshot in snapshot.children) {
                    val productId = productSnapshot.key ?: continue

                    database.reference.child("list").child(productId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(productDataSnapshot: DataSnapshot) {
                                val menuItem = productDataSnapshot.getValue(MenuItem::class.java)
                                menuItem?.let {
                                    categoryProducts.add(it)
                                }

                                loadedProducts++

                                if (loadedProducts.toLong() == totalProducts) {
                                    setProductItemAdapter(categoryProducts)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    requireContext(),
                                    "Không thể tải chi tiết sản phẩm: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Không thể tải sản phẩm theo danh mục: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setProductItemAdapter(categoryProducts: List<MenuItem>) {
        val adapter = PopularAdapter(categoryProducts, requireContext()) { menuItem ->
            addToCart(menuItem) // Thêm vào giỏ hàng
        }
        binding.categoryFlowerRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryFlowerRecyclerView.adapter = adapter
    }
}
