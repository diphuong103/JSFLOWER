package com.example.jsflower

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jsflower.adaptar.NotificationAdapter
import com.example.jsflower.databinding.FragmentMenuBottomSheetBinding
import com.example.jsflower.databinding.FragmentNotifactionBottomBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class Notifaction_Bottom_Fragment :  BottomSheetDialogFragment() {

    private lateinit var binding: FragmentNotifactionBottomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotifactionBottomBinding.inflate(layoutInflater, container, false)

        val notifications = listOf("Don hang cua ban da duoc huy thanh cong", "Don hang cua ban dang duoc van chuyen", "Don hang cua ban da duoc dat")
        val notificationImages = listOf(R.drawable.sademoji, R.drawable.truck, R.drawable.congratulation)

        val adapter = NotificationAdapter(
            ArrayList(notifications),
            ArrayList(notificationImages)
        )


binding.notificationRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationRecyclerView.adapter = adapter
        // Inflate the layout for this fragment
        return binding.root
    }

    companion object {

    }
}