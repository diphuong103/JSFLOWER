package com.example.jsflower.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jsflower.R
import com.example.jsflower.adaptar.BuyAgainAdapter
import com.example.jsflower.databinding.FragmentHistoryBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class HistoryFragment : Fragment() {
    private lateinit var binding : FragmentHistoryBinding
    private lateinit var buyAgainAdapter: BuyAgainAdapter

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHistoryBinding.inflate(layoutInflater, container, false)
        // Inflate the layout for this fragment

        setupRecyclerView()
        return binding.root
    }

    private fun setupRecyclerView()
    {
        val buyAgainFlowerName = arrayListOf("Hoa 1", "Hoa 2", "Hoa 3")
        val buyAgainFlowerPrice = arrayListOf("128000VND", "999999VND", "876544VND")
        val buyAgainFlowerImage = arrayListOf(R.drawable.hoaly_, R.drawable.hoababy_, R.drawable.hoalandiep_)

        buyAgainAdapter = BuyAgainAdapter(buyAgainFlowerName, buyAgainFlowerPrice, buyAgainFlowerImage)
        binding.buyAgainRecyclerView.adapter = buyAgainAdapter
        binding.buyAgainRecyclerView.layoutManager = LinearLayoutManager(requireContext())

    }

    companion object{

    }
}