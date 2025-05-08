package com.example.jsflower.Model

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable
import java.util.ArrayList

class OrderDetails() : Serializable {
    var userUid: String? = null
    var userName: String? = null
    var flowerNames: ArrayList<String>? = null
    var flowerPrices: ArrayList<String>? = null
    var flowerImages: ArrayList<String>? = null
    var flowerQuantities: ArrayList<Int>? = null
    var address: String? = null
    var totalPrice: Double? = null
    var phoneNumber: String? = null
    var orderAccepted: Boolean = false
    var paymentReceived: Boolean = false
    var itemPushKey: String? = null
    var currentTime: Long = 0

    constructor(
        userId: String,
        name: String,
        flowerItemName: ArrayList<String>,
        flowerItemPrice: ArrayList<String>,
        flowerItemImage: ArrayList<String>,
        flowerItemQuantities: ArrayList<Int>,
        address: String,
        total: Double,
        phone: String,
        time: Long,
        itemPushKey: String?,
        orderAccepted: Boolean,
        paymentReceived: Boolean
    ) : this() {
        this.userUid = userId
        this.userName = name
        this.flowerNames = flowerItemName
        this.flowerPrices = flowerItemPrice
        this.flowerImages = flowerItemImage
        this.flowerQuantities = flowerItemQuantities
        this.address = address
        this.totalPrice = total
        this.phoneNumber = phone
        this.currentTime = time  // Fixed to use the parameter
        this.itemPushKey = itemPushKey
        this.orderAccepted = orderAccepted
        this.paymentReceived = paymentReceived  // Fixed variable name
    }

    // Constructor for Parcelable
    private constructor(parcel: Parcel) : this() {
        userUid = parcel.readString()
        userName = parcel.readString()
        flowerNames = ArrayList<String>().apply {
            parcel.readStringList(this)
        }
        flowerPrices = ArrayList<String>().apply {
            parcel.readStringList(this)
        }
        flowerImages = ArrayList<String>().apply {
            parcel.readStringList(this)
        }
        flowerQuantities = ArrayList<Int>().apply {
            parcel.readList(this as MutableList<Any>, Int::class.java.classLoader)
        }
        address = parcel.readString()
        totalPrice = parcel.readDouble()
        phoneNumber = parcel.readString()
        orderAccepted = parcel.readByte() != 0.toByte()
        paymentReceived = parcel.readByte() != 0.toByte()
        itemPushKey = parcel.readString()
        currentTime = parcel.readLong()
    }

    fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userUid)
        parcel.writeString(userName)
        flowerNames?.let { parcel.writeStringList(it) }
        flowerPrices?.let { parcel.writeStringList(it) }
        flowerImages?.let { parcel.writeStringList(it) }
        flowerQuantities?.let { parcel.writeList(it as List<*>) }
        parcel.writeString(address)
        parcel.writeDouble(totalPrice ?: 0.0)
        parcel.writeString(phoneNumber)
        parcel.writeByte(if (orderAccepted) 1 else 0)
        parcel.writeByte(if (paymentReceived) 1 else 0)
        parcel.writeString(itemPushKey)
        parcel.writeLong(currentTime)
    }

    fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderDetails> {
        override fun createFromParcel(parcel: Parcel): OrderDetails {
            return OrderDetails(parcel)
        }

        override fun newArray(size: Int): Array<OrderDetails?> {
            return arrayOfNulls(size)
        }
    }
}