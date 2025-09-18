package com.lonyitrade.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lonyitrade.app.adapters.AdAdapter
import com.lonyitrade.app.viewmodels.SharedViewModel

class HomeFragment : Fragment() {

    private lateinit var adsRecyclerView: RecyclerView
    private lateinit var adAdapter: AdAdapter
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adsRecyclerView = view.findViewById(R.id.adsRecyclerView)
        adsRecyclerView.layoutManager = LinearLayoutManager(context)

        adAdapter = AdAdapter(sharedViewModel.adList.value ?: mutableListOf())
        adsRecyclerView.adapter = adAdapter

        sharedViewModel.adList.observe(viewLifecycleOwner) { updatedList ->
            adAdapter = AdAdapter(updatedList)
            adsRecyclerView.adapter = adAdapter
        }
    }
}