package com.example.hearandthere_test.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hearandthere_test.MyApplication.Companion.context
import com.example.hearandthere_test.R
import com.example.hearandthere_test.BR
import com.example.hearandthere_test.databinding.ItemMapsAudioContentBinding
import com.example.hearandthere_test.model.response.ResAudioTrackInfoItemDto
import com.example.hearandthere_test.model.response.ResTrackPointDto
import com.example.hearandthere_test.ui.map.MapsFragment

class MapsViewPagerAdapter (private val activity: MapsFragment, private val data : List<ResTrackPointDto>, private val item : List<ResAudioTrackInfoItemDto>)
    : RecyclerView.Adapter<MapsViewPagerAdapter.PagerViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        val binding : ItemMapsAudioContentBinding
                = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.item_maps_audio_content, parent, false)
        return PagerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MapsViewPagerAdapter.PagerViewHolder, position: Int) {
        holder.bind(item, position)
        holder.binding(item[position],position, data[position].trackLatitude, data[position].trackLongitude)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class PagerViewHolder(private val binding : ItemMapsAudioContentBinding) : RecyclerView.ViewHolder(binding.root){
        private val imgs : ImageView = itemView.findViewById(R.id.iv_maps_audioInfo)

        fun binding(data : ResAudioTrackInfoItemDto, pos : Int, lati : Double, longi: Double){
            binding.item = data
            binding.cvPlayButton.setOnClickListener {
                activity.clickListener(pos, lati, longi)
            }
        }

        fun bind(data : List<ResAudioTrackInfoItemDto>, position: Int){
            Glide.with(activity)
                .load(data[position].images[0])
                .into(imgs)
        }
    }
}