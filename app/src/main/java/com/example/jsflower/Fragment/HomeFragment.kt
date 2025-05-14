package com.example.jsflower.Fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.jsflower.ChatActivity
import com.example.jsflower.Model.BannerModel
import com.example.jsflower.Model.CartItems
import com.example.jsflower.Model.CategoryModel
import com.example.jsflower.Model.ChatModel
import com.example.jsflower.Model.MenuItem
import com.example.jsflower.R
import com.example.jsflower.adaptar.CategoryAdapter
import com.example.jsflower.adaptar.PopularAdapter
import com.example.jsflower.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date
import kotlin.math.min


class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var menuItems: MutableList<MenuItem>
    private lateinit var categoryList: ArrayList<CategoryModel>
    private lateinit var messagesListener: ValueEventListener
    private var chatRef: DatabaseReference? = null
    private val currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "default_user_id"

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

        // Setup chat button click listener and notification dot
        setupChatButton()

        // Set up chat messages listener for notification
        setupChatNotificationListener()

        return binding.root
    }

    private fun setupChatButton() {
        binding.fabChat.setOnClickListener {
            val intent = Intent(activity, ChatActivity::class.java)
            startActivity(intent)

            // Reset notification dot when opening chat
            binding.notificationDot.visibility = View.VISIBLE

            // Mark messages as read in Firebase
            markMessagesAsRead()
        }
    }

    private fun markMessagesAsRead() {
        val messagesRef = database.reference.child("chats").child(currentUserId).child("messages")
        messagesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(ChatModel::class.java)
                    if (message != null && message.userId == "admin" && !message.isRead) {
                        messageSnapshot.ref.child("isRead").setValue(true)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Error marking messages as read: ${error.message}")
            }
        })
    }

    private fun setupChatNotificationListener() {
        chatRef = database.reference.child("chats").child(currentUserId).child("messages")

        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var hasUnreadAdminMessages = false

                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(ChatModel::class.java)
                    if (message != null && message.userId == "admin" && !message.isRead) {
                        hasUnreadAdminMessages = true
                        break
                    }
                }

                // Update notification dot visibility
                if (isAdded && context != null) {
                    activity?.runOnUiThread {
                        binding.notificationDot.visibility = if (hasUnreadAdminMessages) View.VISIBLE else View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Chat messages listener cancelled: ${error.message}")
            }
        }

        chatRef?.addValueEventListener(messagesListener)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize image slider with loading indicator
        val imageSlider = binding.imageSlider

        // Setup default images (will be used if Firebase data is not available)
        val defaultImageList = ArrayList<SlideModel>().apply {
            add(SlideModel(R.drawable.bannerlanghoadep, ScaleTypes.FIT))
            add(SlideModel(R.drawable.banner_jsflower, ScaleTypes.FIT))
            add(SlideModel(R.drawable.banner_js, ScaleTypes.FIT))
        }

        // Reference to the banners in Firebase
        val bannersRef = FirebaseDatabase.getInstance().getReference("banners")

        bannersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentDate = Date().time
                val activeBanners = ArrayList<BannerModel>()

                // Filter active banners
                for (bannerSnapshot in snapshot.children) {
                    val banner = bannerSnapshot.getValue(BannerModel::class.java)
                    banner?.let {
                        // Only include active banners that are currently valid
                        if (it.isActive && it.startDate <= currentDate && it.endDate >= currentDate) {
                            activeBanners.add(it)
                        }
                    }
                }

                // Update the image slider
                if (activeBanners.isNotEmpty()) {
                    // We have banners from Firebase, use them
                    val firebaseImageList = ArrayList<SlideModel>()

                    for (banner in activeBanners) {
                        if (banner.imageUrl.isNotEmpty()) {
                            firebaseImageList.add(SlideModel(banner.imageUrl, banner.title, ScaleTypes.FIT))
                        }
                    }

                    if (firebaseImageList.isNotEmpty()) {
                        // Use Firebase banners
                        imageSlider.setImageList(firebaseImageList, ScaleTypes.FIT)
                    } else {
                        // No valid image URLs, use defaults
                        imageSlider.setImageList(defaultImageList, ScaleTypes.FIT)
                    }
                } else {
                    // No active banners, use defaults
                    imageSlider.setImageList(defaultImageList, ScaleTypes.FIT)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Error loading banners: ${error.message}")
                // Use default images on error
                imageSlider.setImageList(defaultImageList, ScaleTypes.FIT)
            }
        })

        // Set up click listener for the slider
        imageSlider.setItemClickListener(object : ItemClickListener {
            override fun doubleClick(position: Int) {
                // Not implemented as mentioned
            }

            override fun onItemSelected(position: Int) {
                val itemMessage = "Selected Image $position"
                Toast.makeText(requireContext(), itemMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listeners to prevent memory leaks
        chatRef?.removeEventListener(messagesListener)
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
                        // Set the key for each menu item
                        it.key = flowerSnapshot.key ?: ""
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
            return
        }

        // Create CartItems object
        val cartItem = CartItems(
            menuItem.flowerName.toString(),
            menuItem.flowerPrice.toString(),
            menuItem.flowerDescription.toString(),
            menuItem.flowerImage.toString(),
            1, // Item ID is set to 1 as in the DetailActivity
            menuItem.flowerIngredient,
            quantity = 1,
            flowerKey = menuItem.key
        )

        // Save to Firebase
        val database = FirebaseDatabase.getInstance().reference
        database.child("users").child(userId).child("CartItems").push().setValue(cartItem)
            .addOnSuccessListener {
                Toast.makeText(requireContext(),
                    "Thêm ${menuItem.flowerName} vào giỏ hàng thành công <3",
                    Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(requireContext(),
                    "Thêm vào giỏ hàng thất bại -_-",
                    Toast.LENGTH_SHORT).show()
            }
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
                                    // Ensure we set the key for the item
                                    it.key = productId
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