package com.example.jsflower.Model

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class OrderDetails() : Parcelable {
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
    var flowerKey: String? = null
    val status: String = "pending" // Made status non-nullable and initialized

    // Secondary constructor
    constructor(
        userUid: String,
        userName: String,
        flowerNames: ArrayList<String>,
        flowerPrices: ArrayList<String>,
        flowerImages: ArrayList<String>,
        flowerQuantities: ArrayList<Int>,
        address: String,
        totalPrice: Double,
        phoneNumber: String,
        currentTime: Long,
        itemPushKey: String?,
        orderAccepted: Boolean,
        paymentReceived: Boolean,
        flowerKey: String?
    ) : this() { // Corrected constructor delegation
        this.userUid = userUid
        this.userName = userName
        this.flowerNames = flowerNames
        this.flowerPrices = flowerPrices
        this.flowerImages = flowerImages
        this.flowerQuantities = flowerQuantities
        this.address = address
        this.totalPrice = totalPrice
        this.phoneNumber = phoneNumber
        this.currentTime = currentTime
        this.itemPushKey = itemPushKey
        this.orderAccepted = orderAccepted
        this.paymentReceived = paymentReceived
        this.flowerKey = flowerKey
    }

    // Parcelable implementation
    constructor(parcel: Parcel) : this() { // Changed this() to this()
        userUid = parcel.readString()
        userName = parcel.readString()
        flowerNames = parcel.createStringArrayList()
        flowerPrices = parcel.createStringArrayList()
        flowerImages = parcel.createStringArrayList()
        flowerQuantities = parcel.readArrayList(Int::class.java.classLoader) as? ArrayList<Int>
        address = parcel.readString()
        totalPrice = parcel.readDouble()
        phoneNumber = parcel.readString()
        orderAccepted = parcel.readByte() != 0.toByte()
        paymentReceived = parcel.readByte() != 0.toByte()
        itemPushKey = parcel.readString()
        currentTime = parcel.readLong()
        flowerKey = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userUid)
        parcel.writeString(userName)
        parcel.writeStringList(flowerNames)
        parcel.writeStringList(flowerPrices)
        parcel.writeStringList(flowerImages)
        parcel.writeList(flowerQuantities)
        parcel.writeString(address)
        parcel.writeDouble(totalPrice ?: 0.0) // Handle null totalPrice
        parcel.writeString(phoneNumber)
        parcel.writeByte(if (orderAccepted) 1 else 0)
        parcel.writeByte(if (paymentReceived) 1 else 0)
        parcel.writeString(itemPushKey)
        parcel.writeLong(currentTime)
        parcel.writeString(flowerKey)
    }

    override fun describeContents(): Int {
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
