package com.example.jsflower.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
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

        binding.viewAllMenu.setOnClickListener {
            val bottomSheetDialog = MenuBottomSheetFragment()
            bottomSheetDialog.show(parentFragmentManager, "Test")
        }

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
                // lay ngau nhien item tu db
                if (menuItems.isNotEmpty()) {
                    randomPopularItems()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Không thể tải dữ liệu sản phẩm: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun randomPopularItems() {
        val index = menuItems.indices.toList().shuffled()
        val numItemToShow = minOf(6, menuItems.size)
        val subsetMenuItems = index.take(numItemToShow).map {
            menuItems[it]
        }
        setPopularItemAdapter(subsetMenuItems)
        setProductItemAdapter(subsetMenuItems)
    }

    private fun setPopularItemAdapter(subsetMenuItems: List<MenuItem>) {
        val adapter = MenuAdapter(subsetMenuItems, requireContext())
        binding.popularRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.popularRecyclerView.adapter = adapter
    }

    private fun setProductItemAdapter(subsetMenuItems: List<MenuItem>) {
        val adapter = MenuAdapter(subsetMenuItems, requireContext())
        binding.categoryFlowerRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryFlowerRecyclerView.adapter = adapter
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
                // Không cần triển khai nếu không sử dụng
            }

            override fun onItemSelected(position: Int) {
                val itemMessage = "Selected Image $position"
                Toast.makeText(requireContext(), itemMessage, Toast.LENGTH_SHORT).show()
            }
        })

        binding.popularRecyclerView.addItemDecoration(PopularAdapter.VerticalSpaceItemDecoration(32))
    }

    private fun setupCategoriesRecyclerView() {
        categoryList = ArrayList<CategoryModel>()
        binding.categoriesRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Thiết lập adapter ban đầu với danh sách trống
        val adapter = CategoryAdapter(requireContext(), categoryList, object : CategoryAdapter.OnCategoryClickListener {
            override fun onCategoryClick(category: CategoryModel, position: Int) {
                // Xử lý khi người dùng nhấp vào một thể loại
                Toast.makeText(requireContext(), "Đã chọn: ${category.name}", Toast.LENGTH_SHORT).show()

                // Tải danh sách sản phẩm của thể loại đã chọn
                loadCategoryProducts(category)
            }
        })
        binding.categoriesRecyclerView.adapter = adapter

        // Tải dữ liệu danh mục từ Firebase
        loadCategoriesFromFirebase()
    }

    private fun loadCategoriesFromFirebase() {
        val categoryRef = database.reference.child("category")

        categoryRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryList.clear()

                for (categorySnapshot in snapshot.children) {
                    val categoryId = categorySnapshot.key ?: ""
                    val categoryName = categorySnapshot.child("categoryName").getValue(String::class.java) ?: ""
                    val categoryImage = categorySnapshot.child("categoryImage").getValue(String::class.java) ?: ""

                    val category = CategoryModel(categoryId, categoryName, categoryImage)
                    categoryList.add(category)
                }

                // Cập nhật adapter với dữ liệu mới
                (binding.categoriesRecyclerView.adapter as CategoryAdapter).updateCategories(categoryList)

                // Nếu có ít nhất một danh mục, tải sản phẩm của danh mục đầu tiên
                if (categoryList.isNotEmpty()) {
                    loadCategoryProducts(categoryList[0])
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Không thể tải dữ liệu danh mục: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadCategoryProducts(category: CategoryModel) {
        // Cập nhật tiêu đề phần sản phẩm theo thể loại
        binding.categoryProductsHeader.text = "Sản phẩm ${category.name}"

        // Tải sản phẩm theo category.id từ database
        val categoryProductsRef = database.reference.child("category").child(category.id).child("products")

        categoryProductsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categoryProducts = mutableListOf<MenuItem>()

                // Đếm số lượng sản phẩm để theo dõi khi nào tất cả đã được tải
                val totalProducts = snapshot.childrenCount
                var loadedProducts = 0

                if (totalProducts == 0L) {
                    // Nếu không có sản phẩm trong danh mục này
                    setProductItemAdapter(emptyList())
                    return
                }

                for (productSnapshot in snapshot.children) {
                    val productId = productSnapshot.key ?: continue

                    // Lấy chi tiết sản phẩm từ danh sách sản phẩm
                    database.reference.child("list").child(productId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(productDataSnapshot: DataSnapshot) {
                            val menuItem = productDataSnapshot.getValue(MenuItem::class.java)
                            menuItem?.let {
                                categoryProducts.add(it)
                            }

                            loadedProducts++

                            // Khi đã tải tất cả sản phẩm, cập nhật RecyclerView
                            if (loadedProducts.toLong() == totalProducts) {
                                setProductItemAdapter(categoryProducts)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(requireContext(), "Không thể tải chi tiết sản phẩm: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Không thể tải sản phẩm theo danh mục: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}