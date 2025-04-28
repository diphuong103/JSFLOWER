package com.example.jsflower.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jsflower.R
import com.example.jsflower.adaptar.CartAdapter
import com.example.jsflower.databinding.FragmentCartBinding



class CartFragment : Fragment() {
    private lateinit var binding: FragmentCartBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCartBinding.inflate(inflater, container, false)

        val cartFlowerNames = listOf("Hoa hồng", "Hoa hướng dương", "Hoa xuyến chi")
        val cartItemPrices = listOf("19.999 VND", "299.999 VND", "999.999 VND")
        val cartImages = listOf(
            R.drawable.hoaly_,
            R.drawable.hoababy_,
            R.drawable.hoalandiep_
        )

        val adapter = CartAdapter(
            ArrayList(cartFlowerNames),
            ArrayList(cartItemPrices),
            ArrayList(cartImages)
        )

        binding.CartRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.CartRecyclerView.adapter = adapter

        return binding.root
    }


    companion object {
    }

}