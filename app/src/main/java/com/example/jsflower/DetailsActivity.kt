package com.example.jsflower

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.jsflower.Model.CartItems
import com.example.jsflower.Model.CategoryModel
import com.example.jsflower.Model.MenuItem
import com.example.jsflower.Model.ReviewModel
import com.example.jsflower.adaptar.MenuAdapter
import com.example.jsflower.adaptar.ReviewAdapter
import com.example.jsflower.databinding.ActivityDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class DetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailsBinding

    private lateinit var auth: FirebaseAuth

    private lateinit var database: FirebaseDatabase
    private lateinit var menuItems: MutableList<MenuItem>
    private lateinit var reviewsList: MutableList<ReviewModel>
    private lateinit var categoryList: ArrayList<CategoryModel>
    private lateinit var reviewAdapter: ReviewAdapter

    private var flowerName: String? = null
    private var flowerImage: String? = null
    private var flowerDescriptions: String? = null
    private var flowerIngredients: String? = null
    private var flowerPrice: String? = null
    private var flowerKey: String? = null
    private var flowerTag: String? = null
    private var flowerOriginalPrice: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        // Extract all data from intent
        flowerName = intent.getStringExtra("MenuItemName")
        flowerDescriptions = intent.getStringExtra("MenuItemDescription")
        flowerIngredients = intent.getStringExtra("MenuItemIngredient")
        flowerPrice = intent.getStringExtra("MenuItemPrice")
        flowerOriginalPrice = intent.getStringExtra("MenuItemOriginalPrice")
        flowerImage = intent.getStringExtra("MenuItemImage")
        flowerKey = intent.getStringExtra("MenuItemKey")
        flowerTag = intent.getStringExtra("TAG")


        // Log the retrieved key for debugging
        println("DEBUG: Retrieved flowerKey from intent: $flowerKey")
        println("DEBUG: Received data in DetailsActivity")
        println("DEBUG: flowerName = $flowerName")
        println("DEBUG: flowerKey = $flowerKey")
        println("DEBUG: TAG = $flowerTag")

        println("DEBUG: flowerPrice = $flowerPrice")
        println("DEBUG: flowerOriginalPrice = $flowerOriginalPrice")


        with(binding) {
            detailFlowerName.text = flowerName
            detailDescriptionTextView.text = flowerDescriptions
            detailIngredients.text = flowerIngredients

            // Giá sale (giá hiện tại)
            priceTextView.text = flowerPrice

            // Giá gốc (nếu có, hiển thị gạch ngang)
            priceTextView.text = flowerPrice

// Giá gốc (nếu có, hiển thị gạch ngang)
            if (!flowerOriginalPrice.isNullOrEmpty() && flowerOriginalPrice != flowerPrice) {
//                realPrice.text = flowerOriginalPrice
//                realPrice.paintFlags = realPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
//                realPrice.visibility = View.VISIBLE
            } else {
//                realPrice.visibility = View.GONE
            }

            tagProduct.text = flowerTag

            Glide.with(this@DetailsActivity).load(Uri.parse(flowerImage))
                .into(detailsFlowerImageView)
        }

        binding.addReviewTitle.visibility = View.GONE
        binding.userRatingBar.visibility = View.GONE
        binding.reviewEditText.visibility = View.GONE
        binding.submitReviewButton.visibility = View.GONE
        binding.ycMH.visibility = View.VISIBLE

        if (flowerKey.isNullOrEmpty()) {
            println("DEBUG: flowerKey is null or empty, trying to find by name")
            findFlowerKeyByName {
                loadDataWithKey()
                checkPurchaseHistoryForCurrentProduct()
            }

        } else {
            loadDataWithKey()
            checkPurchaseHistoryForCurrentProduct()
        }


        // First find the key if it's not directly provided
        if (flowerKey.isNullOrEmpty() && !flowerName.isNullOrEmpty()) {
            findFlowerKeyByName {
                loadDataWithKey()
            }

        } else {
            // Get popular products after ensuring we have either a key or a name to find the key
            getAndDisplayPopularItems()
        }

        binding.imageButton.setOnClickListener {
            finish()
        }

        binding.addToCartButton.setOnClickListener {
            addItemToCart()
        }

        // Initialize review adapter and RecyclerView
        reviewsList = mutableListOf()
        reviewAdapter = ReviewAdapter(reviewsList)
        binding.reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DetailsActivity)
            adapter = reviewAdapter
        }

        // Set up submit review button
        binding.submitReviewButton.setOnClickListener {
            submitReview()
        }

        // Load existing reviews - but only if we have a key or can find one
        loadReviews()
        setupClickListeners()
    }

    private fun fetchFlowerDetails(itemName: String) {
        val database = FirebaseDatabase.getInstance()
        val menuRef = database.getReference("menu")

        menuRef.orderByChild("flowerName").equalTo(itemName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (menuSnapshot in snapshot.children) {
                            flowerName =
                                menuSnapshot.child("flowerName").getValue(String::class.java)
                            flowerPrice =
                                menuSnapshot.child("flowerPrice").getValue(String::class.java)
                            flowerDescriptions =
                                menuSnapshot.child("flowerDescription").getValue(String::class.java)
                            flowerIngredients =
                                menuSnapshot.child("flowerIngredient").getValue(String::class.java)
                            flowerImage =
                                menuSnapshot.child("flowerImage").getValue(String::class.java)

                            setupUI()
                            break
                        }
                    } else {
                        Toast.makeText(
                            this@DetailsActivity,
                            "Không tìm thấy thông tin sản phẩm",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish() // Close activity if we can't find the item
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@DetailsActivity,
                        "Lỗi: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            })
    }

    private fun setupUI() {
        // Set the data to UI components
        binding.detailFlowerName.text = flowerName
        binding.priceTextView.text = flowerPrice
        binding.detailDescriptionTextView.text = flowerDescriptions ?: "Không có mô tả"
        binding.detailIngredients.text = flowerIngredients ?: "Không có thông tin"

        // Load image safely
        if (!flowerImage.isNullOrEmpty()) {
            try {
                Glide.with(this@DetailsActivity)
                    .load(flowerImage)
                    .into(binding.detailsFlowerImageView)
            } catch (e: Exception) {
                // Handle image loading error
                Toast.makeText(this, "Không thể tải hình ảnh", Toast.LENGTH_SHORT).show()
            }
        }

        // Set quantity text
//        binding.qu.text = flowerQuantity.toString()
    }

    private fun setupClickListeners() {
        // Back button
        binding.imageButton.setOnClickListener {
            finish()
        }
    }

    private fun loadReviews() {
        // If flowerKey is null or empty, try to find it from the name
        if (flowerKey.isNullOrEmpty()) {
            findFlowerKeyByName {
                loadDataWithKey()
            }
        } else {
            fetchReviews()
        }
    }

    private fun loadDataWithKey() {

        // Load reviews
        fetchReviews()

        // Load popular items
        getAndDisplayPopularItems()
    }

//    private fun findFlowerKeyByName() {
//        if (flowerName.isNullOrEmpty()) {
//            Toast.makeText(this, "Không thể nhận dạng sản phẩm", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val flowersRef = database.reference.child("list")
//        flowersRef.orderByChild("flowerName").equalTo(flowerName)
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (snapshot.exists()) {
//                        // Get the first match (assuming itemName is unique)
//                        for (flowerSnapshot in snapshot.children) {
//                            flowerKey = flowerSnapshot.key
//                            println("DEBUG: Found flowerKey: $flowerKey")
//                            fetchReviews() // Now that we have the key, fetch reviews
//                            break
//                        }
//                    } else {
//                        Toast.makeText(
//                            this@DetailsActivity,
//                            "Không thể tìm thấy mã sản phẩm",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    Toast.makeText(
//                        this@DetailsActivity,
//                        "Lỗi: ${error.message}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            })
//    }

    private fun findFlowerKeyByName(onKeyFound: () -> Unit) {
        if (flowerName.isNullOrEmpty()) {
            Toast.makeText(this, "Không thể nhận dạng sản phẩm", Toast.LENGTH_SHORT).show()
            return
        }

        val flowersRef = database.reference.child("list")
        // QUAN TRỌNG: Đảm bảo tên trường chính xác phù hợp với Firebase của bạn
        // Có thể là "flowerName" thay vì "itemName" tùy cấu trúc DB
        flowersRef.orderByChild("flowerName").equalTo(flowerName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (flowerSnapshot in snapshot.children) {
                            flowerKey = flowerSnapshot.key
                            println("DEBUG: Found flowerKey by name: $flowerKey")
                            onKeyFound() // Gọi callback
                            break
                        }
                    } else {
                        println("DEBUG: Could not find flower by name: $flowerName")
                        Toast.makeText(
                            this@DetailsActivity,
                            "Không thể tìm thấy thông tin sản phẩm",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("DEBUG: Firebase query cancelled with error: ${error.message}")
                    Toast.makeText(
                        this@DetailsActivity,
                        "Lỗi: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun fetchReviews() {
        if (flowerKey.isNullOrEmpty()) {
            println("DEBUG: Cannot fetch reviews - flowerKey is still null or empty")
            return
        }

        println("DEBUG: Fetching reviews for key: $flowerKey")
        val reviewsRef = database.reference.child("reviews").child(flowerKey!!)
        reviewsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reviewsList.clear()
                var totalRating = 0f
                var reviewCount = 0

                for (reviewSnapshot in snapshot.children) {
                    val review = reviewSnapshot.getValue(ReviewModel::class.java)
                    review?.let {
                        reviewsList.add(it)
                        totalRating += it.rating
                        reviewCount++
                    }
                }

                // Update the average rating display
                if (reviewCount > 0) {
                    val averageRating = totalRating / reviewCount
                    binding.averageRatingBar.rating = averageRating
                    binding.ratingCountText.text =
                        "${String.format("%.1f", averageRating)} ($reviewCount đánh giá)"
                } else {
                    binding.averageRatingBar.rating = 0f
                    binding.ratingCountText.text = "0.0 (0 đánh giá)"
                }

                // Update the adapter
                reviewAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@DetailsActivity,
                    "Lỗi khi tải đánh giá: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun submitReview() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show()
            return
        }

        // Make sure we have a key before submitting a review
        if (flowerKey.isNullOrEmpty() && !flowerName.isNullOrEmpty()) {
            findFlowerKeyAndThenSubmitReview(userId)
            return
        }

        if (flowerKey.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "Không thể tìm thấy mã sản phẩm để đánh giá",
                Toast.LENGTH_SHORT
            )
                .show()
            return
        }

        val rating = binding.userRatingBar.rating
        val comment = binding.reviewEditText.text.toString().trim()

        if (rating <= 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show()
            return
        }

        if (comment.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung đánh giá", Toast.LENGTH_SHORT).show()
            return
        }

        // Get user information
        val userRef = database.reference.child("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userName =
                    snapshot.child("name").getValue(String::class.java) ?: "Người dùng ẩn danh"
                val userImage =
                    snapshot.child("profileImage").getValue(String::class.java) ?: ""

                // Create review object
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                val review = ReviewModel(
                    userId = userId,
                    userName = userName,
                    userImage = userImage,
                    rating = rating,
                    comment = comment,
                    date = currentDate,
                    images = emptyList() // Add image support in the future if needed
                )

                // Save to Firebase
                saveReviewToFirebase(review)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@DetailsActivity,
                    "Lỗi khi tải thông tin người dùng: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // New helper method to find flower key and then submit review
    private fun findFlowerKeyAndThenSubmitReview(userId: String) {
        val flowersRef = database.reference.child("list")
        flowersRef.orderByChild("flowerName").equalTo(flowerName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (flowerSnapshot in snapshot.children) {
                            flowerKey = flowerSnapshot.key
                            // Now that we have the key, we can proceed with the review submission
                            submitReview()
                            break
                        }
                    } else {
                        Toast.makeText(
                            this@DetailsActivity,
                            "Không thể tìm thấy mã sản phẩm để đánh giá",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@DetailsActivity,
                        "Lỗi: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun saveReviewToFirebase(review: ReviewModel) {
        val reviewsRef = database.reference.child("reviews").child(flowerKey!!)
        val newReviewKey = reviewsRef.push().key

        if (newReviewKey != null) {
            reviewsRef.child(newReviewKey).setValue(review)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cảm ơn bạn đã gửi đánh giá!", Toast.LENGTH_SHORT)
                        .show()
                    // Clear input fields
                    binding.userRatingBar.rating = 0f
                    binding.reviewEditText.setText("")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Lỗi khi gửi đánh giá: ${e.message}",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
        } else {
            Toast.makeText(this, "Không thể tạo mã đánh giá mới", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addItemToCart() {
        val database = FirebaseDatabase.getInstance().reference
        val userId = auth.currentUser?.uid ?: ""

        // Đảm bảo flowerKey không null trước khi thêm vào giỏ hàng
        if (flowerKey.isNullOrEmpty() && !flowerName.isNullOrEmpty()) {
            findFlowerKeyByName {
                addItemToCartWithKey(userId, database)
            }
        } else {
            addItemToCartWithKey(userId, database)
        }
    }

    // Hàm mới để thêm vào giỏ hàng sau khi đã có key
    private fun addItemToCartWithKey(userId: String, database: DatabaseReference) {
        println("DEBUG: Adding to cart with flowerKey: $flowerKey")

        // Tạo đối tượng CartItem với flowerKey đúng
        val cartItem = CartItems(
            flowerName.toString(),
            flowerPrice.toString(),
            flowerDescriptions.toString(),
            flowerImage.toString(),
            1,
            flowerIngredients,
            quantity = 1,
            flowerKey = flowerKey,
        )
        // Lưu dữ liệu cartItem vào Firebase
        database.child("users").child(userId).child("CartItems").push().setValue(cartItem)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Thêm sản phẩm vào giỏ hàng thành công <3",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }.addOnFailureListener {
                Toast.makeText(
                    this,
                    "Thêm sản phẩm vào giỏ hàng thất bại -_-",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
    }

    private fun getAndDisplayPopularItems() {
        val flowerRef: DatabaseReference = database.reference.child("list")
        val menuItems = mutableListOf<MenuItem>()
        flowerRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                menuItems.clear()
                for (flowerSnapshot in snapshot.children) {
                    val menuItem = flowerSnapshot.getValue(MenuItem::class.java)
                    menuItem?.let {
                        it.key = flowerSnapshot.key ?: "" // Gán key Firebase cho MenuItem
                        menuItems.add(it)
                    }
                }
                randomPopularItems(menuItems)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@DetailsActivity,
                    "Không thể tải dữ liệu sản phẩm: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun randomPopularItems(menuItems: MutableList<MenuItem>) {
        val index = menuItems.indices.toList().shuffled()
        val numItemToShow = minOf(6, menuItems.size)
        val subsetMenuItems = index.take(numItemToShow).map {
            menuItems[it]
        }
        setPopularItemAdapter(subsetMenuItems)
    }

//    private fun setPopularItemAdapter(subsetMenuItems: List<MenuItem>) {
//        val adapter = MenuAdapter(subsetMenuItems, this@DetailsActivity)
//        binding.suggestedProductsRecyclerView.layoutManager =
//            GridLayoutManager(
//                this@DetailsActivity,
//                2
//            )  // Changed to GridLayoutManager with 2 columns
//        binding.suggestedProductsRecyclerView.adapter = adapter
//    }

    private fun setPopularItemAdapter(subsetMenuItems: List<MenuItem>) {
        // Chuyển đổi List thành ArrayList vì MenuAdapter yêu cầu ArrayList
        val adapter = MenuAdapter(
            ArrayList(subsetMenuItems),
            this@DetailsActivity,
            onAddToCart = { menuItem -> addItemToCart(menuItem) },
            listener = null // Không sử dụng interface listener
        )

        // Đảm bảo hiển thị phù hợp với layout item
        val layoutManager = GridLayoutManager(this, 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1 // Hoặc điều chỉnh dựa trên loại item
            }
        }

        binding.suggestedProductsRecyclerView.layoutManager = layoutManager
        binding.suggestedProductsRecyclerView.adapter = adapter
    }

    fun formatVND(amount: Number): String {
        val localeVN = Locale("vi", "VN")
        val currencyVN = Currency.getInstance("VND")

        val vndFormat = NumberFormat.getCurrencyInstance(localeVN)
        vndFormat.currency = currencyVN
        return vndFormat.format(amount)
    }

    private fun addItemToCart(menuItem: MenuItem) {
        val userId = auth.currentUser?.uid ?: ""
        if (userId.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val cartItem = CartItems(
            flowerName = menuItem.flowerName ?: "",
            flowerPrice = menuItem.flowerPrice ?: "",
            flowerDescription = menuItem.flowerDescription ?: "",
            flowerImage = menuItem.flowerImage ?: "",
            quantity = 1,
            flowerIngredient = menuItem.flowerIngredient,
            flowerKey = menuItem.key
        )

        database.reference.child("users").child(userId).child("CartItems").push()
            .setValue(cartItem)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Thêm sản phẩm vào giỏ hàng thành công!",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Thêm giỏ hàng thất bại: ${it.message}",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
    }

    private var flowerKeysFromHistory: MutableList<String> = mutableListOf()

    private fun checkPurchaseHistoryForCurrentProduct() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            // User not logged in, hide review components
            updateReviewUIVisibility(false)
            println("DEBUG: User not logged in, hiding review components")
            return
        }

        if (flowerKey.isNullOrEmpty()) {
            println("DEBUG: Can't check purchase history without flower key")
            return
        }

        val buyHistoryRef = database.reference
            .child("users")
            .child(userId)
            .child("BuyHistory")

        println("DEBUG: Checking purchase history for product with key: $flowerKey")

        buyHistoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var hasPurchased = false

                if (snapshot.exists()) {
                    // Check all orders in the user's buy history
                    for (orderSnapshot in snapshot.children) {
                        // Try different paths to find flower keys
                        // Option 1: Direct flowerKey field
                        val directFlowerKey =
                            orderSnapshot.child("flowerKey").getValue(String::class.java)

                        // Option 2: In flowerImages node
                        val flowerImagesSnapshot = orderSnapshot.child("flowerImages")
                        val flowerKeyInImages =
                            flowerImagesSnapshot.child("flowerKey").getValue(String::class.java)

                        // Option 3: Check in items node if it exists
                        val itemsSnapshot = orderSnapshot.child("items")
                        val itemKeys = mutableListOf<String>()
                        for (itemSnapshot in itemsSnapshot.children) {
                            val itemFlowerKey =
                                itemSnapshot.child("flowerKey").getValue(String::class.java)
                            if (!itemFlowerKey.isNullOrEmpty()) {
                                itemKeys.add(itemFlowerKey)
                            }
                        }

                        // Process all potential key strings we found
                        val keyStringsToCheck =
                            listOfNotNull(directFlowerKey, flowerKeyInImages) + itemKeys

                        for (keyString in keyStringsToCheck) {
                            // Handle both possible formats: comma-separated list or single key
                            if (keyString.contains(",")) {
                                // It's a comma-separated list
                                val productKeys = keyString.split(",")
                                for (keyEntry in productKeys) {
                                    // Extract just the key part (before the "=" if it exists)
                                    val productId = if (keyEntry.contains("=")) {
                                        keyEntry.split("=")[0].trim('{', '}', ' ')
                                    } else {
                                        keyEntry.trim('{', '}', ' ')
                                    }

                                    println("DEBUG: Comparing product ID: $productId with flowerKey: $flowerKey")

                                    // Check if this order contains our current product
                                    if (productId == flowerKey) {
                                        hasPurchased = true
                                        println("DEBUG: Match found! User has purchased product with key: $flowerKey")
                                        break
                                    }
                                }
                            } else {
                                // It's a single key or key-value pair
                                // Need to handle formats like "{-OQCi2ci2J5kNp412n9n=1}"
                                val cleanedKeyString = keyString.trim('{', '}', ' ')

                                // Extract the key part (before the "=" if it exists)
                                val productId = if (cleanedKeyString.contains("=")) {
                                    cleanedKeyString.split("=")[0].trim()
                                } else {
                                    cleanedKeyString
                                }

                                println("DEBUG: Comparing single product ID: original='$keyString', extracted='$productId' with flowerKey: $flowerKey")

                                if (productId == flowerKey) {
                                    hasPurchased = true
                                    println("DEBUG: Match found! User has purchased product with key: $flowerKey")
                                    break
                                }
                            }

                            if (hasPurchased) break
                        }

                        if (hasPurchased) break // Exit loop early if we found a match
                    }
                } else {
                    println("DEBUG: User has no purchase history")
                }

                // Update UI based on purchase history
                updateReviewUIVisibility(hasPurchased)
                println("DEBUG: Review UI visibility updated - can review: $hasPurchased")
            }

            override fun onCancelled(error: DatabaseError) {
                println("DEBUG: Error checking purchase history: ${error.message}")
                // Default to hiding review UI on error
                updateReviewUIVisibility(false)
            }
        })
    }

    // Helper method to update UI visibility based on purchase status
    private fun updateReviewUIVisibility(hasPurchased: Boolean) {
        if (hasPurchased) {
            // User has purchased this product, show review components
            binding.addReviewTitle.visibility = View.VISIBLE
            binding.userRatingBar.visibility = View.VISIBLE
            binding.reviewEditText.visibility = View.VISIBLE
            binding.submitReviewButton.visibility = View.VISIBLE
            binding.ycMH.visibility = View.GONE
        } else {
            // User has not purchased this product, hide review components
            binding.addReviewTitle.visibility = View.GONE
            binding.userRatingBar.visibility = View.GONE
            binding.reviewEditText.visibility = View.GONE
            binding.submitReviewButton.visibility = View.GONE
            binding.ycMH.visibility = View.VISIBLE
        }
    }

    fun getOriginalPriceFromDiscounted(adjustedPrice: Double, tag: String?): Double {
        val discountPercent = when (tag) {
            "Sale" -> 0.25
            "Nổi Bật" -> 0.20
            "Mới" -> 0.15
            else -> 0.0
        }
        return if (discountPercent != 0.0) adjustedPrice / (1 - discountPercent) else adjustedPrice
    }

}