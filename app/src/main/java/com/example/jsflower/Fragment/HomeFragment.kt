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

    // Track the active listeners to remove them properly
    private val activeListeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()

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
                if (isAdded && activity != null && !isDetached()) {
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
        chatRef?.let { ref ->
            activeListeners.add(Pair(ref, messagesListener))
        }
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

        val bannersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if fragment is still attached before using context
                if (!isAdded || isDetached()) return

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
                // Check if fragment is attached to context before continuing
                if (!isAdded || isDetached()) return

                Log.e("HomeFragment", "Error loading banners: ${error.message}")
                // Use default images on error
                imageSlider.setImageList(defaultImageList, ScaleTypes.FIT)
            }
        }

        bannersRef.addListenerForSingleValueEvent(bannersListener)
        activeListeners.add(Pair(bannersRef, bannersListener))

        // Set up click listener for the slider
        imageSlider.setItemClickListener(object : ItemClickListener {
            override fun doubleClick(position: Int) {
                // Not implemented as mentioned
            }

            override fun onItemSelected(position: Int) {
                // Check if fragment is attached to context before showing toast
                if (!isAdded || isDetached()) return

                val itemMessage = "Selected Image $position"
                Toast.makeText(requireContext(), itemMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listeners to prevent memory leaks
        removeAllListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure all listeners are removed
        removeAllListeners()
    }

    private fun removeAllListeners() {
        // Clear all active listeners
        for ((ref, listener) in activeListeners) {
            ref.removeEventListener(listener)
        }
        activeListeners.clear()

        // Also remove the chat listener if it exists
        chatRef?.removeEventListener(messagesListener)
    }

    private fun getAndDisplayPopularItems() {
        val flowerRef: DatabaseReference = database.reference.child("list")
        menuItems = mutableListOf()

        // laays du lieu menu item tu db
        val flowerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if fragment is still attached
                if (!isAdded || isDetached()) return

                for (flowerSnapshot in snapshot.children) {
                    val menuItem = flowerSnapshot.getValue(MenuItem::class.java)
                    menuItem?.let {
                        // Set the key for each menu item
                        it.key = flowerSnapshot.key ?: ""

                        val discountPrice = flowerSnapshot.child("discountPrice").getValue(String::class.java)
                        it.discountedPrice = discountPrice ?: it.flowerPrice

                        menuItems.add(it)
                    }
                }
                if (menuItems.isNotEmpty()) {
                    randomPopularItems()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Check if fragment is still attached before showing toast
                if (!isAdded || isDetached()) return

                Log.e("HomeFragment", "Error loading popular items: ${error.message}")
                Toast.makeText(
                    requireContext(),
                    "Không thể tải dữ liệu sản phẩm: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        flowerRef.addListenerForSingleValueEvent(flowerListener)
        activeListeners.add(Pair(flowerRef, flowerListener))
    }

    private fun randomPopularItems() {
        // Check if fragment is still attached
        if (!isAdded || isDetached()) return

        val index = menuItems.indices.toList().shuffled()
        val numItemToShow = min(6, menuItems.size)
        val subsetMenuItems = index.take(numItemToShow).map {
            menuItems[it]
        }
        setPopularItemAdapter(subsetMenuItems)
    }

    private fun setPopularItemAdapter(subsetMenuItems: List<MenuItem>) {
        // Check if fragment is still attached
        if (!isAdded || isDetached()) return

        val adapter = PopularAdapter(subsetMenuItems.toMutableList(), requireContext()) { menuItem ->
            addToCart(menuItem) // Thêm vào giỏ hàng
        }
        binding.popularRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.popularRecyclerView.adapter = adapter
    }

    private fun addToCart(menuItem: MenuItem) {
        // Check if fragment is still attached
        if (!isAdded || isDetached()) return

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
            flowerKey = menuItem.key,
            discountedPrice = menuItem.discountedPrice
        )

        // Save to Firebase
        val database = FirebaseDatabase.getInstance().reference
        database.child("users").child(userId).child("CartItems").push().setValue(cartItem)
            .addOnSuccessListener {
                // Check if fragment is still attached before showing toast
                if (!isAdded || isDetached()) return@addOnSuccessListener

                Toast.makeText(requireContext(),
                    "Thêm ${menuItem.flowerName} vào giỏ hàng thành công <3",
                    Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                // Check if fragment is still attached before showing toast
                if (!isAdded || isDetached()) return@addOnFailureListener

                Toast.makeText(requireContext(),
                    "Thêm vào giỏ hàng thất bại -_-",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupCategoriesRecyclerView() {
        // Check if fragment is still attached
        if (!isAdded || isDetached()) return

        categoryList = ArrayList<CategoryModel>()
        binding.categoriesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val adapter = CategoryAdapter(
            requireContext(),
            categoryList,
            object : CategoryAdapter.OnCategoryClickListener {
                override fun onCategoryClick(category: CategoryModel, position: Int) {
                    // Check if fragment is still attached before showing toast
                    if (!isAdded || isDetached()) return

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

        val categoryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if fragment is still attached before processing
                if (!isAdded || isDetached()) return

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

                // Check again before updating UI
                if (!isAdded || isDetached()) return

                (binding.categoriesRecyclerView.adapter as CategoryAdapter).updateCategories(
                    categoryList
                )

                if (categoryList.isNotEmpty()) {
                    loadCategoryProducts(categoryList[0])
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Check if fragment is attached to context before showing toast
                if (!isAdded || isDetached()) {
                    Log.e("HomeFragment", "Error loading categories: ${error.message}, Fragment not attached")
                    return
                }

                Log.e("HomeFragment", "Error loading categories: ${error.message}")
                Toast.makeText(
                    requireContext(),
                    "Không thể tải dữ liệu danh mục: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        categoryRef.addValueEventListener(categoryListener)
        activeListeners.add(Pair(categoryRef, categoryListener))
    }

    private fun loadCategoryProducts(category: CategoryModel) {
        // Check if fragment is still attached
        if (!isAdded || isDetached()) return

        binding.categoryProductsHeader.text = "Sản phẩm ${category.name}"

        val categoryProductsRef =
            database.reference.child("category").child(category.id).child("products")

        val productsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if fragment is still attached
                if (!isAdded || isDetached()) return

                val categoryProducts = mutableListOf<MenuItem>()

                val totalProducts = snapshot.childrenCount
                var loadedProducts = 0

                if (totalProducts == 0L) {
                    setProductItemAdapter(emptyList())
                    return
                }

                for (productSnapshot in snapshot.children) {
                    val productId = productSnapshot.key ?: continue

                    val productListener = object : ValueEventListener {
                        override fun onDataChange(productDataSnapshot: DataSnapshot) {
                            // Check if fragment is still attached
                            if (!isAdded || isDetached()) return

                            val menuItem = productDataSnapshot.getValue(MenuItem::class.java)
                            menuItem?.let {
                                // Ensure we set the key for the item
                                it.key = productId
                                categoryProducts.add(it)
                            }

                            loadedProducts++

                            if (loadedProducts.toLong() == totalProducts) {
                                // Final check before updating adapter
                                if (!isAdded || isDetached()) return
                                setProductItemAdapter(categoryProducts)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Check if fragment is still attached before showing toast
                            if (!isAdded || isDetached()) {
                                Log.e("HomeFragment", "Error loading product details: ${error.message}, Fragment not attached")
                                return
                            }

                            Log.e("HomeFragment", "Error loading product details: ${error.message}")
                            Toast.makeText(
                                requireContext(),
                                "Không thể tải chi tiết sản phẩm: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    val productRef = database.reference.child("list").child(productId)
                    productRef.addListenerForSingleValueEvent(productListener)
                    activeListeners.add(Pair(productRef, productListener))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Check if fragment is still attached before showing toast
                if (!isAdded || isDetached()) {
                    Log.e("HomeFragment", "Error loading category products: ${error.message}, Fragment not attached")
                    return
                }

                Log.e("HomeFragment", "Error loading category products: ${error.message}")
                Toast.makeText(
                    requireContext(),
                    "Không thể tải sản phẩm theo danh mục: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        categoryProductsRef.addListenerForSingleValueEvent(productsListener)
        activeListeners.add(Pair(categoryProductsRef, productsListener))
    }

    private fun setProductItemAdapter(categoryProducts: List<MenuItem>) {
        // Check if fragment is still attached
        if (!isAdded || isDetached()) return

        val adapter = PopularAdapter(categoryProducts.toMutableList(), requireContext()) { menuItem ->
            addToCart(menuItem) // Thêm vào giỏ hàng
        }
        binding.categoryFlowerRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryFlowerRecyclerView.adapter = adapter
    }
}